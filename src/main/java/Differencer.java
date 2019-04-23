import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.diff.DiffEntry;


import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Differencer implements Task {

    List<RevCommit> revisions;
    Git git;
    Repository repository;

    public Differencer(Git _git) throws GitAPIException {
        git = _git;
        this.getRevisions();
        repository = git.getRepository();

    }


    public void getRevisions() throws GitAPIException {
        Iterable<RevCommit> revCommitList =  git.log().call();
        Iterator<RevCommit> iter = revCommitList.iterator();
        revisions = new ArrayList<RevCommit>();

        while (iter.hasNext()){
            revisions.add(iter.next());
        }

    }

    public void setRevisions(List revisions){
        this.revisions = revisions;
    }

    public void diffFiles(RevCommit head, RevCommit oldHead){
        try {
//            oldHead = repository.resolve(revstrOld);
//            ObjectId head = repository.resolve(revstrNew);
            ObjectReader reader = repository.newObjectReader();
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, oldHead.getTree());
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, head.getTree());

            // finally get the list of changed files
            try  {
                List<DiffEntry> diffs= git.diff()
                        .setNewTree(newTreeIter)
                        .setOldTree(oldTreeIter)
                        .call();
                for (DiffEntry entry : diffs) {
                    System.out.println("Entry: " + entry);

                }
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        } catch (IncorrectObjectTypeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    public void go() {
        for(int i = 0; i < revisions.size() - 1; i++){
            diffFiles(revisions.get(i), revisions.get(i+1));
            RevCommit base = revisions.get(i);
            System.out.println("#######" + i + " " + new Time(revisions.get(i).getCommitTime()) + " " + base.getShortMessage());

        }

    }
}
