import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.diff.DiffEntry;

import gumtree.spoon.*;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Differencer implements Task {

    List<RevCommit> revisions;
    Git git;
    Repository repository;
    TreeWalk treeWalk;

    public Differencer(Git _git) throws GitAPIException {
        git = _git;
        this.getRevisions();
        repository = git.getRepository();
        treeWalk = new TreeWalk(repository);
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

    public void astDiff(ObjectId newObj, ObjectId oldObj, String filePath){
        TreeWalk treeWalk = new TreeWalk(repository);

        // head
        try {


            File oldTmpFile = File.createTempFile("old", ".java");
            File newTmpFile = File.createTempFile("old", ".java");

            ObjectLoader newLoader = repository.open(newObj);
            newLoader.copyTo(new FileOutputStream(newTmpFile));

            ObjectLoader oldLoader = repository.open(oldObj);
            oldLoader.copyTo(new FileOutputStream(oldTmpFile));

            Diff astDiffs = new AstComparator().compare(newTmpFile, oldTmpFile);
//            Iterator<> it = astDiffs.getAllOperations().iterator();
            System.out.println("Changes are:");

            for (Operation op:astDiffs.getRootOperations()){
                System.out.println("op "+op);

             //   System.out.println(op.getSrcNode().toString());
            //    System.out.println(op.getDstNode().toString());

            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

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

                    if (entry.toString().trim().endsWith("java]"))
                    {


                        // This is only for MODIFY but we have ADD, MOV, RM too.
                        DiffEntry.ChangeType changeType = entry.getChangeType();

                        if (changeType ==DiffEntry.ChangeType.MODIFY
                            //    changeType==DiffEntry.ChangeType.ADD
                        )
                        {
                            String st = entry.getNewPath();
                            ObjectId newObjectId = entry.getNewId().toObjectId();
                            ObjectId oldObjectId = entry.getOldId().toObjectId();
                            astDiff(newObjectId, oldObjectId, "");

                        }
                    }
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
