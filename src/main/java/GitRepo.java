import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.util.Iterator;

public class GitRepo {
    private Git git;

    public GitRepo(String path) throws Exception {
        Git git = Git.open(new File(path));
    }

    public boolean traverse() {

        try {
            Iterable<RevCommit> rev = git.log().call();
            Iterator<RevCommit> it = rev.iterator();
            while (it.hasNext()) {
                RevCommit curCommit = it.next();
                git.diff();
                it.next().getFullMessage();
            }
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return false;
    }
}


