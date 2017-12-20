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

public class BadischesBrauhausModule extends AbstractModule {
    private final String URL = "https://www.badisch-brauhaus.de/unser-essen/unser-mittagstisch/";

    public BadischesBrauhausModule(Context context) {
        super(context, "BadischesBrauhaus", LocalDate.now().plusDays(0), LocalDate.now().plusDays(0));
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

        Document docSpeisekarte = Jsoup.connect("https://www.badisch-brauhaus.de/unser-essen/die-speisekarte/").get();

        Document doc = Jsoup.connect(url).get();
        Element menuElement = doc.getElementsByAttributeValue("class", "_1mf _1mj").first();
        String menuString = menuElement.html();

        String[] textSplitResult = menuString.split("</p>");
        String[] textSplitResult2 = Arrays.copyOf(textSplitResult, textSplitResult.length-1);
        for (String t : textSplitResult2) {
            String shoutTitle = "Badisches Brauhaus";
            String dateString = t.substring(t.indexOf("</strong>") - "dd.MM.".length(), t.indexOf("</strong>")) + startDate.getYear();
            String timeString = "11:30";
            DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
            LocalDate date = formatter.parseLocalDate(dateString);
            LocalTime startTime = DateTimeFormat.forPattern("HH:mm").parseLocalTime(timeString);
            LocalDateTime startDate = date.toLocalDateTime(startTime);

            String addressString = "Stephanienstraße 38-40, 76133 Karlsruhe";
            Address address = parseLocationFromAddress(addressString);
            String shoutMessage = t;//.substring(t.indexOf("<br>") + "<br>".length());

            Element speisekarteElement = docSpeisekarte.getElementsByAttributeValue("class", "entry clearfix post").first();
            String speisekarteString = speisekarteElement.html();
            shoutMessage += "<br>" + "<hr>" + speisekarteString;

            Set<Util.ShoutCategory> categories = new HashSet<>();
            categories.add(Util.ShoutCategory.FOOD);
            List<Bitmap> images = new ArrayList<>();

            Set<String> hashtags = new HashSet<>();

            hashtags.add("badischesbrauhaus");
            hashtags.add("essen");
            hashtags.add("mittagsessen");
            hashtags.add("mittagskarte");

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
