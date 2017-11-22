package com.app.ballyhoo.crawler.main;

import android.graphics.Bitmap;
import android.location.Address;

import com.app.ballyhoo.crawler.modules.AbstractModule;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.util.List;
import java.util.Set;

/**
 * Created by Viet on 20.11.2017.
 */

public class Shout {
    private Set<Util.ShoutCategory> categories;
    private String id;
    private AbstractModule module;

    private LocalDateTime startDate, endDate;
    private Address address;
    private String title;
    private String message;
    private List<Bitmap> images;

    public Shout(String id, AbstractModule module, Set<Util.ShoutCategory> categories, String title, String message, DateTime startDate, DateTime endDate, Address address, List<Bitmap> images) {
        this.id = id;
        this.module = module;

        this.categories = categories;
        this.startDate = startDate.toLocalDateTime();
        this.endDate = endDate.toLocalDateTime();
        this.address = address;
        this.title = title;
        this.message = message;
        this.images = images;
    }

    public String getId() { return id; }

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

    public List<Bitmap> getImages() {
        return images;
    }

    @Override
    public boolean equals(Object obj) {
        return id.equals(((Shout) obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
