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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by viktor on 12/21/2017.
 */

public class StoevchenModule extends AbstractModule {
    private final String URL = "http://www.stoevchen.com/";

    public StoevchenModule(Context context) {
        super(context, "Stoevchen", LocalDate.now().plusDays(0), LocalDate.now().plusDays(0));
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
        result.put(url, params);
        return result;
    }

    @Override
    protected Set<Shout> parseShouts(String url, Map<String, Object> params) throws IOException, JSONException {
        Set<Shout> result = new HashSet<>();

        Document docSpeisekarte = Jsoup.connect("http://www.stoevchen.com/#karten-modal").get();

        Document doc = Jsoup.connect(url).get();
        Elements menuElements = doc.getElementsByAttributeValue("class", "media-body media-middle");
        Elements weekdaysElements = doc.getElementsByAttributeValue("class", "label label-weekday");
        int i = 0;
        for (Element menuElement : menuElements) {
            String shoutTitle = "Stövchen";
            String shoutMessage = weekdaysElements.eq(i).html();
            String messageString = menuElement.html();
            messageString = messageString.replace(" class=\"media-heading\"", "");
            messageString = messageString.replace("&amp;", " &");

            // viet, das check ich nicht, wo ist die speisekarte?
            String speisekarteString = docSpeisekarte.getElementsByAttributeValue("class", "col-md-12").first().html();
            shoutMessage += "<br>" + messageString + "<br>" + speisekarteString;


            String dateString = "????";
            String timeString = "11:30";
            DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
            LocalDate date = formatter.parseLocalDate(dateString);
            LocalTime startTime = DateTimeFormat.forPattern("HH:mm").parseLocalTime(timeString);
            LocalDateTime startDate = date.toLocalDateTime(startTime);

            String addressString = "Waldstraße 54, 76133 Karlsruhe";
            Address address = parseLocationFromAddress(addressString);

            Set<Util.ShoutCategory> categories = new HashSet<>();
            categories.add(Util.ShoutCategory.FOOD);
            List<Bitmap> images = new ArrayList<>();

            Set<String> hashtags = new HashSet<>();

            hashtags.add("stoevchen");
            hashtags.add("essen");
            hashtags.add("mittagsessen");
            hashtags.add("mittagskarte");

            Shout shout = new Shout(url, this, categories, shoutTitle, shoutMessage, hashtags, startDate, null, address, images);

            result.add(shout);
            i++;
        }
        return result;
    }

    @Override
    protected int getMaxConnections() {
        return 5;
    }
}
