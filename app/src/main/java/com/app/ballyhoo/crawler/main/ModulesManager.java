package com.app.ballyhoo.crawler.main;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.annotation.NonNull;

import com.app.ballyhoo.crawler.dbconnector.DBManager;
import com.app.ballyhoo.crawler.modules.AbstractModule;
import com.app.ballyhoo.crawler.modules.FussballDEModule;
import com.app.ballyhoo.crawler.modules.KACityModule;
import com.app.ballyhoo.crawler.modules.KAMensaModule;
import com.app.ballyhoo.crawler.modules.KANightlifeModule;
import com.app.ballyhoo.crawler.modules.KarlsruheDEModule;
import com.app.ballyhoo.crawler.modules.MeineStadtModule;
import com.app.ballyhoo.crawler.modules.ViktorModul;
import com.app.ballyhoo.crawler.modules.VirtualNightsModule;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import org.joda.time.LocalDate;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Viet on 21.11.2017.
 */

public class ModulesManager extends Thread implements Observer {
    private int uploadProgress = 0;

    final int MAX_UPLOAD_THREADS = 2;

    private ShoutsAdapter adapter;
    private ProgressDialog progressDialog;

    private Collection<AbstractModule> modules;
    private DBManager dbManager;

    public ModulesManager(Context context, ShoutsAdapter adapter, ProgressDialog progressDialog) {
        this.adapter = adapter;
        this.progressDialog = progressDialog;

        dbManager = new DBManager();

        modules = new HashSet<>();
//        modules.add(new KarlsruheDEModule(context));
        modules.add(new VirtualNightsModule(context));
//        modules.add(new KACityModule(context));
//        modules.add(new KANightlifeModule(context));
//        modules.add(new KAMensaModule(context));
//        modules.add(new FussballDEModule(context));
//        modules.add(new ViktorModul(context));
        //modules.add(new MeineStadtModule(context));

        for (AbstractModule module: modules)
            module.addObserver(this);
    }

    @Override
    public void run() {
        dbManager.init().addOnSuccessListener(new OnSuccessListener<Map<String, Integer>>() {
            @Override
            public void onSuccess(final Map<String, Integer> crawled) {
                final Collection<Task<Map<LocalDate, Set<Shout>>>> tasks = new HashSet<>();
                for (AbstractModule module: modules) {
                    tasks.add(module.parseHelper("karlsruhe", crawled));
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
                        upload(shouts, crawled);
                    }
                });
            }
        });
    }

    @Override
    public synchronized void update(Observable observable, Object o) {
        progressDialog.show();
        int sumProgress = 1 + uploadProgress;
        for (AbstractModule module: modules)
            sumProgress += module.getMaxProgress();

        progressDialog.setMax(sumProgress);
        progressDialog.incrementProgressBy((int) o);

        if (progressDialog.getProgress() == progressDialog.getMax())
            progressDialog.dismiss();
    }

    private void upload(Map<LocalDate, Set<Shout>> shouts, Map<String, Integer> crawled) {
        final Set<Shout> filtered = new HashSet<>();
        final Queue<Shout> queue = new ConcurrentLinkedQueue<>();

        final Collection<Task<Void>> uploadShoutsTask = new HashSet<>();
        for (Map.Entry<LocalDate, Set<Shout>> e: shouts.entrySet()) {
            for (Shout s: e.getValue()) {
                if (!crawled.containsValue(s.getId())) {
                    filtered.add(s);
                    queue.add(s);
                    uploadShoutsTask.add(dbManager.addShout(s));
                }
            }
        }
        uploadProgress = filtered.size();
        update(null, 0);

        Tasks.whenAll(uploadShoutsTask).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> aVoid) {
                final TaskCompletionSource tcs = new TaskCompletionSource();
                final Collection<Task<Void>> uploadTasks = new HashSet<>();

                for (int i = 0; i < MAX_UPLOAD_THREADS && !queue.isEmpty(); i++)
                    uploadTasks.add(dbManager.addShoutImages(queue.poll(), "shout_images"));

                Tasks.whenAll(uploadTasks).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        update(null, uploadTasks.size());
                        uploadHelper(tcs, queue);
                    }
                });
                tcs.getTask().addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        adapter.setShouts(filtered);
                    }
                });
            }
        });
    }

    private void uploadHelper(final TaskCompletionSource<Void> tcs, final Queue<Shout> queue) {
        final Collection<Task<Void>> uploadTasks = new HashSet<>();
        for (int i = 0; i < MAX_UPLOAD_THREADS && !queue.isEmpty(); i++) {
            uploadTasks.add(dbManager.addShoutImages(queue.poll(), "shout_images"));
        }
        Tasks.whenAll(uploadTasks).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                update(null, uploadTasks.size());
                if (queue.isEmpty()) {
                    update(null, 1);
                    tcs.setResult(task.getResult());
                }
                else
                    uploadHelper(tcs, queue);
            }
        });
    }
}
