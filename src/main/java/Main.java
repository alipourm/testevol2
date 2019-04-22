import org.apache.commons.cli.*;



public class Main {

    public static void main(String[] args) {
        Options options = new Options();

        Option repoPathOption = new Option("r", "path to the directory");
        Option outPathOption = new Option("o", "output file");
        Option logPathOption = new Option("l", "logfile");

        repoPathOption.isRequired();
        outPathOption.isRequired();
        logPathOption.isRequired();


        options.addOption(repoPathOption);
        options.addOption(outPathOption);
        options.addOption(logPathOption);



        CommandLineParser parser = new DefaultParser();
        try {
        CommandLine cmdLine = parser.parse(options, args);

        String repoPath =  cmdLine.getOptionValue("r");
        String outPath  = cmdLine.getOptionValue("o");
        String logPath = cmdLine.getOptionValue("log");

        }
        catch (ParseException e){
            e.fillInStackTrace();
        }



    }

}
