package com.app.ballyhoo.crawler.modules;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Address;
import android.util.Pair;

import com.app.ballyhoo.crawler.main.Shout;
import com.app.ballyhoo.crawler.main.Util;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Viet on 22.11.2017.
 */

public class KarlsruheDEModule extends AbstractModule {
    public KarlsruheDEModule(Context context) {
        super(context, "karlsruheDE", LocalDate.now(), LocalDate.now());
    }

    @Override
    protected Map<String, Map<String, Object>> getParentURLs(String city) {
        assert city.equals("karlsruhe");

        Map<String, Map<String, Object>> result = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        String url = "https://kalender.karlsruhe.de/kalender/db/termine/orte.html";
        result.put(url, params);
        return result;
    }

    @Override
    protected Map<String, Map<String, Object>> parseChildURLs(String url, Map<String, Object> params) throws IOException {
        Map<String, Map<String, Object>> result = new HashMap<>();
        Document doc = Jsoup.connect(url).get();
        Elements listings = doc.getElementsByAttributeValue("class", "boxzweidrittel");

        Set<String> subUrls = new HashSet<>();
//        for (Element list: listings) {
//            Elements items = list.getElementsByAttributeValue("class", "link-intern");
//
//            for (Element event: items) {
//                subUrls.add(event.attr("href"));
//            }
//        }
        subUrls.add("https://kalender.karlsruhe.de/kalender/db/termine/6845.html");
        subUrls.add("https://kalender.karlsruhe.de/kalender/db/termine/1467.html");
        subUrls.add("https://kalender.karlsruhe.de/kalender/db/termine/226.html");
        subUrls.add("https://kalender.karlsruhe.de/kalender/db/termine/4925.html");

        for (String subUrl: subUrls) {
            Set<String> urls = parseSubChildURLs(subUrl);
            for (String subChildUrl: urls)
                result.put(subChildUrl, params);
        }

        return result;
    }

    private Set<String> parseSubChildURLs(String url) throws IOException {
        Set<String> result = new HashSet<>();
        Document doc = Jsoup.connect(url).get();
        Elements listings = doc.getElementsByAttributeValue("class", "boxzweidrittel");

        for (Element list: listings) {
            Elements items = list.getElementsByAttributeValue("class", "link-intern");

            for (Element event: items) {
                result.add(event.attr("href"));
            }
        }
        return result;
    }

    @Override
    protected Set<Shout> parseShouts(String url, Map<String, Object> params) throws IOException, JSONException {
        Document doc = Jsoup.connect(url).get();

        Element content = doc.getElementsByAttributeValue("class", "vevent").get(0);

        Set<Util.ShoutCategory> categories = new HashSet<>();
        categories.add(Util.ShoutCategory.EVENTS);
        String title = content.getElementsByAttributeValue("class", "shortDescription").get(0).html();
        String message = content.getElementsByAttributeValue("class", "description").get(0).html();
        DateTime startDate = DateTime.parse(content.getElementsByAttributeValue("class", "dtstart").get(0).attr("title"));
        DateTime endDate;
        String endDateStr = content.getElementsByAttributeValue("class", "dtend").get(0).attr("title");
        if (endDateStr == null || endDateStr.isEmpty()) {
            endDate = null;
        } else
            endDate = DateTime.parse(endDateStr);
        Element locElement = doc.getElementsByAttributeValue("id", "box_ort").get(0);
        String addressStr = locElement.getElementsByAttributeValue("class", "street-address").get(0).html()
                + " " + locElement.getElementsByAttributeValue("class", "postal-code").get(0).html()
                + " " + locElement.getElementsByAttributeValue("class", "locality").get(0).html();
        Address address = parseLocationFromAddress(addressStr);

        List<Bitmap> images = new ArrayList<>();
        String imgUrl = content.getElementsByAttributeValue("class", "terminbild").get(0).attr("src");
        images.add(downloadImage(imgUrl));

        Set<Shout> result = new HashSet<>();
        Set<String> hashtags = new HashSet<>();
        result.add(new Shout(url, this, categories, title, message, hashtags, startDate, endDate, address, images));
        return result;
    }
}
