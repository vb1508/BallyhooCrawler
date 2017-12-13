package com.app.ballyhoo.crawler.dbconnector;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

/**
 * Created by Viet on 05.12.2017.
 */

class RefManager {
    static final String CRAWLER_DBREF = "crawled_db";
    static final String SHOUTS_DBREF = "shouts_db";
    static final String IMAGES_DBREF = "images_db";

    static String getDateRef(LocalDate d) {
        String result = null;
        if (d != null) {
            int year = d.getYear();
            int month = d.getMonthOfYear();
            int day = d.getDayOfMonth();
            result = year + "-" + month + "-" + day;
        }
        return result;
    }
}
