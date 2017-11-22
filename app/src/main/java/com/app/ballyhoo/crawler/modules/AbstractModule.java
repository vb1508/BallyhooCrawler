package com.app.ballyhoo.crawler.modules;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;

import com.app.ballyhoo.crawler.main.Shout;
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

    Task<Map<LocalDate, Set<Shout>>> startParsing(String city, final LocalDate startDate, final LocalDate endDate) {
        maxProgress = 0;

        final TaskCompletionSource<Map<LocalDate, Set<Shout>>> tcs = new TaskCompletionSource<>();
        final Collection<Task<Map<LocalDate, Set<Shout>>>> tasks = new HashSet<>();

        for (LocalDate i = startDate; !i.isAfter(endDate); i = i.plusDays(1)) {
            final TaskCompletionSource<Map<LocalDate, Set<Shout>>> task = new TaskCompletionSource<>();
            parseHelper(city, i).addOnSuccessListener(new OnSuccessListener<Map<LocalDate, Set<Shout>>>() {
                @Override
                public void onSuccess(Map<LocalDate, Set<Shout>> shouts) {
                    task.setResult(shouts);
                }
            });
            tasks.add(task.getTask());
        }
        Tasks.whenAll(tasks).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Map<LocalDate, Set<Shout>> result = new HashMap<>();
                for (Task<Map<LocalDate, Set<Shout>>> task: tasks) {
                    for (Map.Entry<LocalDate, Set<Shout>> e: task.getResult().entrySet()) {
                        LocalDate date = e.getKey();
                        if (!date.isBefore(startDate) && !date.isAfter(endDate))
                            for (Shout s: e.getValue()) {
                                if (result.get(date) == null)
                                    result.put(date, new HashSet<Shout>());
                                result.get(date).add(s);
                            }
                    }
                }
                tcs.setResult(result);
            }
        });
        return tcs.getTask();
    }

    Address parseLocationFromAddress(String strAddress) {
        Address result = null;
        try {
            Geocoder coder = new Geocoder(context);
            List<Address> addresses = coder.getFromLocationName(strAddress,5);
            result = addresses.get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    int getMaxProgress() {
        return maxProgress;
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

    protected abstract Collection<String> parseChildURLs(String url) throws IOException;

    protected abstract Shout parseChild(String url) throws IOException, JSONException;

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

    private Task<Map<LocalDate, Set<Shout>>> parseHelper(String city, LocalDate date) {
        TaskCompletionSource<Map<LocalDate, Set<Shout>>> tcs = new TaskCompletionSource<>();
        String childUrl = getURL(city, date);

        new ParseChildURLsThread(tcs, childUrl).start();
        return tcs.getTask();
    }

    private class ParseChildURLsThread extends Thread {
        TaskCompletionSource<Map<LocalDate, Set<Shout>>> tcs;
        String url;

        public ParseChildURLsThread(TaskCompletionSource<Map<LocalDate, Set<Shout>>> tcs, String url) {
            this.tcs = tcs;
            this.url = url;
        }

        @Override
        public void run() {
            Collection<String> result = new HashSet<>();
            try {
                result = parseChildURLs(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            maxProgress += result.size();

            final Collection<Task<Shout>> tasks = new HashSet<>();
            for (String href: result) {
                TaskCompletionSource<Shout> temp = new TaskCompletionSource<>();
                new ParseChildThread(temp, href).start();
                tasks.add(temp.getTask());
            }
            Tasks.whenAll(tasks).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Map<LocalDate, Set<Shout>> result = new HashMap<>();
                    for (Task<Shout> task: tasks) {
                        Shout s = task.getResult();
                        if (s != null) {
                            if (result.get(s.getStartDate().toLocalDate()) == null)
                                result.put(s.getStartDate().toLocalDate(), new HashSet<Shout>());
                            result.get(s.getStartDate().toLocalDate()).add(s);
                        }
                    }
                    tcs.setResult(result);
                }
            });
        }
    }

    private class ParseChildThread extends Thread {
        TaskCompletionSource<Shout> tcs;
        String url;

        ParseChildThread(TaskCompletionSource<Shout> tcs, String url) {
            this.tcs = tcs;
            this.url = url;
        }

        @Override
        public void run() {
            Shout result = null;
            try {
                result = parseChild(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // progress bar
            setChanged();
            notifyObservers();

            tcs.setResult(result);
        }
    }
}
