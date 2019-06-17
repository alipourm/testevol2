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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
                    else
                        resultItem.from = element.toString();

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

            try {
                lineCount = Files.lines(path).count();
            } catch (Exception e) {
                e.printStackTrace();
            }

            FileVisitResult result = (FileVisitResult) arg;
            int methods = (int) n.getMethods().stream().filter(m -> m.getAnnotationByName("Test").isPresent()).count();
            int ignored = (int) n.getMethods().stream().filter(m -> m.getAnnotationByName("Ignore").isPresent()).count();

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
                        try {
                            lineCount = Files.lines(path).count();
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
                                    continue;
                                }

                                assert resultOldFile != null;

                                resultItem.changed_methods = resultNewFile.methods - resultOldFile.methods;
                                resultItem.changed_statements = resultNewFile.statements - resultOldFile.statements;


                                Path oldPath = Paths.get(oldTmpFile.getAbsolutePath());
                                long oldLineCount = 0;
                                try {
                                    oldLineCount = Files.lines(oldPath).count();
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
                                System.out.println(" DELETE");

                                break;

                            case RENAME:
                                System.out.println(" RENAME");

                                break;

                            case COPY:
                                System.out.println("COPY");

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
        resultItem.newCommitAuthor = currentRevCommit.getAuthorIdent().getName();
        resultItem.commit_counts = findCommitIndex(prevRevCommit) - findCommitIndex(currentRevCommit);
        result.addResultItem(resultItem);

        diffFiles(currentRevCommit, prevRevCommit);
    }

    void goWithCommits(final String prevCommit, String currentCommit) {
        RevCommit prevRevCommit = findCommit(prevCommit);
        RevCommit currentRevCommit = findCommit(currentCommit);
        goWithCommits(prevRevCommit, currentRevCommit);
    }
}
