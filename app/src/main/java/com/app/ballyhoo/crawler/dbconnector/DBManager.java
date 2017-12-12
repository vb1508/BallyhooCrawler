package com.app.ballyhoo.crawler.dbconnector;

import android.graphics.Bitmap;
import android.location.Address;
import android.support.annotation.NonNull;
import android.util.Pair;

import com.app.ballyhoo.crawler.fbobjects.FBCrawlerRef;
import com.app.ballyhoo.crawler.fbobjects.FBProfileShoutRef;
import com.app.ballyhoo.crawler.fbobjects.FBShout;
import com.app.ballyhoo.crawler.main.Shout;
import com.app.ballyhoo.crawler.main.Util;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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

import static com.app.ballyhoo.crawler.dbconnector.RefManager.CRAWLER_DBREF;
import static com.app.ballyhoo.crawler.dbconnector.RefManager.IMAGES_DBREF;
import static com.app.ballyhoo.crawler.dbconnector.RefManager.SHOUTS_DBREF;

/**
 * Created by Viet on 20.11.2017.
 */

public class DBManager {

    public Task<Map<String, Integer>> init() {
        final TaskCompletionSource<Map<String, Integer>> tcs = new TaskCompletionSource<>();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        dbRef.child(CRAWLER_DBREF).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Integer> crawledSites = new HashMap<>();
                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    for (DataSnapshot snapshot1: snapshot.getChildren()) {
                        crawledSites.put(snapshot1.child("url").getValue(String.class), snapshot1.child("id").getValue(int.class));
                    }
                }
                tcs.setResult(crawledSites);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return tcs.getTask();
    }

    public Task<Void> addShout(Shout shout) {
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        final String opID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Set<Util.ShoutCategory> categories = shout.getCategories();
        String title = shout.getTitle();
        String message = shout.getMessage();
        List<Bitmap> images = shout.getImages();
        Address address = shout.getAddress();
        LocalDateTime from = shout.getStartDate();
        LocalDateTime until = shout.getEndDate();

        if (categories != null && !categories.isEmpty() && title != null && message != null
                && images != null && address != null && from != null) {

            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
            Collection<Task<?>> tasks = new ArrayList<>();

            DatabaseReference dateRef = dbRef.child(SHOUTS_DBREF).child(RefManager.getDateRef(from.toLocalDate()));
            DatabaseReference sRef = dateRef.child("shouts").push();

            final List<String> imgNames = new ArrayList<>();
            while (imgNames.size() < images.size()) {
                imgNames.add(UUID.randomUUID().toString());
            }
            String sID = sRef.getKey();
            shout.setSRef(sID);
            shout.setImgNames(imgNames);

            final long timestamp = System.currentTimeMillis();
            FBShout fbShout = new FBShout(timestamp, Util.ShoutStatus.ACTIVE, opID, imgNames,
                    title, message, address, from, until);
            tasks.add(sRef.child("data").setValue(fbShout));

            for (Util.ShoutCategory category : categories) {
                tasks.add(sRef.child("data").child("categories").push().setValue(category));
            }
            tasks.add(setGeofireRef(dateRef.child("geo"), sID, address.getLatitude(), address.getLongitude()));

            DatabaseReference crawlerRef = dbRef.child(CRAWLER_DBREF).child(shout.getModule().getModuleName()).push();
            tasks.add(crawlerRef.setValue(new FBCrawlerRef(shout.getId(), shout.getUrl())));

            Tasks.whenAll(tasks).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    tcs.setResult(task.getResult());
                }
            });
        } else
            tcs.setException(new Exception("Wrong or insufficient parameters!"));
        return tcs.getTask();
    }

    public Task<Void> addShoutImages(Shout s, String... dbs) {
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();

        String opID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String sID = s.getSRef();
        List<String> imgNames = s.getImgNames();
        List<Bitmap> images = s.getImages();

        if (opID == null || sID == null || opID.isEmpty() || sID.isEmpty()) {
            System.out.println();
        }

        final Collection<Task<UploadTask.TaskSnapshot>> tasks = new HashSet<>();
        // Create a storage reference from our app
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://ballyhoo-78262.appspot.com");
        StorageReference imgRef = storageRef.child(IMAGES_DBREF).child(opID).child("shouts").child(sID);

        for (String db : dbs) imgRef = imgRef.child(db);

        for (int i = 0; i < imgNames.size(); i++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            images.get(i).compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();

            tasks.add(imgRef.child(imgNames.get(i)).putBytes(data));
        }

        Tasks.whenAll(tasks).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                tcs.setResult(task.getResult());
            }
        });
        return tcs.getTask();
    }

    private Task<Void> setGeofireRef(DatabaseReference ref, String id, double lat, double lon) {
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        GeoFire geoFire = new GeoFire(ref);
        geoFire.setLocation(id, new GeoLocation(lat, lon), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null)
                    tcs.setException(error.toException());
                else
                    tcs.setResult(null);
            }
        });
        return tcs.getTask();
    }
}
