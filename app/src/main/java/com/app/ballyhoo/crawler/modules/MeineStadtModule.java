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
        super(context, "MeineStadt", LocalDate.now(), LocalDate.now().plusDays(0));
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
        while (true) {
            Document doc = Jsoup.connect(url).get();
            Elements events = doc.getElementsByAttributeValue("class", "ms-result-item ms-result-item-event ms-link-area-basin");
            for (Element event : events) {
                //Element item = event.child(0);
                Element item = event.getElementsByAttributeValue("class","ms-result-item-headline").first();
                Element item1 = item.child(0);
                String link = item1.attr("href");
                result.put(link, params);
            }
            Element nextPage = doc.getElementsByAttributeValue("rel", "next").first();
            if (nextPage == null) break;
            url = "http://veranstaltungen.meinestadt.de" + nextPage.attr("href");
        }
        return result;
    }

    @Override
    protected Set<Shout> parseShouts(String url, Map<String, Object> params) throws IOException, JSONException {
        Set<Shout> result = new HashSet<>();

        Document doc = Jsoup.connect(url).get();
        Element maincontent = doc.getElementsByAttributeValue("class", "ms-mpd-maincontent").first();
        Element maincontent2 = maincontent.child(0);

        Element element_title = maincontent2.getElementsByAttributeValue("class", "ms-headline ms-headline--h1").first();
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

        String timeDateString = dateString + "." + timeString;

        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
        LocalDate date = formatter.parseLocalDate(dateString);

        LocalDateTime startTime = date.toLocalDateTime(new LocalTime(11, 0)); // das muss veraendert werden, so dass nicht alles um 11 anfaengt.

        String shoutMessage = "";
        Set<Util.ShoutCategory> categories = new HashSet<>();
        categories.add(Util.ShoutCategory.OTHER);

        List<Bitmap> images = new ArrayList<>();

        //for (Element adenauerLinie: adenauerTag.child(0).children()) {
        //    String linienName = adenauerLinie.getElementsByAttributeValue("class", "mensatype").first().child(0).text();
        //    String linienBeschreibung = adenauerLinie.getElementsByAttributeValue("class", "mensadata").text();
        //    shoutMessage += linienName + " " + linienBeschreibung;
        //}

        Shout shout = new Shout(url, this, categories, shoutTitle, shoutMessage, startTime, null, address, images);
        result.add(shout);


        return result;
    }
}
