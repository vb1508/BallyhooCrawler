package com.app.ballyhoo.crawler.modules;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Address;

import com.app.ballyhoo.crawler.main.Shout;
import com.app.ballyhoo.crawler.main.Util;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Viet on 24.11.2017.
 */

public class KACityModule extends AbstractModule {
    private final String URL = "http://www.ka-city.de/";

    public KACityModule(Context context) {
        super(context, "KAcity");
    }

    @Override
    protected String getURL(String city, LocalDate date) {
        return URL + "dinner-lounge/mittagstisch/mittagstisch-uebersicht/cal/"
                + date.getYear() + "/" + date.getMonthOfYear() + "/" + date.getDayOfMonth();
    }

    @Override
    protected Set<String> parseChildURLs(String url) throws IOException {
        Set<String> result = new HashSet<>();
        Document doc = Jsoup.connect(url).get();

        String[] temp = url.split("/");
        String suffix = "cal/" + temp[temp.length-3] + "/" + temp[temp.length-2]
                + "/" + temp[temp.length-1];

        Elements listings = doc.getElementsByAttributeValue("class", "default_catheader_allday");

        for (Element element: listings) {
            Element href = element.getElementsByAttribute("href").get(0);
            result.add(URL + href.attr("href") + suffix);
        }
        return result;
    }

    @Override
    protected Set<Shout> parseShouts(String url) throws IOException, JSONException {
        Document doc = Jsoup.connect(url).get();
        Set<Util.ShoutCategory> categories = new HashSet<>();
        categories.add(Util.ShoutCategory.FOOD);

        Element header = doc.getElementsByAttributeValue("class", "grid_16 alpha omega").get(0);

        String title = header.getElementsByAttributeValue("class", "bgr-1 spacing-bot").get(0).html();
        Element addressElement = header.getElementsByAttributeValue("class", "wtdirectory_all wtdirectory_all_list").get(0);
        String addressStr = addressElement.getElementsByAttributeValue("class", "wtdirectory_all wtdirectory_all_even wtdirectory_all_address").get(1).html()
                + " " + addressElement.getElementsByAttributeValue("class", "wtdirectory_all wtdirectory_all_even wtdirectory_all_city").get(1).html();
        Address address = parseLocationFromAddress(addressStr);

        Element content = header.getElementsByAttributeValue("class", "dayview").get(0);

        String message = content.getElementsByAttributeValue("class", "vkal_description").get(0).getElementsByAttributeValue("class", "bodytext").get(0).html();
        String startDateStr = content.getElementsByAttributeValue("class", "title").get(0).getElementsByTag("h1").html();
        DateTimeFormatter dtf = DateTimeFormat.forPattern("EEEE, dd.MMMM y");
        LocalDateTime startDate = dtf.parseLocalDateTime(startDateStr);
        String imgUrl = header.getElementsByAttributeValue("class", "csc-textpic-image csc-textpic-last").get(0).getElementsByAttribute("src").attr("src");
        Bitmap image = downloadImage(URL + imgUrl);
        List<Bitmap> images = new ArrayList<>();
        images.add(image);

        Set<Shout> result = new HashSet<>();
        result.add(new Shout(url, this, categories, title, message, startDate, null, address, images));
        return result;
    }
}
