/**
 * Copyright © 2011, The National Archives <digitalpreservation@nationalarchives.gov.uk>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package uk.gov.nationalarchives.utf8.validator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.Optional;

/**
 * Validates a File or InputStream byte by byte
 * to ensure it is UTF-8 Valid
 * 
 * @author Adam Retter <adam.retter@googlemail.com>
 * @version 1.2
 */
public class Utf8Validator {

    private static ThreadLocal<File> FILE_BEING_VALIDATED = new ThreadLocal<>();

    private ValidationHandlerFunction handler;

    /**
     * @param handler A ValidationHandler that receives errors
     */
    public Utf8Validator(final ValidationHandler handler) {
        this.handler = (file, message, byteOffset) -> handler.error(message, byteOffset);
    }

    /**
     * @param handler A ValidationHandlerFunction that receives errors
     */
    public Utf8Validator(ValidationHandlerFunction handler) {
        this.handler = handler;
    }

    /**
     * Validates the File as UTF-8
     * 
     * @param f The file to UTF-8 validate
     * 
     * @throws IOException Exception is thrown if the file cannot be read
     * @throws ValidationException thrown if the ValidationHandler determines
     * that an error causes an exception
     */
    public void validate(final File f) throws IOException, ValidationException {
        InputStream is = null;
        try {
            FILE_BEING_VALIDATED.set(f);
            is = new BufferedInputStream(new FileInputStream(f));
            validate(is);
        } finally {
            FILE_BEING_VALIDATED.remove();
            if(is != null) {
                is.close();
            }
        }
    }
    
    /**
     * Validates Input Stream as UTF-8
     * 
     * @param is Input Stream for UTF-8 validation
     * 
     * @throws IOException Exception is thrown if the stream cannot be read
     * @throws ValidationException thrown if the ValidationHandler determines
     * that an error causes an exception
     */
    public void validate(final InputStream is) throws IOException, ValidationException {
        
        final ByteCountingInputStream bis = new ByteCountingInputStream(is);
        
        final BitSet fourByteChar = new BitSet(Byte.SIZE);    
        fourByteChar.set(0, 4);                         //11110    
        final BitSet threeByteChar = new BitSet(Byte.SIZE);   
        threeByteChar.set(0, 3);                        //1110
        final BitSet twoByteChar = new BitSet(Byte.SIZE);     
        twoByteChar.set(0, 2);                          //110
        
        //main processing
        int b = -1;
        while((b = bis.read()) > -1) {
            final BitSet bs = toBitSet((byte)b);

            if(startsWith(bs, fourByteChar)) {
                //Four byte Sequence
                checkRemainingBytes(bis, 3);
            }
            else if(startsWith(bs, threeByteChar)) {
                //Three byte Sequence
                checkRemainingBytes(bis, 2);
            }
            else if(startsWith(bs, twoByteChar)) {
                //Two byte Sequence
                checkRemainingBytes(bis, 1);
            }
            else {
                //One byte Sequence
                checkSingleByteChar(bs, bis.getByteCount());
            }
        }
    }
    
    /**
     * Checks whether a single byte character is UTF-8 valid
     * 
     * @param bs Bitset of the single byte character to check
     * @param byteOffset The position of the byte in the stream
     * 
     * @throws ValidationException thrown if the ValidationHandler determines
     * that an error causes an exception
     */
    private void checkSingleByteChar(final BitSet bs, final long byteOffset) throws ValidationException {
        
        //msb of a single byte character must be 0
        if(bs.get(0) == true) {
            handler.error(Optional.ofNullable(FILE_BEING_VALIDATED.get()), "Invalid single byte UTF-8 character ", byteOffset);
        }
    }
    
    /**
     * Checks whether the remaining bytes in a multi-byte character are valid UTF-8
     * 
     * @param is The byte stream to read and check the bytes from
     * @param nRemainingBytes The number of bytes to read and check
     * 
     * @throws IOException if the stream cannot be read
     * @throws ValidationException thrown if the ValidationHandler determines
     * that an error causes an exception
     */
    private void checkRemainingBytes(final ByteCountingInputStream is, final int nRemainingBytes) throws IOException, ValidationException {
        final byte remain[] = new byte[nRemainingBytes];
        final int read = is.read(remain);
        if(read != nRemainingBytes) {
            handler.error(Optional.ofNullable(FILE_BEING_VALIDATED.get()), "Invalid UTF-8 Sequence, expecting: " + (nRemainingBytes + 1) + "bytes, but got: " + (read + 1) + "bytes - reached end of stream.", -1);
        }
        
        for(int i = 0; i < nRemainingBytes; i++) {
            //remaining bytes must start with bits 10
            final BitSet bs = toBitSet(remain[i]);
            if(!(bs.get(0) == true &&  bs.get(1) == false)) {
                handler.error(Optional.ofNullable(FILE_BEING_VALIDATED.get()), "Invalid UTF-8 sequence, byte " + (i+2) + " of " + (nRemainingBytes+1) + " byte multibyte sequence.", (is.getByteCount() - nRemainingBytes + i + 1));
            }
        }
    }
    
    /**
     * Determines whether a bitset bs starts with the bits from the bitset cmp
     * 
     * @param bs The bitset to check
     * @param cmp The comparison bitset
     * 
     * @return true if the Bitset bs starts with the bits from the bitset cmp, else false
     */
    private boolean startsWith(final BitSet bs, final BitSet cmp) {
        final BitSet nCmp = (BitSet)cmp.clone();
        nCmp.and(bs);
        return nCmp.equals(cmp);
    }    
    
    /**
     * Converts a byte to a BitSet MSB first
     * 
     * @param b The byte to convert to a BitSet
     * @return The BitSet representation of the byte
     */
    private BitSet toBitSet(final byte b) {
        final BitSet bs = new BitSet(Byte.SIZE);
        
        for(int i = 0; i < Byte.SIZE; i++) {
            if ((b & (1 <<(i % Byte.SIZE))) > 0) {
                bs.set(Byte.SIZE -i - 1);
            }
        }
        return bs;
    }
    
    /*
    private static void printBitSet(BitSet bs)
    {
        String bitString = new String();
        for(int i = 0; i < Byte.SIZE; i++)
        {
            if(bs.get(i))
                bitString += "1";
            else
                bitString += "0";
        }
        System.out.println(bitString);
    }
    */
}
