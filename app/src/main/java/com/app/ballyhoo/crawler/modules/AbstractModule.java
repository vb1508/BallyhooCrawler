package com.app.ballyhoo.crawler.modules;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;

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

    AbstractModule(Context context, String moduleName) {
        this.context = context;
        this.moduleName = moduleName;
    }

    public String getModuleName() { return moduleName; }

    public int getMaxProgress() {
        return maxProgress;
    }

    public Task<Map<LocalDate, Set<Shout>>> parseHelper(String city, LocalDate start, LocalDate end, final Map<String, Integer> crawled) {
        maxProgress = 1;

        final TaskCompletionSource<Map<LocalDate, Set<Shout>>> tcs = new TaskCompletionSource<>();

        final Collection<Task<Set<String>>> tasks = new HashSet<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            TaskCompletionSource<Set<String>> temp = new TaskCompletionSource<>();
            String childUrl = getURL(city, date);
            new ParseChildURLsThread(temp, childUrl).start();
            tasks.add(temp.getTask());
        }

        Tasks.whenAll(tasks).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Set<String> hrefs = new HashSet<>();
                for (Task<Set<String>> task: tasks) {
                    if (task.getResult() != null && !task.getResult().isEmpty())
                        hrefs.addAll(task.getResult());
                }
                hrefs.removeAll(crawled.keySet());

                maxProgress = hrefs.size();
                final Collection<Task<Set<Shout>>> crawlChildrenTasks = new HashSet<>();
                for (String href: hrefs) {
                    TaskCompletionSource<Set<Shout>> temp = new TaskCompletionSource<>();
                    new ParseChildThread(temp, href).start();
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

    protected abstract String getURL(String city, LocalDate date);

    protected abstract Set<String> parseChildURLs(String url) throws IOException;

    protected abstract Set<Shout> parseShouts(String url) throws IOException, JSONException;

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
        TaskCompletionSource<Set<String>> tcs;
        String url;

        public ParseChildURLsThread(TaskCompletionSource<Set<String>> tcs, String url) {
            this.tcs = tcs;
            this.url = url;
        }

        @Override
        public void run() {
            Set<String> result = new HashSet<>();
            try {
                result = parseChildURLs(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            tcs.setResult(result);
        }
    }

    private class ParseChildThread extends Thread {
        TaskCompletionSource<Set<Shout>> tcs;
        String url;

        ParseChildThread(TaskCompletionSource<Set<Shout>> tcs, String url) {
            this.tcs = tcs;
            this.url = url;
        }

        @Override
        public void run() {
            Set<Shout> result = null;
            try {
                result = parseShouts(url);
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
