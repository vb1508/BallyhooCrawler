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
 * Created by viktor on 12/24/2017.
 */

public class BadnerhalleDEModule extends AbstractModule {
    private final String URL = "https://badnerlandhalle.de/content/veranstaltungen/";

    public BadnerhalleDEModule(Context context) {
        super(context, "BadnerhalleDE", LocalDate.now().plusDays(0), LocalDate.now().plusDays(0));
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

        Document doc = Jsoup.connect(url).get();
        Elements eventElements = doc.getElementsByAttributeValue("class", "eventon_list_event evo_eventtop  event");
        for( Element eventElement: eventElements){
            Element elementPart = eventElement.getElementsByAttributeValue("class", "evo_event_schema").first();
            String shoutTitle = elementPart.getElementsByAttributeValue("itemprop", "name").first().html();
            String datasheet_string = eventElement.getElementsByAttributeValue("type", "application/ld+json").first().html();
            String dateTimeString = datasheet_string.substring(datasheet_string.lastIndexOf("\"startDate\": \"") + "\"startDate\": \"".length(),datasheet_string.indexOf("\",\n" +
                    "\t\t\t\t\t\t\t  \t\"endDate\": \""));
            String dateString = dateTimeString.substring(0, dateTimeString.indexOf("T"));
            String timeString = dateTimeString.substring(dateTimeString.indexOf("T") + 4 ,dateTimeString.indexOf("T") + 9 );
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
            LocalDate date = formatter.parseLocalDate(dateString);
            LocalTime time = DateTimeFormat.forPattern("HH-mm").parseLocalTime(timeString);
            LocalDateTime startDate = date.toLocalDateTime(time);

            dateTimeString = datasheet_string.substring(datasheet_string.lastIndexOf("\"endDate\": \"") + "\"endDate\": \"".length(),datasheet_string.indexOf("\",\n" +
                    "\t\t\t\t\t\t\t  \t\"image\""));
            dateString = dateTimeString.substring(0, dateTimeString.indexOf("T"));
            timeString = dateTimeString.substring(dateTimeString.indexOf("T") + 4 ,dateTimeString.indexOf("T") + 9 );
            formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
            date = formatter.parseLocalDate(dateString);
            time = DateTimeFormat.forPattern("HH-mm").parseLocalTime(timeString);
            LocalDateTime endDate = date.toLocalDateTime(time);

            String shoutMessage = eventElement.getElementsByAttributeValue("class", "event_excerpt").first().html();
            shoutMessage = shoutMessage.replace(" class=\"padb5 evo_h3\"","");

            Set<Util.ShoutCategory> categories = new HashSet<>();
            categories.add(Util.ShoutCategory.EVENTS);
            List<Bitmap> images = new ArrayList<>();

            Set<String> hashtags = new HashSet<>();

            hashtags.add("badnerlandhalle");
            hashtags.add("events");

            String addressString = "Badnerlandhalle Karlsruhe Neureut Rubensstr. 21 , 76149 Karlsruhe";
            Address address = parseLocationFromAddress(addressString);

            Shout shout = new Shout(url, this, categories, shoutTitle, shoutMessage, hashtags, startDate, endDate, address, images);

            result.add(shout);
        }
        return result;
    }

    @Override
    protected int getMaxConnections() {
        return 5;
    }
}
