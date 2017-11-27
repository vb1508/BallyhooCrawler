package com.app.ballyhoo.crawler.modules;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.util.Pair;

import com.app.ballyhoo.crawler.main.Shout;
import com.app.ballyhoo.crawler.main.Util;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

/**
 * Created by Viet on 19.11.2017.
 */

public abstract class AbstractModule extends Observable {
    private String moduleName;
    private int maxProgress;
    private Context context;

    LocalDate startDate, endDate;

    AbstractModule(Context context, String moduleName, LocalDate startDate, LocalDate endDate) {
        this.context = context;
        this.moduleName = moduleName;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getModuleName() { return moduleName; }

    public int getMaxProgress() {
        return maxProgress;
    }

    public Task<Map<LocalDate, Set<Shout>>> parseHelper(String city, final Map<String, Integer> crawled) {
        maxProgress = 1;

        final TaskCompletionSource<Map<LocalDate, Set<Shout>>> tcs = new TaskCompletionSource<>();

        final Collection<Task<Map<String, Map<String, Object>>>> tasks = new HashSet<>();

        Map<String, Map<String, Object>> parentUrls = getParentURLs(city);
        for (Map.Entry<String, Map<String, Object>> e: parentUrls.entrySet()) {
            TaskCompletionSource<Map<String, Map<String, Object>>> temp = new TaskCompletionSource<>();
            new ParseChildURLsThread(temp, e.getKey(), e.getValue()).start();
            tasks.add(temp.getTask());
        }

        Tasks.whenAll(tasks).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Map<String, Map<String, Object>> hrefs = new HashMap<>();
                for (Task<Map<String, Map<String, Object>>> task: tasks) {
                    if (task.getResult() != null && !task.getResult().isEmpty())
                        for (Map.Entry<String, Map<String, Object>> e: task.getResult().entrySet()) {
                            if (!crawled.keySet().contains(e.getKey()))
                                hrefs.put(e.getKey(), e.getValue());
                        }
                }

                maxProgress = hrefs.size();
                final Collection<Task<Set<Shout>>> crawlChildrenTasks = new HashSet<>();
                for (Map.Entry<String, Map<String, Object>> e: hrefs.entrySet()) {
                    TaskCompletionSource<Set<Shout>> temp = new TaskCompletionSource<>();
                    new ParseChildThread(temp, e.getKey(), e.getValue()).start();
                    crawlChildrenTasks.add(temp.getTask());
                }
                Tasks.whenAll(crawlChildrenTasks).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Map<LocalDate, Set<Shout>> result = new HashMap<>();
                        for (Task<Set<Shout>> task: crawlChildrenTasks) {
                            if (task.getResult() != null && task.isSuccessful())
                                for (Shout s: task.getResult()) {
                                    if (s != null && s.isValid()) {
                                        if (result.get(s.getStartDate().toLocalDate()) == null)
                                            result.put(s.getStartDate().toLocalDate(), new HashSet<Shout>());
                                        result.get(s.getStartDate().toLocalDate()).add(s);
                                    }
                                }
                        }
                        tcs.setResult(result);
                    }
                });
            }
        });

        return tcs.getTask();
    }

    Address parseLocationFromAddress(String strAddress) {
        Address result = null;
        try {
            Geocoder coder = new Geocoder(context);
            List<Address> addresses = coder.getFromLocationName(Util.clean(strAddress),5);
            result = addresses.get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    Bitmap downloadImage(String URL) {
        Bitmap bitmap = null;
        try {
            InputStream in = openHttpConnection(URL);
            bitmap = BitmapFactory.decodeStream(in);
            in.close();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return bitmap;
    }

    protected abstract Map<String, Map<String, Object>> getParentURLs(String city);

    protected abstract Map<String, Map<String, Object>> parseChildURLs(String url, Map<String, Object> params) throws IOException;

    protected abstract Set<Shout> parseShouts(String url, Map<String, Object> params) throws IOException, JSONException;

    private InputStream openHttpConnection(String urlString) throws IOException {
        InputStream in = null;
        int response;

        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();

        if (!(conn instanceof HttpURLConnection))
            throw new IOException("Not an HTTP connection");

        try {
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            response = httpConn.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
            }
        } catch (Exception ex) {
            throw new IOException("Error connecting");
        }
        return in;
    }

    private class ParseChildURLsThread extends Thread {
        TaskCompletionSource<Map<String, Map<String, Object>>> tcs;
        String url;
        Map<String, Object> params;

        public ParseChildURLsThread(TaskCompletionSource<Map<String, Map<String, Object>>> tcs, String url, Map<String, Object> params) {
            this.tcs = tcs;
            this.url = url;
            this.params = params;
        }

        @Override
        public void run() {
            Map<String, Map<String, Object>> result = new HashMap<>();
            try {
                result = parseChildURLs(url, params);
            } catch (IOException e) {
                e.printStackTrace();
            }
            tcs.setResult(result);
        }
    }

    private class ParseChildThread extends Thread {
        TaskCompletionSource<Set<Shout>> tcs;
        String url;
        Map<String, Object> params;

        ParseChildThread(TaskCompletionSource<Set<Shout>> tcs, String url, Map<String, Object> params) {
            this.tcs = tcs;
            this.url = url;
            this.params = params;
        }

        @Override
        public void run() {
            Set<Shout> result = null;
            try {
                result = parseShouts(url, params);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // progress bar
            setChanged();
            notifyObservers(1);

            tcs.setResult(result);
        }
    }
}
