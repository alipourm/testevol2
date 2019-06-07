import DiffResult.CSVWriter;
import DiffResult.Result;
import org.apache.commons.cli.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;


public class Main {

    public static void main(String[] args) {
        Options options = new Options();

        Option repoPathOption = new Option("repository", "Path to the repository folder");
        Option prevCommit = new Option("prev", "Previous full commit hash");
        Option currentCommit = new Option("current", "Current full commit hash");

        repoPathOption.setRequired(true);
        repoPathOption.setArgs(1);
        prevCommit.setRequired(true);
        prevCommit.setArgs(1);
        currentCommit.setRequired(true);
        currentCommit.setArgs(1);

        options.addOption(repoPathOption);
        options.addOption(prevCommit);
        options.addOption(currentCommit);

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmdLine = parser.parse(options, args);

            String repoPath =  cmdLine.getOptionValue("repository");
            String prevCommitHash  = cmdLine.getOptionValue("prev");
            String currentCommitHash = cmdLine.getOptionValue("current");

            Differencer d = new Differencer(Git.open(new File(repoPath)));

            d.goWithCommits(prevCommitHash, currentCommitHash);

            Result result = Result.getResultInstance();
            CSVWriter csvWriter = new CSVWriter(result);
            csvWriter.write();

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
