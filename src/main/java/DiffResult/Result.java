package DiffResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Result {

    private static Result result;

    private static List<String> columns = Stream.of(
            "level", "path", "action", "what", "from", "to", "loc", "changed_loc", "is_test_file", "smells",
            "test_methods", "test_ignored"
    ).collect(Collectors.toList());

    private ResultItem current;
    private ArrayList<ResultItem> resultItems = new ArrayList<>();

    private Result() {}

    public static Result getResultInstance() {
        if (result==null)
            result = new Result();
        return result;
    }

    public ResultItem currentItem() {
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

}
