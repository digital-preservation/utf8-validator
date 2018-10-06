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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Adam Retter <adam.retter@googlemail.com>
 */
@RunWith(Parameterized.class)
public class Utf8ValidatorTest {
    @Parameterized.Parameters(name = "{0}")
    public static java.util.Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"classic", false},
                {"memory-mapped", true}
        });
    }

    @Parameterized.Parameter(value = 0)
    public String name;

    @Parameterized.Parameter(value = 1)
    public boolean memoryMappedIo;

    @Test
    public void validOneByteChar() throws IOException, ValidationException, URISyntaxException {
        //character 'x'
        new Utf8Validator(memoryMappedIo, new PrintingValidationHandler(true, System.out))
                .validate(testResource("valid-one-byte-char.bin"));
    }

    @Test(expected = ValidationException.class)
    public void invalidOneByteChar() throws IOException, ValidationException, URISyntaxException {
        //first byte from 'e accute' two byte character
        new Utf8Validator(memoryMappedIo, new PrintingValidationHandler(true, System.out))
                .validate(testResource("invalid-one-byte-char.bin"));
    }

    @Test
    public void validTwoByteChar() throws IOException, ValidationException, URISyntaxException {
        //character 'copyright symbol'
        new Utf8Validator(memoryMappedIo, new PrintingValidationHandler(true, System.out))
                .validate(testResource("valid-two-byte-char.bin"));
    }

    @Test(expected = ValidationException.class)
    public void invalidTwoByteChar() throws IOException, ValidationException, URISyntaxException {
        //first byte from 'copyright symbol' and then byte from 'x' character
        new Utf8Validator(memoryMappedIo, new PrintingValidationHandler(true, System.out))
                .validate(testResource("invalid-two-byte-char.bin"));
    }

    @Test(expected = ValidationException.class)
    public void invalidTwoByteChar2() throws IOException, ValidationException, URISyntaxException {
        //first byte from 'x' character and then first byte from  'copyright symbol'
        new Utf8Validator(memoryMappedIo, new PrintingValidationHandler(true, System.out))
                .validate(testResource("invalid-two-byte-char-2.bin"));
    }

    @Test
    public void validThreeByteChar() throws IOException, ValidationException, URISyntaxException {
        //character 'euro symbol'
        new Utf8Validator(memoryMappedIo, new PrintingValidationHandler(true, System.out))
                .validate(testResource("valid-three-byte-char.bin"));
    }

    @Test(expected = ValidationException.class)
    public void invalidThreeByteChar() throws IOException, ValidationException, URISyntaxException {
        //first two bytes from 'euro symbol' and then byte from 'x' character
        new Utf8Validator(memoryMappedIo, new PrintingValidationHandler(true, System.out))
                .validate(testResource("invalid-three-byte-char.bin"));
    }

    @Test(expected = ValidationException.class)
    public void invalidThreeByteChar2() throws IOException, ValidationException, URISyntaxException {
        //first byte from 'euro symbol', then byte from 'x' character, then second byte from 'euro symbol'
        new Utf8Validator(memoryMappedIo, new PrintingValidationHandler(true, System.out))
                .validate(testResource("invalid-three-byte-char-2.bin"));
    }

    @Test(expected = ValidationException.class)
    public void invalidThreeByteChar3() throws IOException, ValidationException, URISyntaxException {
        //byte from character 'x' and the first two bytes from 'euro symbol'
        new Utf8Validator(memoryMappedIo, new PrintingValidationHandler(true, System.out))
                .validate(testResource("invalid-three-byte-char-3.bin"));
    }

    @Test
    public void validFourByteChar() throws IOException, ValidationException, URISyntaxException {
        //character 'domino tile horizontal black'
        new Utf8Validator(memoryMappedIo, new PrintingValidationHandler(true, System.out))
                .validate(testResource("valid-four-byte-char.bin"));
    }

    @Test(expected = ValidationException.class)
    public void invalidFourByteChar() throws IOException, ValidationException, URISyntaxException {
        //first three bytes from character 'domino tile horizontal black', then the byte from character(x)
        new Utf8Validator(memoryMappedIo, new PrintingValidationHandler(true, System.out))
                .validate(testResource("invalid-four-byte-char.bin"));
    }

    @Test(expected = ValidationException.class)
    public void oneInvalidOneByteChar_followedByTwoValidOneByteChars() throws IOException, ValidationException, URISyntaxException {
        //characters: invalid char, 'comma', 'c'
        new Utf8Validator(memoryMappedIo, new PrintingValidationHandler(true, System.out))
                .validate(testResource("invalid-mixed-1.bin"));
    }

    @Test(expected = ValidationException.class)
    public void oneValidOneByteChar_oneInvalidOneByteChar_followedByOneValidOneByteChar() throws IOException, ValidationException, URISyntaxException {
        //characters: 'comma', invalid char, 'c'
        new Utf8Validator(memoryMappedIo, new PrintingValidationHandler(true, System.out))
                .validate(testResource("invalid-mixed-2.bin"));
    }

    private File testResource(final String filename) throws URISyntaxException {
        final URL resource = getClass().getResource(filename);
        return new File(resource.toURI());
    }
}
