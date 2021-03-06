import DiffResult.*;
import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.RenameDetector;
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
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.support.reflect.declaration.CtMethodImpl;
import testsmell.AbstractSmell;
import testsmell.SmellyElement;
import testsmell.TestFile;
import testsmell.TestSmellDetector;
import util.GitMessage;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class Differencer implements Task {

    private List<RevCommit> revisions;
    private Git git;
    private Repository repository;
    private ArrayList<TestFile> testFiles;
    private Result result;

    Differencer(Git _git) throws GitAPIException {
        git = _git;
        this.getRevisions();
        repository = git.getRepository();
        testFiles = new ArrayList<>();
        this.result = Result.getResultInstance();
    }

    private void getRevisions() throws GitAPIException {
        Iterable<RevCommit> revCommitList = git.log().call();
        Iterator<RevCommit> iter = revCommitList.iterator();
        revisions = new ArrayList<RevCommit>();

        while (iter.hasNext()) {
            revisions.add(iter.next());
        }
    }

    public void setRevisions(List revisions) {
        this.revisions = revisions;
    }

    public void astDiffModify(ObjectId newObj, ObjectId oldObj, String filePath) {

        // head
        try {


            File oldTmpFile = File.createTempFile("old", ".java");
            File newTmpFile = File.createTempFile("new", ".java");
            ObjectLoader newLoader = repository.open(newObj);
            newLoader.copyTo(new FileOutputStream(newTmpFile));

            ObjectLoader oldLoader = repository.open(oldObj);
            oldLoader.copyTo(new FileOutputStream(oldTmpFile));


            Diff astDiffs = new AstComparator().compare(oldTmpFile, newTmpFile);

            ResultItem fileResultItem = Result.getResultInstance().getCurrentItem();

            for (Operation op : astDiffs.getRootOperations()) {

                if (op.getAction() instanceof Addition)
                    fileResultItem.no_add++;
                else if (op.getAction() instanceof Update)
                    fileResultItem.no_update++;
                else if (op.getAction() instanceof Delete)
                    fileResultItem.no_delete++;

                ResultItem resultItem = this.result.createItem();
                resultItem.commit = fileResultItem.commit;
                resultItem.commitMessage = fileResultItem.commitMessage;
                resultItem.level = ResultItem.LEVEL.METHOD;
                resultItem.path = filePath;
                resultItem.action = op.getAction().getClass().getSimpleName();
                resultItem.what = op.getNode().getClass().getSimpleName().substring(2, op.getNode().getClass().getSimpleName().length() -4 );

                // action position
                CtElement element = op.getNode();
                CtElement parent = element;
                while (parent.getParent() != null && !(parent.getParent() instanceof CtPackage)) {
                    parent = parent.getParent();
                }
                String position = "";
                if (parent instanceof CtType) {
                    position += ((CtType) parent).getQualifiedName();
                }
                if (element.getPosition() != null && !(element.getPosition() instanceof NoSourcePosition)) {
                    position += ":" + element.getPosition().getLine();
                }
                if (op.getAction() instanceof Move) {
                    CtElement elementDest = (CtElement) op.getAction().getNode().getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT_DEST);
                    position = element.getParent(CtClass.class).getQualifiedName();
                    if (element.getPosition() != null && !(element.getPosition() instanceof NoSourcePosition)) {
                        position += ":" + element.getPosition().getLine();
                    }
                    resultItem.from = position;
                    position = elementDest.getParent(CtClass.class).getQualifiedName();
                    if (elementDest.getPosition() != null && !(elementDest.getPosition() instanceof NoSourcePosition)) {
                        position += ":" + elementDest.getPosition().getLine();
                    }
                    resultItem.to = position;
                } else {
                    resultItem.lineOfCode = position;

                    if(element instanceof CtMethodImpl)
                        resultItem.from = ((CtMethodImpl) element).getSimpleName();
                    else {
                        CtElement methodElement = element; // Find the method
                        while (methodElement != null && !(methodElement instanceof CtMethodImpl)) {
                            methodElement = methodElement.getParent();
                        }
                        if (methodElement != null) {
                            resultItem.is_test_file = methodElement.getAnnotations().stream().anyMatch(a -> a.getAnnotationType().getSimpleName().equals("Test"));
                            if (resultItem.is_test_file) {
                                resultItem.testCaseName = ((CtMethodImpl) methodElement).getSimpleName();
                            }
                        }
                        resultItem.from = element.toString();
                    }

                    resultItem.to = op.getDstNode() != null ? op.getDstNode().toString() : "";
                }

                result.addResultItem(resultItem);
            }
            oldTmpFile.delete();
            newTmpFile.delete();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private class ClassVisitor extends VoidVisitorAdapter {
        private String filePath;
        private String realFilePath;

        ClassVisitor(String filePath, String realFilePath) {
            this.filePath = filePath;
            this.realFilePath = realFilePath;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg) {
            super.visit(n, arg);

            Path path = Paths.get(filePath);
            long lineCount = 0;

            try(Stream<String> s = Files.lines(path, Charset.forName("ISO-8859-1"))) {
                lineCount = s.count();
            } catch (Exception e) {
                e.printStackTrace();
            }

            FileVisitResult result = (FileVisitResult) arg;
            int methods = (int) n.getMethods().stream().filter(m -> m.getAnnotationByName("Test").isPresent() || m.getAnnotationByName("org.junit.Test").isPresent()).count();
            int ignored = (int) n.getMethods().stream().filter(m -> m.getAnnotationByName("Ignore").isPresent() || m.getAnnotationByName("org.junit.Ignore").isPresent() ).count();

            result.lineCount = lineCount;
            result.methods = (long) n.getMethods().size();
            result.statements = (long) n.getChildNodesByType(ExpressionStmt.class).size();
            result.testMethods = (long) methods;
            result.testIgnoredMethods = (long) ignored;

            if ( methods > 0) result.testFile = new TestFile(realFilePath, filePath, "");
        }
    }

    private FileVisitResult visitFile(File file, String filePath) {
        try {
            CompilationUnit compilationUnit = JavaParser.parse(file);

            ClassVisitor classVisitor = new ClassVisitor(file.getAbsolutePath(), filePath);
            FileVisitResult fileVisitResult = new FileVisitResult();

            classVisitor.visit(compilationUnit, fileVisitResult);
            return fileVisitResult;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private DiffResultAdded astDiffAdd(ObjectId newObj, String filePath) {

        DiffResultAdded diffResultAdded = new DiffResultAdded();
        diffResultAdded.setFilePath(filePath);


        // head
        try {
            File tmpFile = File.createTempFile("new", ".java");

            ObjectLoader newLoader = repository.open(newObj);
            newLoader.copyTo(new FileOutputStream(tmpFile));

            FileVisitResult result = visitFile(tmpFile, filePath);
            ResultItem resultItem = Result.getResultInstance().getCurrentItem();
            resultItem.methods = result.methods;
            resultItem.statements = result.statements;

//            if (compilationUnit. (MethodDeclaration.class).stream()
//                    .filter(Util::isValidTestMethod).count() > 0) {
//                System.out.println("!!!!! Found Test method");
//            }

            try {
                tmpFile.delete();
            } catch (SecurityException e) {

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return diffResultAdded;
    }


    public void diffFiles(RevCommit head, RevCommit oldHead) {
        try {
//            oldHead = repository.resolve(revstrOld);
//            ObjectId head = repository.resolve(revstrNew);
            ObjectReader reader = repository.newObjectReader();
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();

            oldTreeIter.reset(reader, oldHead.getTree());
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();

            newTreeIter.reset(reader, head.getTree());
            TestSmellDetector testSmellDetector = TestSmellDetector.createTestSmellDetector();

            // finally get the list of changed files
            try {
                List<DiffEntry> diffs = git.diff()
                        .setNewTree(newTreeIter)
                        .setOldTree(oldTreeIter)
                        .call();
                RenameDetector rd = new RenameDetector(repository);
                rd.addAll(diffs);
                diffs = rd.compute();

                ArrayList<DiffResultInterface> diffResult = new ArrayList<>();

                for (DiffEntry entry : diffs) {

                    if (entry.toString().trim().endsWith("java]")) {
                        DiffEntry.ChangeType changeType = entry.getChangeType();
                        ObjectId newObjectId = null;
                        ObjectId oldObjectId = null;
                        String filePatth = null;

                        ResultItem resultItem = this.result.createItem();
                        resultItem.level = ResultItem.LEVEL.FILE;
                        resultItem.path = entry.getOldPath().equals("/dev/null") ? "" : entry.getOldPath();
                        resultItem.action = changeType.name();
                        resultItem.commit = head.getName();
                        resultItem.commitTime = head.getCommitTime();

                        File tmpFile = File.createTempFile("new", ".java");

                        if (changeType != DiffEntry.ChangeType.DELETE) { // In case of deletion, there is no new object to get
                            ObjectLoader newLoader = repository.open(entry.getNewId().toObjectId());
                            newLoader.copyTo(new FileOutputStream(tmpFile));
                        } else {
                            ObjectLoader newLoader = repository.open(entry.getOldId().toObjectId());
                            newLoader.copyTo(new FileOutputStream(tmpFile));
                        }
                        FileVisitResult resultNewFile;
                        try {
                            resultNewFile = visitFile(tmpFile.getAbsoluteFile(), entry.getNewPath());
                        } catch (com.github.javaparser.ParseProblemException e) {
                            resultItem.path = entry.getNewPath();
                            resultItem.what = "EXCEPTION OCCURRED!";
                            this.result.addResultItem(resultItem);
                            tmpFile.delete();
                            continue;
                        }

                        assert resultNewFile != null;
                        resultItem.methods = resultNewFile.methods;
                        resultItem.statements = resultNewFile.statements;

                        resultItem.is_test_file = resultNewFile.isTestFile();
                        resultItem.test_methods = resultNewFile.testMethods;
                        resultItem.test_ignored = resultNewFile.testIgnoredMethods;

                        Path path = Paths.get(tmpFile.getAbsolutePath());
                        long lineCount = 0;

                        try(Stream<String> s = Files.lines(path)) {
                            lineCount = s.count();
                        } catch (Exception e) {

                        }

                        switch (changeType) {
                            case MODIFY:
                                File oldTmpFile = File.createTempFile("old", ".java");

                                ObjectLoader oldLoader = repository.open(entry.getOldId().toObjectId());
                                oldLoader.copyTo(new FileOutputStream(oldTmpFile));
                                FileVisitResult resultOldFile = null;

                                try {
                                    resultOldFile = visitFile(oldTmpFile.getAbsoluteFile(), entry.getOldPath());
                                } catch (com.github.javaparser.ParseProblemException e) {
                                    resultItem.from = "EXCEPTION OCCURRED!";
                                    this.result.addResultItem(resultItem);
                                    oldTmpFile.delete();
                                    continue;
                                }

                                assert resultOldFile != null;

                                resultItem.changed_methods = resultNewFile.methods - resultOldFile.methods;
                                resultItem.changed_statements = resultNewFile.statements - resultOldFile.statements;


                                Path oldPath = Paths.get(oldTmpFile.getAbsolutePath());
                                long oldLineCount = 0;
                                try(Stream<String> s = Files.lines(oldPath)) {
                                    oldLineCount = s.count();
                                } catch (Exception e) {

                                }
                                resultItem.loc = lineCount;
                                resultItem.changed_loc = lineCount - oldLineCount;

                                filePatth = entry.getNewPath();
                                newObjectId = entry.getNewId().toObjectId();
                                oldObjectId = entry.getOldId().toObjectId();
                                astDiffModify(newObjectId, oldObjectId, filePatth);
                                try {
                                    oldTmpFile.delete();
                                } catch (SecurityException e) {

                                }
                                break;
                            case ADD:
                                resultItem.what = entry.getNewPath();
                                resultItem.loc = lineCount;
                                filePatth = entry.getNewPath();
                                newObjectId = entry.getNewId().toObjectId();
                                astDiffAdd(newObjectId, filePatth);
                                break;
                            case DELETE:

                                break;

                            case RENAME:

                                break;

                            case COPY:

                                break;
                        }
                        if (resultNewFile.isTestFile()) {
                            testSmellDetector.detectSmells(resultNewFile.testFile);
                            resultItem.smells = resultNewFile.testFile.getNumOfSmells();

                            for (AbstractSmell smell : resultNewFile.testFile.getTestSmells()) {
                                try {
                                    if (smell.getHasSmell())
                                        resultItem.smell_types.add(String.valueOf(smell.getSmellyElements().stream().filter(SmellyElement::getHasSmell).count()));
                                    else
                                        resultItem.smell_types.add("0");
                                }
                                catch (NullPointerException e){
                                    resultItem.smell_types.add("0");
                                }
                            }
                        }

                        this.result.addResultItem(resultItem);
                        try {
                            tmpFile.delete();
                        } catch (SecurityException e) {

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
        System.out.println("Done!");

    }

    private RevCommit findCommit(String commit) {
        return revisions.stream().filter(r -> r.name().equals(commit)).findFirst().orElse(null);
    }

    private int findCommitIndex(RevCommit revCommit) {
        return revisions.indexOf(revCommit);
    }


    public void go() {
        for (int i = 0; i < revisions.size() - 1; i++) {
            goWithCommits(revisions.get(i), revisions.get(i + 1));
        }
    }

    public String getFirstCommit() {
        return revisions.get(revisions.size()-1).name();
    }

    public String getLastCommit() {
        return revisions.get(0).name();
    }

    public void followCommits(String prevCommit, String currentCommit) {
        RevCommit prevRevCommit = findCommit(prevCommit);
        RevCommit currentRevCommit = findCommit(currentCommit);
        for (int i = revisions.size() - findCommitIndex(prevRevCommit); i < revisions.size() - findCommitIndex(currentRevCommit); i++) {
            goWithCommits(revisions.get(revisions.size() - i), revisions.get(revisions.size() - (i + 1)));
        }
    }

    private void goWithCommits(RevCommit prevRevCommit, RevCommit currentRevCommit) {
        Result result = Result.getResultInstance();
        ResultItem resultItem = result.createItem();
        resultItem.level = ResultItem.LEVEL.COMMIT;
        resultItem.from = prevRevCommit.getName();
        resultItem.to = currentRevCommit.getName();
        resultItem.isBugFix = GitMessage.isBugFix(currentRevCommit);
        resultItem.commit = currentRevCommit.getName();
        resultItem.commitTime = currentRevCommit.getCommitTime();
        resultItem.commitMessage = currentRevCommit.getFullMessage();
        resultItem.newCommitAuthor = currentRevCommit.getAuthorIdent().getName();
        resultItem.commit_counts = findCommitIndex(prevRevCommit) - findCommitIndex(currentRevCommit);
        resultItem.commitIndex = findCommitIndex(currentRevCommit);
        result.addResultItem(resultItem);

        diffFiles(currentRevCommit, prevRevCommit);

        ArrayList<ResultItem> resultItemsForCommit = result.getFileLevelResultItemByCommit(currentRevCommit.getName());
        resultItemsForCommit.forEach(resultItem1 -> {
            resultItem.loc += resultItem1.loc;
            resultItem.changed_loc += resultItem1.changed_loc;
            resultItem.is_test_file = resultItem1.is_test_file || resultItem.is_test_file;
            resultItem.smells += resultItem1.smells;
            resultItem.test_methods += resultItem1.test_methods;
            resultItem.test_ignored += resultItem1.test_ignored;
            resultItem.methods += resultItem1.methods;
            resultItem.statements += resultItem1.statements;
            resultItem.no_delete += resultItem1.no_delete;
            resultItem.no_update += resultItem1.no_update;
            resultItem.no_add += resultItem1.no_add;
            if(resultItem.smell_types.size() == 0 && resultItem1.smell_types.size() > 0)
                resultItem.smell_types.addAll(resultItem1.smell_types);
            else if (resultItem1.smell_types.size() > 0)
                for (int i=0; i<resultItem.smell_types.size();i++) {
                    resultItem.smell_types.set(i,String.valueOf( Integer.valueOf(resultItem.smell_types.get(i)) + Integer.valueOf(resultItem1.smell_types.get(i) )));
                }
        });
    }

    void goWithCommits(final String prevCommit, String currentCommit) {
        RevCommit prevRevCommit = findCommit(prevCommit);
        RevCommit currentRevCommit = findCommit(currentCommit);
        goWithCommits(prevRevCommit, currentRevCommit);
    }
}
