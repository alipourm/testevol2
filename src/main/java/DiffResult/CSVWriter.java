package DiffResult;

import testsmell.ResultsWriter;

import java.io.IOException;

public class CSVWriter implements Writer
{
    private Result result;

    public CSVWriter(Result result) {
        this.result = result;
    }

    @Override
    public void write(String fileName) throws IOException {
        ResultsWriter resultsWriter = ResultsWriter.getResultsWriterInstance(fileName);
        resultsWriter.writeColumnName(Result.getColumns());
        result.getResultItems().forEach(resultItem -> {
            try {
                resultsWriter.writeLine(resultItem.toArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        resultsWriter.finish();
    }
}
