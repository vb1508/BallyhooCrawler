package com.app.ballyhoo.crawler.modules;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Address;

import com.app.ballyhoo.crawler.main.Shout;
import com.app.ballyhoo.crawler.main.Util;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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
 * Created by viktor on 12/17/2017.
 */

public class MeineStadtModule extends AbstractModule {
    private final String URL = "http://veranstaltungen.meinestadt.de/deutschland/alle/alle/kalender/";

    public MeineStadtModule(Context context) {
        super(context, "MeineStadt", LocalDate.now().plusDays(1), LocalDate.now().plusDays(10));
    }

    @Override
    protected Map<String, Map<String, Object>> getParentURLs(String city) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            String childURL = URL + "d-" + d.getDayOfMonth() + "-" + d.getMonthOfYear() + "t-" + d.getDayOfMonth() + "-" + d.getMonthOfYear();
            Map<String, Object> params = new HashMap<>();
            params.put("date", d);
            result.put(childURL, params);
        }
        return result;
    }

    @Override
    protected Map<String, Map<String, Object>> parseChildURLs(String url, Map<String, Object> params) throws IOException {
        Map<String, Map<String, Object>> result = new HashMap<>();

        boolean hasNextPage = true;
        while (hasNextPage) {
            Document doc = Jsoup.connect(url).get();
            Elements events = doc.getElementsByAttributeValue("class", "ms-result-item ms-result-item-event ms-link-area-basin");
            for (Element event : events) {
                //Element item = event.child(0);
                Element item = event.getElementsByAttributeValue("class", "ms-result-item-headline").first();
                Element item1 = item.child(0);
                String link = item1.attr("href");
                result.put(link, params);
            }
            Element nextPage = doc.getElementsByAttributeValue("rel", "next").first();
            if (nextPage != null)
                url = "http://veranstaltungen.meinestadt.de" + nextPage.attr("href");
            else
                hasNextPage = false;
            //hasNextPage = false; // nur zum debuggen
        }
        return result;
    }

    @Override
    protected Set<Shout> parseShouts(String url, Map<String, Object> params) throws IOException, JSONException {
        Set<Shout> result = new HashSet<>();

        Document doc = Jsoup.connect(url).get();
        Element maincontent = doc.getElementsByAttributeValue("class", "ms-mpd-maincontent").first();
        Element maincontent_child = maincontent.child(0);

        Element element_title = maincontent_child.getElementsByAttributeValue("class", "ms-headline ms-headline--h1").first();
        String shoutTitle = element_title.text();

        Element basicmodule = maincontent.getElementsByAttributeValue("class", "ms-mpd-basicmodule-list").first();

        Element element_street = basicmodule.getElementsByAttributeValue("name", "street").first();
        String street = element_street.attr("value");

        Elements element_city = basicmodule.getElementsByAttributeValue("name", "city");
        String city = element_city.first().attr("value");

        Elements element_zip = basicmodule.getElementsByAttributeValue("name", "zip");
        String zip = element_zip.first().attr("value");

        String addressString = street + ", " + zip + " " + city;
        Address address = parseLocationFromAddress(addressString);

        Elements element_date = basicmodule.getElementsByAttributeValue("name", "date");
        String dateString = element_date.first().attr("value");

        Elements element_time = basicmodule.getElementsByAttributeValue("name", "time");
        String timeString = element_time.first().attr("value");

        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
        LocalDate date = formatter.parseLocalDate(dateString);

        LocalTime startTime = DateTimeFormat.forPattern("HH:mm").parseLocalTime(timeString);
        LocalDateTime startDate = date.toLocalDateTime(startTime);

        String shoutMessage = "";
        Set<Util.ShoutCategory> categories = new HashSet<>();

        Element categoryContent = doc.getElementsByAttributeValue("class", "ms-infobox").first();
        Element categoryContent_child1 = categoryContent.child(1);
        String categoryString = categoryContent_child1.html();

        boolean addShout = false;
        Set<String> hashtags = new HashSet<>();

        if (categoryString.toLowerCase().contains("Freizeit".toLowerCase()) || categoryString.toLowerCase().contains("Sport".toLowerCase())) {
            categories.add(Util.ShoutCategory.SOCIAL);
            hashtags.add("freizeit");
            addShout = true;
        } else if (categoryString.toLowerCase().contains("Konzerte".toLowerCase()) || categoryString.toLowerCase().contains("Musicals".toLowerCase()) || categoryString.toLowerCase().contains("Theater".toLowerCase()) || categoryString.toLowerCase().contains("Festivals".toLowerCase()) || categoryString.toLowerCase().contains("Messen".toLowerCase()) || categoryString.toLowerCase().contains("Volksfeste".toLowerCase())) {
            categories.add(Util.ShoutCategory.EVENTS);
            hashtags.add("events");
            addShout = true;
        } else if (categoryString.toLowerCase().contains("Partys".toLowerCase())) {
            categories.add(Util.ShoutCategory.NIGHTLIFE);
            hashtags.add("nightlife");
            addShout = true;
        }

        List<Bitmap> images = new ArrayList<>();
        Elements galerieElements = maincontent.getElementsByAttributeValue("class", "ms-galerie-slider-thumb");
        for (Element galerieElement: galerieElements) {
            String imgUrl = galerieElement.attr("src");//.getString("image").replace("\\", "");
            imgUrl = imgUrl.replace("\\", "");
            if(imgUrl.length() > 10) {
                images.add(downloadImage(imgUrl));
            }

        }
        if (maincontent.getElementsByAttributeValue("class", "ms-field-value").first() != null) {
            if( addShout) {
                String linienName = maincontent.getElementsByAttributeValue("class", "ms-field-value").first().html();
                shoutMessage += linienName;
            }
        }else{
            addShout = false;
        }


        Shout shout = new Shout(url, this, categories, shoutTitle, shoutMessage, hashtags, startDate, null, address, images);

        if (addShout) { // nur shouts mit beschreibung sollen erstellt werden
            result.add(shout);
        }


        return result;
    }

    @Override
    protected int getMaxConnections() {
        return 5;
    }
}
