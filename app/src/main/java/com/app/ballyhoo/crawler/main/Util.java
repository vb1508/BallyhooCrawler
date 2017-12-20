package com.app.ballyhoo.crawler.main;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

/**
 * Created by Viet on 19.11.2017.
 */

public class Util {
    public enum ShoutCategory {NIGHTLIFE, FOOD, EVENTS, SOCIAL, SHOPPING, SMALLTALK}
    public enum ShoutStatus {ACTIVE, DEACTIVATED}
    public enum ShoutType {PERMANENT, LOCAL, AD}

    public static String clean(String html) {
        Document jsoupDoc = Jsoup.parse(html);
        //set pretty print to false, so \n is not removed
        jsoupDoc.outputSettings(new Document.OutputSettings().prettyPrint(false));

        //select all <br> tags and append \n after that
        jsoupDoc.select("br").after("\\n");

        //select all <p> tags and prepend \n before that
        jsoupDoc.select("p").before("\\n");

        //get the HTML from the document, and retaining original new lines
        String str = jsoupDoc.html().replaceAll("\\\\n", "\n");
        return Jsoup.clean(str, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false)).replace("&gt;", ">").replace("&amp;", "&").replace("&nbsp;", " ");
    }
}
