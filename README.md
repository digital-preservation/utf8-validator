UTF8 Validator
==============

A UTF-8 Validation Tool which may be used as either a command line tool or as a library embedded in your own program.

Released under the [BSD 3-Clause Licence](http://opensource.org/licenses/BSD-3-Clause).

[![Build Status](https://travis-ci.org/digital-preservation/utf8-validator.png?branch=master)](https://travis-ci.org/digital-preservation/utf8-validator)


Use from the Command Line
-------------------------
You can either download the application from [here](http://TODO) or [build from the source code](#building-from-source-code). You should extract this ZIP file to the place on your computer where you keep your applications. You can then run either `bin/validate` (Linux/Mac/Unix) or `bin\validate.bat` (Windows).

For example, to report all validation errors:

```bash
$ cd /opt/utf8-validator-1.2
$ bin/validate /tmp/my-file.txt
```

For example to report the first validation error and exit:

```bash
$ cd /opt/utf8-validator-1.2
$ bin/validate --fail-fast /tmp/my-file.txt
```

Command Line Exit Codes
-----------------------
* **0** Success
* **1** Inavlid Arguments provided to the application
* **2** File was not UTF-8 Valid
* **4** IO Error, e.g. could not read file


Use as a Library
----------------
The UTF-8 Validator is written in Java and may be easily used from any Java (Scala, Clojure, JVM Language etc) application. We are using the Maven build system, and our artifacts have been published to [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22uk.gov.nationalarchives%22).

If you are using Maven, you can simply add this to the dependencies section of your pom.xml:

```xml
<dependency>
    <groupId>uk.gov.nationalarchives</groupId>
    <artifactId>utf8-validator</artifactId>
    <version>1.2</version>
</dependency>
```

Alternatively if you are using Sbt, you can add this to your library dependencies:

```scala
"uk.gov.nationalarchives" % "utf8-validator" % "1.2"
```

To use the Library you need to implement the very simple interface `uk.gov.nationalarchives.utf8.validator.ValidationHandler` (or you could use `uk.gov.nationalarchives.utf8.validator.PrintingValidationHandler` if it suits you). The interface has a single method which is called whenever a validator finds a validation error. You can then instantiate `Utf8Validator` and validate from either a `java.io.File` or `java.io.InputStream`. For example:

```java
ValidationHandler handler = new ValidationHandler() {
	@Override
	public void error(final String message, final long byteOffset) throws ValidationException {
		System.err.println("[Error][@" + byteOffset + "] " + message);
	};
};

File f = ... //your file here

new Utf8Validator(handler).validate(f);
```

New Java 8 features allow you to write a `ValidationHandlerFunction` or `ValidationHandler` as a lambda expression.  The `ValidationHandlerFunction` provides additional information about the validation error, including the `File` the error occurred in.  This information is encapsulated as an `Optional` because validation may be occurring over an `InputStream`.  The Java 8 Streams API allows you to parallelize validation over a large set of files.  For example:

```java
List<File> filesToValidate = ...

// Stores error messages
ConcurrentHashMap<File, Map<Long, String>> fileErrors = new ConcurrentHashMap<>();

Utf8Validator validator = new Utf8Validator((file, message, off) -> {
    if (fileErrors.containsKey(file.get())) {
        return; // only log the first error
    }
   fileErrors.putIfAbsent(file.get(), new TreeMap<>());
   fileErrors.get(file.get()).put(off, message);
});

filesToValidate.parallelStream().forEach((f) -> {
    try {
        validator.validate(f);
    } catch (IOException | ValidationException e) {
        fail("Unexpected validation exception validating " + f + ": " + e.getMessage());
    }
});
```

Building from Source Code
--------------------------
* Git clone the repository from https://github.com/digital-preservation/utf8-validator.git
* Build using [Maven](http://maven.apache.org), by running `mvn package` you will then find a ZIP of the compiled application in `target/utf8-validator-1.2-application.zip`.