import org.apache.commons.cli.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;


public class Main {

    public static void main(String[] args) {
        Options options = new Options();

        Option repoPathOption = new Option("r", "path to the directory");
        Option outPathOption = new Option("o", "output file");
        Option logPathOption = new Option("l", "logfile");

        repoPathOption.setRequired(true);
        repoPathOption.setArgs(1);
//        outPathOption.setRequired(true);
//        logPathOption.setRequired(true);


        options.addOption(repoPathOption);
        options.addOption(outPathOption);
        options.addOption(logPathOption);



        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmdLine = parser.parse(options, args);

            String repoPath =  cmdLine.getOptionValue("r");
            String outPath  = cmdLine.getOptionValue("o");
            String logPath = cmdLine.getOptionValue("log");
            System.out.println("repoPat "+ repoPath );

            Differencer d = new Differencer(Git.open(new File(repoPath)));
            System.out.println("repoPat");
            d.go();
        }
        catch (ParseException exp){
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }


    }

}
