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
 * Created by viktor on 12/21/2017.
 */

public class LIVE1Module extends AbstractModule {
    private final String URL = "https://www1.wdr.de/radio/1live/events/1live-sektor-events/index.html";

    public LIVE1Module(Context context) {
        super(context, "1Live", LocalDate.now().plusDays(0), LocalDate.now().plusDays(7));
    }

    @Override
    protected Map<String, Map<String, Object>> getParentURLs(String city) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        String childURL = URL;
        Map<String, Object> params = new HashMap<>();
        params.put("date", startDate);
        result.put(childURL, params);
        return result;
    }

    @Override
    protected Map<String, Map<String, Object>> parseChildURLs(String url, Map<String, Object> params) throws IOException {
        Map<String, Map<String, Object>> result = new HashMap<>();

        boolean hasNextPage = true;
        while (hasNextPage) {
            Document doc = Jsoup.connect(url).get();
            Elements events = doc.getElementsByAttributeValue("class", "teaser");
            for (Element event : events) {
                Element item = event.child(0);
                String link = "https://www1.wdr.de" + item.attr("href");
                result.put(link, params);
            }
            Element nextPage = doc.getElementsByAttributeValue("class", "next").first();//.child(0);
            if (nextPage != null)
                url = "https://www1.wdr.de" + nextPage.attr("href");
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
        String shoutTitle = doc.getElementsByAttributeValue("class","headline small").first().html();
        String shoutMessage = doc.getElementsByAttributeValue("class","einleitung small").first().html();

        Elements events = doc.getElementsByAttributeValue("class", "box modTable eventDetail");
        for (Element event: events){
            String dateString = event.getElementsByAttributeValue("headers","eventDate").first().text();
            dateString = dateString.substring(dateString.indexOf(", ") + 2, dateString.indexOf(", ") + 2 + "dd.MM.yyyy".length());

            String timeString = event.getElementsByAttributeValue("headers","eventStart").first().text();
            timeString = timeString.replace(" Uhr","");
            DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
            LocalDate date = formatter.parseLocalDate(dateString);
            LocalTime startTime = DateTimeFormat.forPattern("HH.mm").parseLocalTime(timeString);
            LocalDateTime startDate = date.toLocalDateTime(startTime);

            String addressString = event.getElementsByAttributeValue("headers","eventLocation").first().text();
            Address address = parseLocationFromAddress(addressString);

            Set<Util.ShoutCategory> categories = new HashSet<>();
            categories.add(Util.ShoutCategory.EVENTS);

            List<Bitmap> images = new ArrayList<>();
            Element galerieElement = doc.getElementsByAttributeValue("class", "media mediaA").first();
            String imgUrl = galerieElement.child(0).attr("src");//.getString("image").replace("\\", "");
            imgUrl = imgUrl.replace("\\", "");
            if(imgUrl.length() > 10) {
                images.add(downloadImage("https://www1.wdr.de" + imgUrl));
            }

            Set<String> hashtags = new HashSet<>();

            hashtags.add("1live");
            hashtags.add("events");
            hashtags.add("event");

            Shout shout = new Shout(url, this, categories, shoutTitle, shoutMessage, hashtags, startDate, null, address, images);

            result.add(shout);

        }

        return result;
    }

    @Override
    protected int getMaxConnections() {
        return 5;
    }
}
