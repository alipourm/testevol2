package DiffResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResultItem
{
    public enum LEVEL {
        FILE, METHOD, COMMIT
    }

    public LEVEL level;
    public int commit_counts;
    public String newCommitAuthor;
    public long commitIndex;
    public String commit;
    public int commitTime;
    public String commitMessage;
    public boolean isBugFix;
    public String path;
    public String action;
    public String what;
    public String lineOfCode;
    public String testCaseName;
    public String from;
    public String to;
    public long loc;
    public long changed_loc;
    public boolean is_test_file;
    public long smells;
    public long test_methods;
    public long test_ignored;
    public long methods;
    public long statements;
    public long changed_methods;
    public long changed_statements;
    public long no_add=0;
    public long no_update=0;
    public long no_delete=0;
    public List<String> smell_types = new ArrayList<>();

    public ArrayList<String> toArray() {
        boolean is_assert = false;
        if (from != null && from.length() > 6 && from.substring(0,6).equals("assert"))
            is_assert = true;
        if (to != null && to.length() > 6 && to.substring(0,6).equals("assert"))
            is_assert = true;

        ArrayList<String> toStringResult = (ArrayList<String>) Stream.of(
                level.toString(), Integer.toString(commit_counts), newCommitAuthor, Long.toString(commitIndex), commit, String.valueOf(commitTime) , commitMessage, isBugFix ? "true" : "",
                path, action, what, lineOfCode, testCaseName, is_assert ? "true" : "", from, to,
                Long.toString(loc), Long.toString(changed_loc), is_test_file ? "true": "",
                Long.toString(smells), Long.toString(test_methods), Long.toString(test_ignored),
                Long.toString(methods), Long.toString(statements), Long.toString(changed_methods),
                Long.toString(changed_statements), Long.toString(no_add), Long.toString(no_update),
                Long.toString(no_delete)
        ).collect(Collectors.toList());

        toStringResult.addAll(smell_types);

        return toStringResult;
    }
}
