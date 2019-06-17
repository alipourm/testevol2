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
        Option follow = new Option("follow", "Follow commits from prev to current");
        Option fileName = new Option("dest", "Destination File Name");

        repoPathOption.setRequired(true);
        repoPathOption.setArgs(1);
        prevCommit.setRequired(true);
        prevCommit.setArgs(1);
        currentCommit.setRequired(true);
        currentCommit.setArgs(1);
        follow.setArgs(0);
        fileName.setArgs(1);

        options.addOption(repoPathOption);
        options.addOption(prevCommit);
        options.addOption(currentCommit);
        options.addOption(follow);
        options.addOption(fileName);

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmdLine = parser.parse(options, args);

            String repoPath =  cmdLine.getOptionValue("repository");
            String prevCommitHash  = cmdLine.getOptionValue("prev");
            String currentCommitHash = cmdLine.getOptionValue("current");
            boolean shouldFollow = cmdLine.hasOption("follow");
            String destFileName = cmdLine.getOptionValue("dest");

            Differencer d = new Differencer(Git.open(new File(repoPath)));

            if (shouldFollow)
                d.followCommits(prevCommitHash, currentCommitHash);
            else
                d.goWithCommits(prevCommitHash, currentCommitHash);

            Result result = Result.getResultInstance();
            CSVWriter csvWriter = new CSVWriter(result);
            csvWriter.write(destFileName);

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
