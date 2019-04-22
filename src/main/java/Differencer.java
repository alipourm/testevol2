import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;

public class Differencer implements Task {

    public void diffFiles(Git git){
        Repository repository = git.getRepository();
        ObjectId oldHead = null;
        try {
            oldHead = repository.resolve("HEAD^^^^{tree}");
            ObjectId head = repository.resolve("HEAD^{tree}");

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Printing diff between tree: " + oldHead + " and " + head);

        // prepare the two iterators to compute the diff between
        try (ObjectReader reader = repository.newObjectReader()) {
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, oldHead);
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, head);

            // finally get the list of changed files
            try (Git git = new Git(repository)) {
                List<DiffEntry> diffs= git.diff()
                        .setNewTree(newTreeIter)
                        .setOldTree(oldTreeIter)
                        .call();
                for (DiffEntry entry : diffs) {
                    System.out.println("Entry: " + entry);
                }
            }
        }
    }
    }



    public void go() {

    }
}
