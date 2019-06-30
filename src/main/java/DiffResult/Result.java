package DiffResult;

import testsmell.TestSmellDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Result {

    private static Result result;

    private static List<String> columns = Stream.of(
            "level", "commit_counts", "new_commit_author", "commit_index", "commit", "commit_time", "is_bug_fix",
            "path", "action", "what", "line_of_code", "is_assert", "from", "to",
            "loc", "changed_loc", "is_test_file",
            "smells", "test_methods", "test_ignored",
            "methods", "statements", "changed_methods",
            "changed_statements", "no_add", "no_update",
            "no_delete"
    ).collect(Collectors.toList());



    private ResultItem current;
    private ArrayList<ResultItem> resultItems = new ArrayList<>();

    private Result() {
        TestSmellDetector testSmellDetector = new TestSmellDetector();
        columns.addAll(testSmellDetector.getTestSmellNames());
    }

    public static Result getResultInstance() {
        if (result==null)
            result = new Result();
        return result;
    }

    public ResultItem getCurrentItem() {
        return current;
    }

    public ResultItem createItem() {
        current = new ResultItem();
        return current;
    }

    public void addResultItem(ResultItem resultItem) {
        resultItems.add(resultItem);
    }

    public static List<String> getColumns() {
        return columns;
    }

    public ArrayList<ResultItem> getResultItems() {
        return resultItems;
    }

    public ArrayList<ResultItem> getFileLevelResultItemByCommit(String commit) {
        Stream<ResultItem> s = this.resultItems
                .stream()
                .filter(resultItem -> resultItem.commit != null)
                .filter(resultItem -> resultItem.commit.equals(commit))
                .filter(resultItem -> resultItem.level == ResultItem.LEVEL.FILE);

        System.out.println(commit);
        return s.collect(Collectors.toCollection(ArrayList::new));
    }
}
