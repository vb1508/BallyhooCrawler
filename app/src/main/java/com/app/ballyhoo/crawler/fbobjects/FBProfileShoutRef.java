package com.app.ballyhoo.crawler.fbobjects;

import com.app.ballyhoo.crawler.main.Util;

/**
 * Created by Viet on 13.08.2017.
 */

public class FBProfileShoutRef {
    private Util.ShoutType type;
    private String dateRef;
    private String locRef;

    public FBProfileShoutRef(Util.ShoutType type, String dateRef, String locRef) {
        this.type = type;
        this.dateRef = dateRef;
        this.locRef = locRef;
    }

    public Util.ShoutType getType() {
        return type;
    }

    public String getDateRef() {
        return dateRef;
    }

    public String getLocRef() {
        return locRef;
    }
}
