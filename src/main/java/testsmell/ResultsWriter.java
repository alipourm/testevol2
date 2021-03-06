package testsmell;

import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;

/**
 * This class is utilized to write output to a CSV file
 */
public class ResultsWriter {

    private static ResultsWriter resultsWriter;

    private FileWriter writer;

    /**
     * Creates the file into which output it to be written into. Results from each file will be stored in a new file
     * @throws IOException
     */
    private ResultsWriter(String fileName) throws IOException {
        String time =  String.valueOf(Calendar.getInstance().getTimeInMillis());
        String outputFile;
        if (fileName != null && !fileName.equals(""))
            outputFile = fileName;
        else
            outputFile = MessageFormat.format("{0}_{1}_{2}.{3}", "Output", "TestEvol", time, "csv");
        writer = new FileWriter(outputFile,false);
    }

    /**
     * Factory method that provides a new instance of the ResultsWriter
     * @return new ResultsWriter instance
     * @throws IOException
     */

    public static ResultsWriter getResultsWriterInstance(String fileName) throws IOException {
        if (resultsWriter == null)
            resultsWriter = new ResultsWriter(fileName);
        return resultsWriter;
    }

    /**
     * Writes column names into the CSV file
     * @param columnNames the column names
     * @throws IOException
     */
    public void writeColumnName(List<String> columnNames) throws IOException {
        writeOutput(columnNames);
    }

    /**
     * Writes column values into the CSV file
     * @param columnValues the column values
     * @throws IOException
     */
    public void writeLine(List<String> columnValues) throws IOException {
        writeOutput(columnValues);
    }

    /**
     * Appends the input values into the CSV file
     * @param dataValues the data that needs to be written into the file
     * @throws IOException
     */
    private void writeOutput(List<String> dataValues)throws IOException {
        for (int i=0; i<dataValues.size(); i++) {
            writer.append(StringEscapeUtils.escapeCsv(dataValues.get(i)));

            if(i!=dataValues.size()-1)
                writer.append(",");
            else
                writer.append(System.lineSeparator());

        }
    }

    public void finish() throws IOException {
        writer.flush();
        writer.close();
    }
}
