package com.app.ballyhoo.crawler.modules;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Address;

import com.app.ballyhoo.crawler.main.Shout;
import com.app.ballyhoo.crawler.main.Util;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Viet on 18.11.2017.
 */

public class VirtualNightsModule extends AbstractModule {
    private final String URL = "http://www.virtualnights.com";

    public VirtualNightsModule(Context context) {
        super(context, "virtualnights");
    }

    @Override
    protected String getURL(String city, LocalDate date) {
        return URL + "/" + city + "/events/" + date.getYear() + "-"
                + date.getMonthOfYear() + "-" + date.getDayOfMonth();
    }

    @Override
    protected Set<String> parseChildURLs(String url) throws IOException {
        Set<String> result = new HashSet<>();

        Document doc = Jsoup.connect(url).get();
        Elements listings = doc.getElementsByAttributeValue("id", "eventlisting");

        for (Element list: listings) {
            Elements items = list.getElementsByAttributeValue("itemprop", "url");

            for (Element event: items) {
                result.add(URL + event.attr("href"));
            }
        }
        return result;
    }

    @Override
    protected Set<Shout> parseShouts(String url) throws IOException, JSONException {
        Document doc = Jsoup.connect(url).get();
        Elements header = doc.getElementsByAttributeValue("type", "application/ld+json");

        assert header.size() == 1;
        //
        JSONObject json = new JSONObject(header.html());
        DateTime startDate = DateTime.parse(json.getString("startDate"));
        DateTime endDate = DateTime.parse(json.getString("endDate"));
        String title = json.getString("name");
        String imgUrl = json.getString("image").replace("\\", "");
        List<Bitmap> images = new ArrayList<>();
        images.add(downloadImage(imgUrl));
        Set<Util.ShoutCategory> categories = new HashSet<>();
        categories.add(Util.ShoutCategory.NIGHTLIFE);
        Address address = parseLocationFromAddress(json.getJSONObject("location").getString("address"));
        String message = doc.getElementsByAttributeValue("class", "event-description").html();

        Set<Shout> result = new HashSet<>();
        result.add(new Shout(url, VirtualNightsModule.this, categories, title, message, startDate, endDate, address, images));
        return result;
    }
}
