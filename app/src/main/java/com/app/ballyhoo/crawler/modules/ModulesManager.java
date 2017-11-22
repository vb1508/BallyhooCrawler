package com.app.ballyhoo.crawler.modules;

import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

import com.app.ballyhoo.crawler.dbconnector.DBManager;
import com.app.ballyhoo.crawler.main.MainActivity;
import com.app.ballyhoo.crawler.main.Shout;
import com.app.ballyhoo.crawler.main.ShoutsAdapter;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

/**
 * Created by Viet on 21.11.2017.
 */

public class ModulesManager extends Thread implements Observer {
    private ShoutsAdapter adapter;
    private ProgressDialog progressDialog;

    private Collection<AbstractModule> modules;
    private DBManager dbManager;

    public ModulesManager(Context context, ShoutsAdapter adapter, ProgressDialog progressDialog) {
        this.adapter = adapter;
        this.progressDialog = progressDialog;

        dbManager = new DBManager();

        modules = new HashSet<>();
        modules.add(new VirtualNightsModule(context));

        for (AbstractModule module: modules)
            module.addObserver(this);
    }

    @Override
    public void run() {
        final LocalDate startDate = LocalDate.now();
        final LocalDate endDate = startDate.plusDays(1);

        final Collection<Task<Map<LocalDate, Set<Shout>>>> tasks = new HashSet<>();
        for (AbstractModule module: modules) {
            tasks.add(module.startParsing("karlsruhe", startDate, endDate));
        }

        Tasks.whenAll(tasks).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                final Map<LocalDate, Set<Shout>> shouts = new HashMap<>();

                LocalDate startFilter = null;
                LocalDate endFilter = null;

                for (Task<Map<LocalDate, Set<Shout>>> task: tasks) {
                    Map<LocalDate, Set<Shout>> temp = task.getResult();
                    for (LocalDate d : temp.keySet()) {
                        if (startFilter == null || startFilter.isAfter(d))
                            startFilter = d;
                        if (endFilter == null || endFilter.isBefore(d))
                            endFilter = d;
                        if (shouts.get(d) == null)
                            shouts.put(d, new HashSet<Shout>());
                        shouts.get(d).addAll(temp.get(d));
                    }
                }

                dbManager.init(startFilter, endFilter).addOnSuccessListener(new OnSuccessListener<Map<LocalDate, Set<String>>>() {
                    @Override
                    public void onSuccess(Map<LocalDate, Set<String>> localDateSetMap) {
                        Set<Shout> filtered = new HashSet<>();
                        for (Map.Entry<LocalDate, Set<Shout>> e: shouts.entrySet()) {
                            for (Shout s: e.getValue()) {
                                if (localDateSetMap.get(e.getKey()) == null || !localDateSetMap.get(e.getKey()).contains(s.getId())) {
                                    filtered.add(s);
                                    dbManager.addShout(s);
                                }
                            }
                        }
                        adapter.setShouts(filtered);
                    }
                });
            }
        });
    }

    @Override
    public void update(Observable observable, Object o) {
        progressDialog.show();
        if (observable instanceof AbstractModule) {
            AbstractModule module = (AbstractModule) observable;
            progressDialog.setMax(module.getMaxProgress());
            progressDialog.incrementProgressBy(1);

            if (progressDialog.getProgress() == progressDialog.getMax())
                progressDialog.dismiss();
        }
    }
}
