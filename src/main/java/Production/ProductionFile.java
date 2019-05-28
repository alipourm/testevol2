package Production;

public class ProductionFile {
    private String app, filePath;
    private long lines;

    public long getLines() {
        return lines;
    }

    public void setLines(long lines) {
        this.lines = lines;
    }

    public ProductionFile(String app, String filePath) {
        this.app = app;
        this.filePath = filePath;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }


}
