/*
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

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Validates a File or InputStream byte by byte
 * to ensure it is UTF-8 Valid.
 * 
 * @author Adam Retter <adam.retter@googlemail.com>
 * @version 1.2
 */
public class Utf8Validator {

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private static final int FOUR_BYTE_CHAR =       0xF0;   // 11110xxx
    private static final int THREE_BYTE_CHAR =      0xE0;   // 1110xxxx
    private static final int TWO_BYTE_CHAR =        0xC0;   // 110xxxxx

    private int bufferSize;
    private boolean memMapped;
    private ValidationHandler handler;

    /**
     * @param handler A ValidationHandler that receives errors
     */
    public Utf8Validator(final ValidationHandler handler) {
        this(DEFAULT_BUFFER_SIZE, handler);
    }

    /**
     * @param bufferSize the amount of data from the file (in bytes) to buffer in RAM
     * @param handler A ValidationHandler that receives errors
     */
    public Utf8Validator(final int bufferSize, final ValidationHandler handler) {
        this(false, DEFAULT_BUFFER_SIZE, handler);
    }


    /**
     * @memMapped true if memory mapped I/O should be used
     * @param handler A ValidationHandler that receives errors
     */
    public Utf8Validator(final boolean memMapped, final ValidationHandler handler) {
        this(memMapped, DEFAULT_BUFFER_SIZE, handler);
    }

    /**
     * @memMapped true if memory mapped I/O should be used
     * @param bufferSize the amount of data from the file (in bytes) to buffer in RAM
     * @param handler A ValidationHandler that receives errors
     */
    public Utf8Validator(final boolean memMapped, final int bufferSize, final ValidationHandler handler) {
        this.memMapped = memMapped;
        this.bufferSize = bufferSize <= 0 ? DEFAULT_BUFFER_SIZE : bufferSize;
        this.handler = handler;
    }
    
    /**
     * Validates the File as UTF-8.
     * 
     * @param f The file to UTF-8 validate
     * 
     * @throws IOException Exception is thrown if the file cannot be read
     * @throws ValidationException thrown if the ValidationHandler determines
     * that an error causes an exception
     */
    public void validate(final File f) throws IOException, ValidationException {
        if (memMapped) {
            RandomAccessFile raf = null;
            FileChannel fc = null;
            try {
                raf = new RandomAccessFile(f, "r");
                fc = raf.getChannel();
                final MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                validate(buffer);
                buffer.clear();
            } finally {
                if(fc != null) {
                    fc.close();
                }
                if(raf != null) {
                    raf.close();
                }
            }
        } else {
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(f), bufferSize);
                validate(is);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }
    }
    
    /**
     * Validates Input Stream as UTF-8.
     * 
     * @param is Input Stream for UTF-8 validation
     * 
     * @throws IOException Exception is thrown if the stream cannot be read
     * @throws ValidationException thrown if the ValidationHandler determines
     * that an error causes an exception
     */
    public void validate(final InputStream is) throws IOException, ValidationException {
        int read = 0;                       // total bytes read
        byte multiByteLen = 0;              // length of multi-byte character sequence (or zero if a single byte character)
        byte multiBytesRemain = 0;          // bytes remaining to read of multi-byte character sequence (or zero if a single byte character)
        int b = -1;                         // current byte

        while ((b = is.read()) > -1) {

            read++;

            if (multiBytesRemain > 0) {
                multiBytesRemain--;
                if ((b >>> 6) != 2) {
                    handler.error("Invalid UTF-8 sequence, byte " + (multiByteLen - multiBytesRemain) + " of " + multiByteLen + " byte sequence.", read);
                }

            } else if ((b & 0x80) == 0) {
                // One byte Sequence (MSB of a single byte character must be 0)
                continue;

            } else if ((b & FOUR_BYTE_CHAR) == FOUR_BYTE_CHAR) {
                //Four byte Sequence
                multiByteLen = 4;
                multiBytesRemain = 3;

            } else if((b & THREE_BYTE_CHAR) == THREE_BYTE_CHAR) {
                //Three byte Sequence
                multiByteLen = 3;
                multiBytesRemain = 2;

            } else if((b & TWO_BYTE_CHAR) == TWO_BYTE_CHAR) {
                //Two byte Sequence
                multiByteLen = 2;
                multiBytesRemain = 1;

            } else {
                handler.error("Invalid single byte UTF-8 character ", read);
            }
        }

        if (multiBytesRemain > 0) {
            handler.error("Invalid UTF-8 Sequence, expecting: " + multiBytesRemain + " more bytes in " + multiByteLen + " byte sequence. End of File!", read);
        }
    }

    /**
     * Validates Mapped Byte Buffer as UTF-8.
     *
     * @param buf Mapped Byte Buffer for UTF-8 validation
     *
     * @throws IOException Exception is thrown if the buf cannot be read
     * @throws ValidationException thrown if the ValidationHandler determines
     * that an error causes an exception
     */
    public void validate(final MappedByteBuffer buf) throws IOException, ValidationException {
        int read = 0;                       // total bytes read
        byte multiByteLen = 0;              // length of multi-byte character sequence (or zero if a single byte character)
        byte multiBytesRemain = 0;          // bytes remaining to read of multi-byte character sequence (or zero if a single byte character)
        int b = -1;                         // current byte

        while (buf.remaining() > 0) {

            b = buf.get() & 0xFF;

            read++;

            if (multiBytesRemain > 0) {
                multiBytesRemain--;
                if ((b >>> 6) != 2) {
                    handler.error("Invalid UTF-8 sequence, byte " + (multiByteLen - multiBytesRemain) + " of " + multiByteLen + " byte sequence.", read);
                }

            } else if ((b & 0x80) == 0) {
                // One byte Sequence (MSB of a single byte character must be 0)
                continue;

            } else if ((b & FOUR_BYTE_CHAR) == FOUR_BYTE_CHAR) {
                //Four byte Sequence
                multiByteLen = 4;
                multiBytesRemain = 3;

            } else if((b & THREE_BYTE_CHAR) == THREE_BYTE_CHAR) {
                //Three byte Sequence
                multiByteLen = 3;
                multiBytesRemain = 2;

            } else if((b & TWO_BYTE_CHAR) == TWO_BYTE_CHAR) {
                //Two byte Sequence
                multiByteLen = 2;
                multiBytesRemain = 1;

            } else {
                handler.error("Invalid single byte UTF-8 character ", read);
            }
        }

        if (multiBytesRemain > 0) {
            handler.error("Invalid UTF-8 Sequence, expecting: " + multiBytesRemain + " more bytes in " + multiByteLen + " byte sequence. End of File!", read);
        }
    }
}
