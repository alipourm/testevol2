package DiffResult;

import java.util.ArrayList;

public interface DiffResultInterface {
    enum DiffType {
        ADDED, DELETED, RENAMED, MODIFIED
    }

    DiffType getDiffType();
    String getFilePath();

}
