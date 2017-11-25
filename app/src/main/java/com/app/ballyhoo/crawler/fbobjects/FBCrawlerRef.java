package com.app.ballyhoo.crawler.fbobjects;

/**
 * Created by Viet on 25.11.2017.
 */

public class FBCrawlerRef {
    private int id;
    private String url;

    public FBCrawlerRef(int id, String url) {
        this.id = id;
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public int getId() {
        return id;
    }
}
