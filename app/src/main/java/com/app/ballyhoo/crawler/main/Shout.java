package com.app.ballyhoo.crawler.main;

import android.graphics.Bitmap;
import android.location.Address;

import com.app.ballyhoo.crawler.modules.AbstractModule;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Viet on 20.11.2017.
 */

public class Shout {
    private Set<Util.ShoutCategory> categories;
    private int id;
    private String sRef;
    private AbstractModule module;

    private String url;
    private LocalDateTime startDate, endDate;
    private Address address;
    private String title;
    private String message;
    private List<String> imgNames;
    private List<Bitmap> images;
    private Set<String> hashtags;

    public Shout(String url, AbstractModule module, Set<Util.ShoutCategory> categories, String title, String message, Collection<String> hashtags, LocalDateTime startDate, LocalDateTime endDate, Address address, List<Bitmap> images) {
        this.url = url;
        this.module = module;

        this.categories = categories;
        this.startDate = startDate;
        //if (endDate == null) {
        //    this.endDate = new LocalDateTime(startDate.getYear(), startDate.getMonthOfYear(), startDate.getDayOfMonth(), 0, 0);
        //    if (!this.endDate.isAfter(startDate))
        //        this.endDate = this.endDate.plusDays(1);
        //} else
        this.endDate = endDate;
        this.address = address;
        this.title = title;
        this.message = message;
        this.images = images;

        this.id = generateId();

        parseHashtags();
        this.hashtags.addAll(hashtags);
    }

    public Shout(String url, AbstractModule module, Set<Util.ShoutCategory> categories, String title, String message, Collection<String> hashtags, DateTime startDate, DateTime endDate, Address address, List<Bitmap> images) {
        this(url, module, categories, title, message, hashtags, startDate.toLocalDateTime(), endDate.toLocalDateTime(), address, images);
    }

    public int getId() { return id; }

    public String getUrl() { return url; }

    public AbstractModule getModule() { return module; }

    public Set<Util.ShoutCategory> getCategories() { return categories; }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public Address getAddress() {
        return address;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public void setImgNames(List<String> imgNames) {
        this.imgNames = imgNames;
    }

    public List<String> getImgNames() {
        return imgNames;
    }

    public void setSRef(String sRef) {
        this.sRef = sRef;
    }

    public String getSRef() {
        return sRef;
    }

    public List<Bitmap> getImages() {
        return images;
    }

    public boolean isValid() {
        return (url != null && categories != null && startDate != null && address != null
                && title != null && message != null && images != null);
    }

    @Override
    public boolean equals(Object obj) {
        return id == ((Shout) obj).id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    int generateId() {
        int result = startDate.hashCode() +
                ((Double) address.getLatitude()).hashCode() + ((Double) address.getLongitude()).hashCode();
        if (endDate != null)
            result += endDate.hashCode();
        return result;
    }

    public Set<String> getHashtags() {
        return hashtags;
    }

    private void parseHashtags() {
        hashtags = new HashSet<>();
        Pattern MY_PATTERN = Pattern.compile("#([A-Za-z0-9_-]+)");
        Matcher mat = MY_PATTERN.matcher(message);
        while (mat.find()) {
            String hashtag = mat.group(0);
            hashtags.add(hashtag.substring(1, hashtag.length()));
        }
    }
}
