import DiffResult.CSVWriter;
import DiffResult.Result;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.ObjectUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;


public class Main {

    public static void main(String[] args) {
        Options options = new Options();

        Option repoPathOption = new Option("repository", "Path to the repository folder. Default: Current Directory");
        Option prevCommit = new Option("prev", "Previous full commit hash. Default: First Commit");
        Option currentCommit = new Option("current", "Current full commit hash. Default: Last Commit");
        Option follow = new Option("follow", "Follow commits from prev to current. Default: false");
        Option fileName = new Option("dest", "Destination File Name");

        repoPathOption.setArgs(1);
        prevCommit.setArgs(1);
        currentCommit.setArgs(1);
        follow.setArgs(0);
        fileName.setArgs(1);

        options.addOption(repoPathOption)
                .addOption(prevCommit)
                .addOption(currentCommit)
                .addOption(follow)
                .addOption(fileName);

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmdLine = parser.parse(options, args);

            String repoPath = ObjectUtils.defaultIfNull(cmdLine.getOptionValue("repository"), ".");

            String prevCommitHash = cmdLine.getOptionValue("prev");
            String currentCommitHash = cmdLine.getOptionValue("current");

            boolean shouldFollow = cmdLine.hasOption("follow");
            String destFileName = cmdLine.getOptionValue("dest");

            Differencer d = new Differencer(Git.open(new File(repoPath)));

            prevCommitHash = (prevCommitHash == null) ? d.getFirstCommit() : prevCommitHash;
            currentCommitHash = (currentCommitHash == null) ? d.getLastCommit() : currentCommitHash;

            if (shouldFollow)
                d.followCommits(prevCommitHash, currentCommitHash);
            else
                d.goWithCommits(prevCommitHash, currentCommitHash);

            Result result = Result.getResultInstance();
            CSVWriter csvWriter = new CSVWriter(result);
            csvWriter.write(destFileName);

        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Result result = Result.getResultInstance();
            CSVWriter csvWriter = new CSVWriter(result);
            try {
                csvWriter.write("withError");
            } catch (IOException i) {
                System.err.println("Problem writing final file!");
            }
        } catch (GitAPIException e) {
            e.printStackTrace();
        }


    }

}
