package com.app.ballyhoo.crawler.modules;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Address;

import com.app.ballyhoo.crawler.main.Shout;
import com.app.ballyhoo.crawler.main.Util;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by viktor on 12/16/2017.
 */

public class FussballDEModule extends AbstractModule {
    private final String URL = "http://www.fussball.de/spieltagsuebersicht/3liga-deutschland-3-liga-herren-saison1718-deutschland/-/staffel/01VM7AUE1K000000VS54898EVSV90M3P-G#!/section/mediastream";

    public FussballDEModule(Context context) {
        super(context, "FussballDE", LocalDate.now(), LocalDate.now().plusDays(7));
    }

    @Override
    protected Map<String, Map<String, Object>> getParentURLs(String city) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        params.put("date", startDate);
        result.put(URL, params);
        return result;
    }

    @Override
    protected Map<String, Map<String, Object>> parseChildURLs(String url, Map<String, Object> params) throws IOException {
        Map<String, Map<String, Object>> result = new HashMap<>();

        Document doc = Jsoup.connect(url).get();
        Element listings = doc.getElementsByAttributeValue("class", "fixtures-matches-table").first();
        Elements games = listings.getElementsByAttributeValue("class", "column-detail");

        for (Element game: games) {
            Element item = game.child(0);
            Element item_href = game.getElementsByAttribute("href").first();
            String b = item_href.attr("href");
            String a = item.attr("href");
            result.put(item.attr("href"), params);
        }
        return result;
    }

    @Override
    protected Set<Shout> parseShouts(String url, Map<String, Object> params) throws IOException, JSONException {
        Set<Shout> result = new HashSet<>();

        Document doc = Jsoup.connect(url).get();
        Element location = doc.getElementsByAttributeValue("class", "location").first();

        String addressString = location.ownText();
        Address address = parseLocationFromAddress(addressString);

        //DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
        LocalDate date = startDate;//formatter.parseLocalDate(dateString);
        LocalDateTime startTime = date.toLocalDateTime(new LocalTime(11, 0));
        Elements data = doc.getElementsByAttributeValue( "type","text/javascript");
        //Elements gast0 = doc.select("script type=\"text/javascript\":contains(edGastmannschaftName)");
        Element datasheet = data.get(9); // das kann ein problem sein
        //Elements gast0 = data.select("edGastmannschaftName");
        //Elements gast3 = gast.getElementsByClass("edGastmannschaftName");
        //Elements gast4 = gast.getElementsByTag("edGastmannschaftName");
        String datasheet_string = datasheet.html();
        //Element gast2 = gast.getElementsByAttribute("edGastmannschaftName").first();
        String gastmannschaft = datasheet_string.substring(datasheet_string.lastIndexOf("edGastmannschaftName='") + "edGastmannschaftName='".length(),datasheet_string.indexOf("';\n" + "edGebiet="));
        String heimmannschaft = datasheet_string.substring(datasheet_string.lastIndexOf("edHeimmannschaftName='")+ "edHeimmannschaftName='".length(),datasheet_string.indexOf("';\n" + "edMandant='"));
        String liga = datasheet_string.substring(datasheet_string.lastIndexOf("edSpielklasseName='")+"edSpielklasseName='".length(),datasheet_string.indexOf("';\n" + "edSpielklasseTypId='"));

        String shoutTitle = liga + " " + heimmannschaft + " gegen " + gastmannschaft;
        String shoutMessage = heimmannschaft + " gegen " + gastmannschaft;
        Set<Util.ShoutCategory> categories = new HashSet<>();
        categories.add(Util.ShoutCategory.EVENTS);

        List<Bitmap> images = new ArrayList<>();

        Shout shout = new Shout(url, this, categories, shoutTitle, shoutMessage, startTime, null, address, images);
        result.add(shout);

        return result;
    }
}
