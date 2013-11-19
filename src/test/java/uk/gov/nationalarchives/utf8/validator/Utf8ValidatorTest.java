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

import uk.gov.nationalarchives.utf8.validator.PrintingValidationHandler;
import uk.gov.nationalarchives.utf8.validator.Utf8Validator;
import uk.gov.nationalarchives.utf8.validator.ValidationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import org.junit.Test;

/**
 * @author Adam Retter <adam.retter@landmarkinfo.co.uk>
 */
public class Utf8ValidatorTest
{   
    @Test
    public void validOneByteChar() throws IOException, ValidationException {
        byte data[] = new byte[] { 0x78 };  //character 'x'
        new Utf8Validator(new PrintingValidationHandler(true, System.out)).validate(new ByteArrayInputStream(data));
    }
    
    @Test(expected=ValidationException.class) 
    public void invalidOneByteChar() throws IOException, ValidationException {
        byte data[] = new byte[] { (byte)0xC3 };  //first byte from 'e accute' two byte character
        new Utf8Validator(new PrintingValidationHandler(true, System.out)).validate(new ByteArrayInputStream(data));
    }
    
    @Test
    public void validTwoByteChar() throws IOException, ValidationException {
        byte data[] = new byte[] { (byte)0xC2, (byte)0xA9 };  //character 'copyright symbol'
        new Utf8Validator(new PrintingValidationHandler(true, System.out)).validate(new ByteArrayInputStream(data));
    }
    
    @Test(expected=ValidationException.class) 
    public void invalidTwoByteChar() throws IOException, ValidationException {
        byte data[] = new byte[] { (byte)0xC2, 0x78 };  //first byte from 'copyright symbol' and then byte from 'x' character
        new Utf8Validator(new PrintingValidationHandler(true, System.out)).validate(new ByteArrayInputStream(data));
    }
    
    @Test(expected=ValidationException.class) 
    public void invalidTwoByteChar2() throws IOException, ValidationException {
        byte data[] = new byte[] { 0x78, (byte)0xC2 };  //first byte from 'x' character and then first byte from  'copyright symbol'
        new Utf8Validator(new PrintingValidationHandler(true, System.out)).validate(new ByteArrayInputStream(data));
    }
    
    @Test
    public void validThreeByteChar() throws IOException, ValidationException {
        byte data[] = new byte[] { (byte)0xE2, (byte)0x82, (byte)0xAC };  //character 'euro symbol'
        new Utf8Validator(new PrintingValidationHandler(true, System.out)).validate(new ByteArrayInputStream(data));
    }
    
    @Test(expected=ValidationException.class) 
    public void invalidThreeByteChar() throws IOException, ValidationException {
        byte data[] = new byte[] { (byte)0xE2, (byte)0x82, 0x78 };  //first two bytes from 'euro symbol' and then byte from 'x' character
        new Utf8Validator(new PrintingValidationHandler(true, System.out)).validate(new ByteArrayInputStream(data));
    }
    
    @Test(expected=ValidationException.class) 
    public void invalidThreeByteChar2() throws IOException, ValidationException {
        byte data[] = new byte[] { (byte)0xE2, 0x78, (byte)0x82 };  //first byte from 'euro symbol', then byte from 'x' character, then second byte from 'euro symbol'
        new Utf8Validator(new PrintingValidationHandler(true, System.out)).validate(new ByteArrayInputStream(data));
    }
    
    @Test(expected=ValidationException.class) 
    public void invalidThreeByteChar3() throws IOException, ValidationException {
        byte data[] = new byte[] { 0x78, (byte)0xE2, (byte)0x82 };  //byte from character 'x' and the first two bytes from 'euro symbol'
        new Utf8Validator(new PrintingValidationHandler(true, System.out)).validate(new ByteArrayInputStream(data));
    }
    
    @Test
    public void validFourByteChar() throws IOException, ValidationException {
        byte data[] = new byte[] { (byte)0xF0, (byte)0x9F, (byte)0x80, (byte)0xB0 };  //character 'domino tile horizontal black'
        new Utf8Validator(new PrintingValidationHandler(true, System.out)).validate(new ByteArrayInputStream(data));
    }
    
    @Test(expected=ValidationException.class)
    public void invalidFourByteChar() throws IOException, ValidationException {
        byte data[] = new byte[] { (byte)0xF0, (byte)0x9F, (byte)0x80, (byte)0x78 };  //first three bytes from character 'domino tile horizontal black', then the byte from character(x)
        new Utf8Validator(new PrintingValidationHandler(true, System.out)).validate(new ByteArrayInputStream(data));
    }
    
    @Test(expected=ValidationException.class)
    public void oneInvalidOneByteChar_followedByTwoValidOneByteChars() throws IOException, ValidationException {
        final byte data[] = new byte[] {(byte)0x92, (byte)0x2C, (byte)0x63}; //characters: invalid char, 'comma', 'c'
        new Utf8Validator(new PrintingValidationHandler(true, System.out)).validate(new ByteArrayInputStream(data));
    }
    
    @Test(expected=ValidationException.class)
    public void oneValidOneByteChar_oneInvalidOneByteChar_followedByOneValidOneByteChar() throws IOException, ValidationException {
        final byte data[] = new byte[] {(byte)0x2C, (byte)0x92, (byte)0x63}; //characters: 'comma', invalid char, 'c'
        new Utf8Validator(new PrintingValidationHandler(true, System.out)).validate(new ByteArrayInputStream(data));
    }
}
