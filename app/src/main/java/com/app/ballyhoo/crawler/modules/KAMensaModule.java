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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Viet on 04.12.2017.
 */

public class KAMensaModule extends AbstractModule {
    private final String URL = "http://www.sw-ka.de/de/essen/";

    public KAMensaModule(Context context) {
        super(context, "KAMensa", LocalDate.now(), LocalDate.now().plusWeeks(1));
    }

    @Override
    protected Map<String, Map<String, Object>> getParentURLs(String city) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusWeeks(1)) {
            String childURL = URL + "?kw=" + d.getWeekOfWeekyear();
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
        Element listings = doc.getElementsByAttributeValue("id", "c1").first();
        Element tabsNav = listings.getElementsByAttributeValue("class", "ui-tabs-nav").first();

        for (Element adenauer: tabsNav.children()) {
            String href = adenauer.child(0).attr("href").replace("#","");
            String dateString = adenauer.child(0).attr("rel");
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
            LocalDate date = formatter.parseLocalDate(dateString);
            LocalDateTime startTime = date.toLocalDateTime(new LocalTime(11, 0));

            Element adenauerTag = listings.getElementsByAttributeValue("id", href).first();

            String shoutTitle = "Mensa am Adenauerring";
            String shoutMessage = "";
            Set<Util.ShoutCategory> categories = new HashSet<>();
            categories.add(Util.ShoutCategory.FOOD);

            String addressString = "Adenauerring 7, 76131 Karlsruhe";
            Address address = parseLocationFromAddress(addressString);
            List<Bitmap> images = new ArrayList<>();

            for (Element adenauerLinie: adenauerTag.child(0).children()) {
                String linienName = adenauerLinie.getElementsByAttributeValue("class", "mensatype").first().child(0).text();
                String linienBeschreibung = adenauerLinie.getElementsByAttributeValue("class", "mensadata").text();
                shoutMessage += linienName + " " + linienBeschreibung;
            }

            Set<String> hashtags = new HashSet<>();
            Shout shout = new Shout(url, this, categories, shoutTitle, shoutMessage, hashtags, startTime, null, address, images);
            result.add(shout);
        }

        return result;
    }
}
