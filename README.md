# Notes2Google

## Description
Service to export contacts from Lotus Notes and import into Google contacts.

## Dependencies
https://github.com/klehmann/domino-jna

## Java version
We have to use Java 8 due to CORBA libraries and 32 bit due to JNA access.<br>
Install jdk-8u202-windows-i586.exe (taken from [oracle.com](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html)).<br>
Latest version is 1.8.0_381. Message to update will appear.

## Registration of Lotus Notes DLLs
During compilation time the following DLL files are needed.
Please use the appropriated commands (see below) for specific version.

### 9.0.1 (Windows)
    mvn install:install-file -Dfile="C:\Program Files (x86)\IBM\Notes\jvm\lib\ext\Notes.jar" -DgroupId=com.ibm -DartifactId=domino-api-binaries -Dversion=9.0.1 -Dpackaging=jar
    mvn install:install-file -Dfile="C:\Program Files (x86)\HCL\Notes\jvm\lib\ext\Notes.jar" -DgroupId=com.ibm -DartifactId=domino-api-binaries -Dversion=9.0.1 -Dpackaging=jar
    mvn install:install-file -Dfile="C:\Users\armin\Desktop\Notes.jar" -DgroupId=com.ibm -DartifactId=domino-api-binaries -Dversion=9.0.1 -Dpackaging=jar

### 9.0.1 (MacOS)
    mvn install:install-file -Dfile="/Applications/IBM Notes.app/Contents/MacOS/jvm/lib/ext/Notes.jar" -DgroupId=com.ibm -DartifactId=domino-api-binaries -Dversion=9.0.1 -Dpackaging=jar
    (Mac OS X)

### 12.0.0 (Windows)
    mvn install:install-file -Dfile="C:\Program Files (x86)\HCL\Notes\jvm\lib\ext\Notes.jar" -DgroupId=com.ibm -DartifactId=domino-api-binaries -Dversion=12.0.0 -Dpackaging=jar
    mvn install:install-file -Dfile="C:\Program Files (x86)\HCL\Notes\osgi\shared\eclipse\plugins\com.ibm.commons_12.0.0.20210508-0545\lwpd.commons.jar" -DgroupId=com.ibm -DartifactId=ibm-commons -Dversion=12.0.0 -Dpackaging=jar
    mvn install:install-file -Dfile="C:\Program Files (x86)\HCL\Notes\osgi\shared\eclipse\plugins\com.ibm.domino.napi_12.0.0.20210508-0545\lwpd.domino.napi.jar" -DgroupId=com.ibm -DartifactId=napi -Dversion=12.0.0 -Dpackaging=jar

### 12.0.1 (Windows)
    mvn install:install-file -Dfile="C:\Program Files (x86)\HCL\Notes\jvm\lib\ext\Notes.jar" -DgroupId=com.ibm -DartifactId=domino-api-binaries -Dversion=12.0.1 -Dpackaging=jar
    mvn install:install-file -Dfile="C:\Program Files (x86)\HCL\Notes\osgi\shared\eclipse\plugins\com.ibm.commons_12.0.1.20211117-2131\lwpd.commons.jar" -DgroupId=com.ibm -DartifactId=ibm-commons -Dversion=12.0.1 -Dpackaging=jar
    mvn install:install-file -Dfile="C:\Program Files (x86)\HCL\Notes\osgi\shared\eclipse\plugins\com.ibm.domino.napi_12.0.1.20211117-2131\lwpd.domino.napi.jar" -DgroupId=com.ibm -DartifactId=napi -Dversion=12.0.1 -Dpackaging=jar

## Build
mvn clean install

## Run
Add "C:\Program Files (x86)\HCL\Notes" to the "PATH" variable.
