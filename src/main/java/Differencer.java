import DiffResult.DiffResultAdded;
import DiffResult.DiffResultInterface;
import DiffResult.Result;
import DiffResult.ResultItem;
import Production.ProductionFile;
import com.github.gumtreediff.actions.model.Move;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
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
import testsmell.TestFile;
import testsmell.TestSmellDetector;




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
    private TreeWalk treeWalk;
    private ArrayList<TestFile> testFiles;
    private ArrayList<ProductionFile> productionFiles;
    private Result result;

    Differencer(Git _git) throws GitAPIException {
        git = _git;
        this.getRevisions();
        repository = git.getRepository();
        treeWalk = new TreeWalk(repository);
        testFiles = new ArrayList<>();
        productionFiles = new ArrayList<>();
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

            Diff astDiffs = new AstComparator().compare(newTmpFile, oldTmpFile);
//            Iterator<> it = astDiffs.getAllOperations().iterator();



            System.out.println("Changes are:");

            for (Operation op : astDiffs.getRootOperations()) {
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
                    resultItem.from = position;
                }

                result.addResultItem(resultItem);
                System.out.println("op " + op);

                //   System.out.println(op.getSrcNode().toString());
                //    System.out.println(op.getDstNode().toString());

            }

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
            Path path = Paths.get(filePath);
            long lineCount = 0;
            try {
                lineCount = Files.lines(path).count();
            } catch (IOException e) {
                e.printStackTrace();
            }

            int methods = (int) n.getMethods().stream().filter(m -> m.getAnnotationByName("Test").isPresent()).count();
            int ignored = (int) n.getMethods().stream().filter(m -> m.getAnnotationByName("Ignore").isPresent()).count();
            if ( methods > 1) {
                TestFile testFile = new TestFile(realFilePath, filePath, "");
                testFile.setNumOfIgnoredTests(ignored);
                testFile.setNumOfMethods(methods);
                testFile.setLines(lineCount);
                testFiles.add(testFile);
            } else {
                productionFiles.add(new ProductionFile(realFilePath, filePath));
            }

        }
    }


    private DiffResultAdded astDiffAdd(ObjectId newObj, String filePath) {

        DiffResultAdded diffResultAdded = new DiffResultAdded();
        diffResultAdded.setFilePath(filePath);


        // head
        try {
            File tmpFile = File.createTempFile("new", ".java");

            ObjectLoader newLoader = repository.open(newObj);
            newLoader.copyTo(new FileOutputStream(tmpFile));

            CompilationUnit compilationUnit = JavaParser.parse(tmpFile);

            ClassVisitor classVisitor = new ClassVisitor(tmpFile.getAbsolutePath(), filePath);
            classVisitor.visit(compilationUnit, null);

//            if (compilationUnit. (MethodDeclaration.class).stream()
//                    .filter(Util::isValidTestMethod).count() > 0) {
//                System.out.println("!!!!! Found Test method");
//            }


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
                    System.out.println("Entry: " + entry);

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

                        ObjectLoader newLoader = repository.open(entry.getNewId().toObjectId());
                        newLoader.copyTo(new FileOutputStream(tmpFile));

                        Path path = Paths.get(tmpFile.getAbsolutePath());
                        long lineCount = 0;
                        try {
                            lineCount = Files.lines(path).count();
                        } catch (Exception e) {

                        }

                        switch (changeType) {
                            case MODIFY:
                                System.out.println(" MODIFY");

                                File oldTmpFile = File.createTempFile("old", ".java");

                                ObjectLoader oldLoader = repository.open(entry.getOldId().toObjectId());
                                oldLoader.copyTo(new FileOutputStream(oldTmpFile));


                                Path oldPath = Paths.get(oldTmpFile.getAbsolutePath());
                                long oldLineCount = 0;
                                try {
                                    oldLineCount = Files.lines(oldPath).count();
                                } catch (Exception e) {

                                }
                                resultItem.loc = lineCount;
                                resultItem.changed_log = lineCount - oldLineCount;

                                filePatth = entry.getNewPath();
                                newObjectId = entry.getNewId().toObjectId();
                                oldObjectId = entry.getOldId().toObjectId();
                                astDiffModify(newObjectId, oldObjectId, filePatth);
                                break;
                            case ADD:
                                System.out.println(" ADD");
                                resultItem.what = entry.getNewPath();
                                resultItem.loc = lineCount;
                                filePatth = entry.getNewPath();
                                newObjectId = entry.getNewId().toObjectId();
                                diffResult.add(astDiffAdd(newObjectId, filePatth));
                                // Check if last added element to test file array is the current file
                                TestFile current = testFiles.size() > 0 ? testFiles.get(testFiles.size() - 1) : null;
                                if (current != null && current.getApp().equals(entry.getNewPath())) {
                                    testSmellDetector.detectSmells(current);
                                    resultItem.is_test_file = true;
                                    resultItem.smells = current.getNumOfSmells();
                                    resultItem.test_methods = current.getNumOfMethods();
                                    resultItem.test_ignored = current.getNumOfIgnoredTests();
                                }
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
                        this.result.addResultItem(resultItem);
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



//        testFiles.forEach(testFile -> {
//            try {
//                testSmellDetector.detectSmells(testFile);
//                System.out.println(testFile.toString());
//            } catch (IOException exception) {
//                throw new RuntimeException(exception.getMessage());
//            }
//        });

        System.out.println("Done!");

    }

    private RevCommit findCommit(String commit) {
        return revisions.stream().filter(r -> r.name().equals(commit)).findFirst().orElse(null);
    }


    public void go() {
        for (int i = 0; i < revisions.size() - 1; i++) {
            diffFiles(revisions.get(i), revisions.get(i + 1));
            RevCommit base = revisions.get(i);
            System.out.println("#######" + i + revisions.get(i).name() + " " + new Time(revisions.get(i).getCommitTime()) + " " + base.getShortMessage());

        }

    }

    public void goWithCommits(final String prevCommit, String currentCommit) {
        RevCommit prevRevCommit = findCommit(prevCommit);
        RevCommit currentRevCommit = findCommit(currentCommit);
        diffFiles(currentRevCommit, prevRevCommit);
    }
}
