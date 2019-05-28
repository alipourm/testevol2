package DiffResult;

import java.util.ArrayList;

public class DiffResultAdded implements DiffResultInterface {

    enum ClassType {
        INTERFACE, CLASS
    }

    private String filePath;
    public ClassType classType;
    private int publicMethods, privateMethods;

    @Override
    public DiffType getDiffType() {
        return DiffType.ADDED;
    }

    @Override
    public String getFilePath() {
        return this.filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

}
