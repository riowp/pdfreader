package com.pdfreader.app;

public class RecentFile {
    private String uri;
    private String fileName;
    private long lastOpened;

    public RecentFile(String uri, String fileName, long lastOpened) {
        this.uri = uri;
        this.fileName = fileName;
        this.lastOpened = lastOpened;
    }

    public String getUri() { return uri; }
    public String getFileName() { return fileName; }
    public long getLastOpened() { return lastOpened; }

    public void setUri(String uri) { this.uri = uri; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setLastOpened(long lastOpened) { this.lastOpened = lastOpened; }
}
