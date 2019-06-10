package DiffResult;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResultItem
{
    public enum LEVEL {
        FILE, METHOD, ASSERTION
    }

    public LEVEL level;
    public String path;
    public String action;
    public String what;
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

    public ArrayList<String> toArray() {
        return (ArrayList<String>) Stream.of(
                level.toString(), path, action, what, from, to,
                Long.toString(loc), Long.toString(changed_loc), is_test_file ? "true": "false",
                Long.toString(smells), Long.toString(test_methods), Long.toString(test_ignored),
                Long.toString(methods), Long.toString(statements), Long.toString(changed_methods),
                Long.toString(changed_statements)
        ).collect(Collectors.toList());
    }
}
