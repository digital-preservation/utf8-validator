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

/**
 * UTF8 Validator Command Line
 *
 * @author Adam Retter <adam.retter@googlemail.com>
 * @version 1.2
 */
public class Utf8ValidateCmd {
    
    final static String VERSION = "1.2";
        
    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        
        //check useage
        if(args.length < 1) {
            System.out.println("UTF8 Validator version: " + VERSION);
            System.out.println("Useage: utf8validate [options] <file>");
            System.out.println("");
            System.out.println("\t-f | --fail-fast");
            System.out.println("\t\tStops on the first validation error rather than reporting all errors");
            System.out.println("");
            System.exit(ExitCode.INVALID_ARGS.getCode());
        }
        
        //parse args
        final boolean failFast;
        final File f;
        if(args[0].equals("-f") || args[0].equals("--fail-fast")) {
            failFast = true;
            f = new File(args[1]);
        } else {
            failFast = false;
            f = new File(args[0]);
        }
        if(!f.exists()) {
            System.out.println("File: " + f.getPath() + " does not exist!");
            System.exit(ExitCode.INVALID_ARGS.getCode());
        }
        
        final PrintingValidationHandler handler = new PrintingValidationHandler(failFast, System.out);
        
        ExitCode result = ExitCode.OK;
        final long start = System.currentTimeMillis();
        
        System.out.println("Validating: " + f.getPath());
        
        try {
            new Utf8Validator(handler).validate(f);
            
            if(!failFast && handler.isErrored()) {
                result = ExitCode.VALIDATION_ERROR;
            } else {
                System.out.println("Valid OK (took " + (System.currentTimeMillis() - start) + "ms)");
                result = ExitCode.OK;
            }
        } catch(final ValidationException ve) {
            System.out.println(ve.getMessage());
            result = ExitCode.VALIDATION_ERROR;
        } catch(final IOException ioe) {
            System.err.println("[ERROR]" + ioe.getMessage());
            result = ExitCode.IO_ERROR;
        }
        
        System.exit(result.getCode());
    }
}
