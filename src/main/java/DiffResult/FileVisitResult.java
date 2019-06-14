package DiffResult;

import testsmell.TestFile;

public class FileVisitResult {
    public long lineCount;
    public long methods;
    public long statements;
    public long testMethods;
    public long testIgnoredMethods;
    public TestFile testFile;

    public boolean isTestFile() {
        return this.testMethods > 0;
    }
}
