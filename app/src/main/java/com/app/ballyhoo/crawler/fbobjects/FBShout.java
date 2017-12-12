package com.app.ballyhoo.crawler.fbobjects;

import android.location.Address;

import com.app.ballyhoo.crawler.main.Util;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Viet on 13.08.2017.
 */

public class FBShout {
    private long timestamp;
    private Util.ShoutStatus status;
    private String opID;
    private Map<String, String> imgNames;
    private String title;
    private String message;

    private Map<String, Double> location;
    private Map<String, Integer> validity;

    public FBShout(long timestamp, Util.ShoutStatus status, String opID, List<String> imgNames,
                   String title, String message, Address location, LocalDateTime from, LocalDateTime until) {
        this.timestamp = timestamp;
        this.status = status;
        this.opID = opID;
        this.title = title;
        this.message = message;
        this.location = new HashMap<>();
        validity = new HashMap<>();
        this.location.put("lat", location.getLatitude());
        this.location.put("lon", location.getLongitude());
        validity.put("fromDateYear", from.getYear());
        validity.put("fromDateMonth", from.getMonthOfYear());
        validity.put("fromDateDay", from.getDayOfMonth());
        validity.put("fromHour", from.getHourOfDay());
        validity.put("fromMinute", from.getMinuteOfHour());

        if (until != null) {
            validity.put("untilDateYear", until.getYear());
            validity.put("untilDateMonth", until.getMonthOfYear());
            validity.put("untilDateDay", until.getDayOfMonth());
            validity.put("untilHour", until.getHourOfDay());
            validity.put("untilMinute", until.getMinuteOfHour());
        }

        this.imgNames = new HashMap<>();
        for (int i = 0; i < imgNames.size(); i++)
            this.imgNames.put("" + i, imgNames.get(i));
    }

    public Util.ShoutStatus getStatus() { return status; }

    public long getTimestamp() { return timestamp; }

    public Map<String, String> getImgNames() {
        return imgNames;
    }

    public String getOpID() {
        return opID;
    }

    public String getTitle() { return title; }

    public String getMessage() {
        return message;
    }

    public Map<String, Double> getLocation() { return location; }

    public Map<String, Integer> getValidity()  { return validity; }
}
