package testsmell;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class TestFile {
    private String app, testFilePath, productionFilePath;
    private List<AbstractSmell> testSmells;
    private int numOfMethods;
    private int numOfIgnoredTests;
    private long lines;

    public long getLines() {
        return lines;
    }

    public void setLines(long lines) {
        this.lines = lines;
    }


    public int getNumOfIgnoredTests() {
        return numOfIgnoredTests;
    }

    public void setNumOfIgnoredTests(int numOfIgnoredTests) {
        this.numOfIgnoredTests = numOfIgnoredTests;
    }


    public int getNumOfMethods() {
        return numOfMethods;
    }

    public void setNumOfMethods(int numOfMethods) {
        this.numOfMethods = numOfMethods;
    }


    public String getApp() {
        return app;
    }

    public String getProductionFilePath() {
        return productionFilePath;
    }

    public String getTestFilePath() {
        return testFilePath;
    }

    public List<AbstractSmell> getTestSmells() {
        return testSmells;
    }

    public boolean getHasProductionFile() {
        return ((productionFilePath != null && !productionFilePath.isEmpty()));
    }

    public TestFile(String app, String testFilePath, String productionFilePath) {
        this.app = app;
        this.testFilePath = testFilePath;
        this.productionFilePath = productionFilePath;
        this.testSmells = new ArrayList<>();
    }

    public void addSmell(AbstractSmell smell) {
        testSmells.add(smell);
    }

    public String getTagName(){
        return testFilePath.split("\\\\")[4];
    }

    public String getTestFileName(){
        int lastIndex = testFilePath.lastIndexOf("\\");
        return testFilePath.substring(lastIndex+1,testFilePath.length());
    }

    public String getTestFileNameWithoutExtension(){
        int lastIndex = getTestFileName().lastIndexOf(".");
        return getTestFileName().substring(0,lastIndex);
    }

    public String getProductionFileNameWithoutExtension(){
        int lastIndex = getProductionFileName().lastIndexOf(".");
        if(lastIndex==-1)
            return "";
        return getProductionFileName().substring(0,lastIndex);
    }

    public String getProductionFileName(){
        int lastIndex = productionFilePath.lastIndexOf("\\");
        if(lastIndex==-1)
            return "";
        return productionFilePath.substring(lastIndex+1,productionFilePath.length());
    }

    @Override
    public String toString() {
        long numOfSmells = this.getTestSmells().stream().filter(Objects::nonNull).filter(AbstractSmell::getHasSmell).mapToLong(s -> s.getSmellyElements().stream().filter(SmellyElement::getHasSmell).count()).sum();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Result of " + this.app + "\n")
                .append("Methods:" + this.numOfMethods + ", Ignored:" + this.numOfIgnoredTests + ", lines:" + this.lines + "\n")
                .append("Smeels:" + numOfSmells);

        return stringBuilder.toString();
    }

    public String getRelativeTestFilePath() {
        String[] splitString = testFilePath.split("\\\\");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            stringBuilder.append(splitString[i] + "\\");
        }
        return testFilePath.substring(stringBuilder.toString().length()).replace("\\", "/");
    }

    public String getRelativeProductionFilePath() {
        if (!StringUtils.isEmpty(productionFilePath)) {
            String[] splitString = productionFilePath.split("\\\\");
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                stringBuilder.append(splitString[i] + "\\");
            }
            return productionFilePath.substring(stringBuilder.toString().length()).replace("\\", "/");
        } else {
            return "";

        }
    }
}