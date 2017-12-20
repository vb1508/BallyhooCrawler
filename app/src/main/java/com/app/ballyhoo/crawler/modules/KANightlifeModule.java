package com.app.ballyhoo.crawler.modules;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Address;
import android.util.Pair;

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
 * Created by Viet on 25.11.2017.
 */

public class KANightlifeModule extends AbstractModule {
    private final String URL = "http://www.ka-nightlife.de/";

    public KANightlifeModule(Context context) {
        super(context, "KANightlife", LocalDate.now(), LocalDate.now().plusDays(7));
    }

    @Override
    protected Map<String, Map<String, Object>> getParentURLs(String city) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String url = URL + "events.php?day=" + date.getDayOfMonth() + "&month=" + date.getMonthOfYear()
                    + "&year=" + date.getYear() + "&sid=&x=" + date.getDayOfMonth() + 1;
            Map<String, Object> params = new HashMap<>();
            params.put("date", date);
            result.put(url, params);
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

        LocalDate date = (LocalDate) params.get("date");
        Document doc = Jsoup.connect(url).get();
        Elements listings = doc.select("table[border=0][cellpadding=0][cellspacing=0][width=442]");
        for (Element list: listings) {
            Elements header = list.select("td[height=80][valign=top]");
            String title = header.select("table[border=0][cellpadding=0][cellspacing=0]").get(0).getElementsByTag("font").get(0).html();
            Element timeAndLocationElement = header.select("table[border=0][cellpadding=3][cellspacing=0]").get(0).child(0);
            String startTimeStr = timeAndLocationElement.child(0).child(1).text().replace(" Uhr", "");
            LocalTime startTime = DateTimeFormat.forPattern("HH:mm").parseLocalTime(startTimeStr);
            LocalDateTime startDate = date.toLocalDateTime(startTime);
            LocalDateTime endDate = null;
            Set<Util.ShoutCategory> categories = new HashSet<>();
            if (startTime.getHourOfDay() < 15) {
                categories.add(Util.ShoutCategory.FOOD);
            } else
                categories.add(Util.ShoutCategory.NIGHTLIFE);

            String addressStr = timeAndLocationElement.child(1).child(1).ownText();
            Address address = parseLocationFromAddress(addressStr);
            String message = list.select("td[height=100%][valign=top]").get(0).getElementsByTag("font").get(0).html();

            String imgUrl = list.select("td[rowspan=3][valign=top]").get(0).getElementsByAttribute("src").attr("src");
            List<Bitmap> images = new ArrayList<>();
            Bitmap image = downloadImage(URL + imgUrl);
            if (image != null)
                images.add(image);
            Set<String> hashtags = new HashSet<>();
            hashtags.add("party");
            hashtags.add("club");
            hashtags.add("clubbing");
            Shout shout = new Shout(url, this, categories, title, message, hashtags, startDate, endDate, address, images);
            result.add(shout);
        }
        return result;
    }
}
