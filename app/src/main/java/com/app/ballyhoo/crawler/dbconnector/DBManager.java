package com.app.ballyhoo.crawler.dbconnector;

import android.graphics.Bitmap;
import android.location.Address;
import android.support.annotation.NonNull;
import android.util.Pair;

import com.app.ballyhoo.crawler.fbobjects.FBProfileShoutRef;
import com.app.ballyhoo.crawler.fbobjects.FBShout;
import com.app.ballyhoo.crawler.main.Shout;
import com.app.ballyhoo.crawler.main.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Viet on 20.11.2017.
 */

public class DBManager {
    private final String CRAWLER_DBREF = "crawled";
    private final String PROFILES_DBREF = "user_profiles";
    private final String SHOUTS_DBREF = "shouts";
    private final String IMAGES_DBREF = "images";

    public Task<Map<LocalDate, Set<String>>> init(LocalDate start, LocalDate end) {
        final TaskCompletionSource<Map<LocalDate, Set<String>>> tcs = new TaskCompletionSource<>();
        final Map<LocalDate, Set<String>> result = new HashMap<>();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        final Collection<Task<Pair<LocalDate, HashSet<String>>>> tasks = new HashSet<>();
        for (LocalDate i = start; !i.isAfter(end); i = i.plusDays(1)) {
            final TaskCompletionSource<Pair<LocalDate, HashSet<String>>> task = new TaskCompletionSource<>();
            final Pair<LocalDate, HashSet<String>> crawledModules = new Pair<>(i, new HashSet<String>());

            String dateRef = getDateRef(i);
            dbRef.child(CRAWLER_DBREF).child(dateRef).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                        for (DataSnapshot snapshot1: snapshot.getChildren())
                            crawledModules.second.add(snapshot1.getValue(String.class));
                    }
                    task.setResult(crawledModules);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            tasks.add(task.getTask());
        }
        Tasks.whenAll(tasks).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                for (Task<Pair<LocalDate, HashSet<String>>> task: tasks) {
                    if (result.get(task.getResult().first) == null)
                        result.put(task.getResult().first, new HashSet<String>());
                    result.get(task.getResult().first).addAll(task.getResult().second);
                }
                tcs.setResult(result);
            }
        });
        return tcs.getTask();
    }

    public Task<Void> addShout(Shout shout) {
        final TaskCompletionSource tcs = new TaskCompletionSource();
        final String opID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Set<Util.ShoutCategory> categories = shout.getCategories();
        String opName = "";
        String title = shout.getTitle();
        String message = shout.getMessage();
        List<Bitmap> images = shout.getImages();
        Address address = shout.getAddress();
        LocalDateTime from = shout.getStartDate();
        LocalDateTime until = shout.getEndDate();

        if (categories != null && !categories.isEmpty() && message != null && !message.isEmpty()
                && images != null && address != null && from != null && until != null) {

            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

            Collection<Task<?>> tasks = new ArrayList<>();

            String dateRef = getDateRef(from.toLocalDate());
            String locRef = getLocRef(address);

            DatabaseReference sRef = dbRef.child(SHOUTS_DBREF).child(locRef).child(dateRef).push();
            final String sID = sRef.getKey();

            final List<String> imgNames = new ArrayList<>();
            while (imgNames.size() < images.size()) {
                imgNames.add(UUID.randomUUID().toString());
            }
            tasks.addAll(addShoutImagesTask(opID, sRef.getKey(), imgNames, images, "shout_images"));

            final long timestamp = System.currentTimeMillis();
            FBShout fbShout = new FBShout(timestamp, Util.ShoutStatus.ACTIVE, opID, opName, imgNames,
                    title, message, address, from, until);
            tasks.add(sRef.child("data").setValue(fbShout));

            for (Util.ShoutCategory category: categories) {
                tasks.add(sRef.child("data").child("categories").push().setValue(category));
            }

            // Dealing with points
            DatabaseReference myShoutsRef = dbRef.child(PROFILES_DBREF).child(opID).child("myShouts");
            FBProfileShoutRef dateLocRef = new FBProfileShoutRef(Util.ShoutType.LOCAL, dateRef, locRef);
            tasks.add(myShoutsRef.child(sID).setValue(dateLocRef));

            DatabaseReference crawlerRef = dbRef.child(CRAWLER_DBREF).child(dateRef).child(shout.getModule().getModuleName()).push();
            tasks.add(crawlerRef.setValue(shout.getId()));

            Tasks.whenAll(tasks).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        tcs.setResult(task.getResult());
                    } else
                        tcs.setException(task.getException());
                }
            });
        } else
            tcs.setException(new Exception("Wrong or insufficient parameters!"));
        return tcs.getTask();
    }

    private Collection<UploadTask> addShoutImagesTask(String opID, String sID, List<String> imgNames, List<Bitmap> images, String... dbs) {
        Collection<UploadTask> tasks = new ArrayList<>();
        // Create a storage reference from our app
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://ballyhoo-78262.appspot.com");
        StorageReference imgRef = storageRef.child(IMAGES_DBREF).child(opID).child(sID);

        for (int i = 0; i < dbs.length; i++)
            imgRef = imgRef.child(dbs[i]);

        for (int i = 0; i < imgNames.size(); i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            images.get(i).compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();

            tasks.add(imgRef.child(imgNames.get(i)).putBytes(data));
        }
        return tasks;
    }

    private String getLocRef(Address l) {
        return "lat_" + (int) Math.floor(l.getLatitude() * 10) + "_lon_" + (int) Math.floor(l.getLongitude() * 10);
    }

    private String getDateRef(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthOfYear();
        int day = date.getDayOfMonth();

        return year + "-" + month + "-" + day;
    }
}
