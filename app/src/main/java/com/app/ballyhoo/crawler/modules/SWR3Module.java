
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

public class SWR3Module  extends AbstractModule {
    private final String URL = "https://www.swr3.de/events/events/-/id=63960/did=1368546/nid=63960/cf=42/zt44i3/index.html?page_1368548=aHR0cDovL3dyYXBzLnN3cjMuZGUvcmVzL2V2ZW50cy92ZXJhbnN0YWx0dW5nZW4v&datum=";

    public SWR3Module(Context context) {
        super(context, "SWR3", LocalDate.now().plusDays(0), LocalDate.now().plusDays(7));
    }

    @Override
    protected Map<String, Map<String, Object>> getParentURLs(String city) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            String childURL = URL + d.getYear()+"-"+ d.getMonthOfYear() +"-"+ d.getDayOfMonth();
            Map<String, Object> params = new HashMap<>();
            params.put("date", d);
            result.put(childURL, params);
        }
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

        Document doc = Jsoup.connect(url).get();
        Element panel_today = doc.getElementsByAttributeValue("class", "panel panel-primary").first();
        //Element item = event.child(0);
        Elements events = panel_today.getElementsByAttributeValue("class", "panel panel-default");
        for (Element event: events){
            String shoutTitle = event.getElementsByAttributeValue("class","list-group-item list-group-item-info").first().html();
            String datetimeString = event.getElementsByAttributeValue("class","list-group-item").eq(0).html();
            String timeString = datetimeString.substring(datetimeString.indexOf(" | ") + " | ".length(),datetimeString.indexOf(" Uhr"));
            String dateString = datetimeString.substring(datetimeString.indexOf(" | ") - "dd.MM.yyyy".length(),datetimeString.indexOf(" | "));
            DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy");
            LocalDate date = formatter.parseLocalDate(dateString);
            LocalTime startTime = DateTimeFormat.forPattern("HH:mm").parseLocalTime(timeString);
            LocalDateTime startDate = date.toLocalDateTime(startTime);

            String addressString = event.getElementsByAttributeValue("class","list-group-item").eq(2).html()+ ", " + event.getElementsByAttributeValue("class","list-group-item").eq(1).html();
            Address address = parseLocationFromAddress(addressString);

            String shoutMessage = event.getElementsByAttributeValue("class","list-group-item").eq(3).html();

            Set<Util.ShoutCategory> categories = new HashSet<>();
            categories.add(Util.ShoutCategory.EVENTS);
            List<Bitmap> images = new ArrayList<>();

            Set<String> hashtags = new HashSet<>();

            hashtags.add("swr3");
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
