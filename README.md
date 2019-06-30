BEAGLE -- version 1.0.0
================
BEAGLE is a lightweight tool for providing a comprehensive picture of the evolution of tests in the project history. 
It provides information about the quality of tests measured by test smells. BEAGLE also tracks changes at various 
granularity from single-line to file level with minimal set up required from users. Currently BEAGLE works for Java
projects only.

Using BEAGLE
===========
#### Requirements
1. Download latest JAR file from [release page](https://github.com/alipourm/testevol2/releases)
2. Make sure you have Java 1.8 installed

#### How to run
`COMMIT_A` and `COMMIT_B` are two hash commits which A is before B. 
1. Track changes between two commit hashes of a local git repository
    - `java -jar beagle.jar -repository <PATH_TO_REPOSITORY> -prev <COMMIT_A> -current <COMMIT_B>`
2. Follow commits from A->B and track changes for each pair of commits
    - `java -jar beagle.jar -repository <PATH_TO_REPOSITORY> -prev <COMMIT_A> -current <COMMIT_B> -follow`
3. Set specific file name for the output CSV file
    - `java -jar beagle.jar -repository <PATH_TO_REPOSITORY> -prev <COMMIT_A> -current <COMMIT_B> -dest <FILE_NAME>`
    

Compile the code
----------------
1. clone the source code
2. run `mvn package`
3. Look for the JAR file located in target directory 