package com.app.ballyhoo.crawler.main;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.app.ballyhoo.crawler.R;
import com.app.ballyhoo.crawler.dbconnector.DBManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private ProgressDialog progressDialog;
    private FloatingActionButton fab, delete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressDialog = new ProgressDialog(this);

        RecyclerView rv = findViewById(R.id.rv);
        final ShoutsAdapter adapter = new ShoutsAdapter();
        rv.setAdapter(adapter);

        fab = findViewById(R.id.fab);
        delete = findViewById(R.id.delete);
        fab.setEnabled(false);
        delete.setEnabled(false);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.setProgress(0);
                progressDialog.setMax(1);
                progressDialog.setMessage("Lädt...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.show();

                ModulesManager modulesManager = new ModulesManager(MainActivity.this, adapter, progressDialog);
                modulesManager.start();
            }
        });

        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.setProgress(0);
                progressDialog.setMax(1);
                progressDialog.setMessage("Löscht...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.show();

                DBManager manager = new DBManager();
                manager.deleteAll().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressDialog.hide();
                        Toast.makeText(MainActivity.this, "Alles gelöscht!", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        login();
    }

    private void login() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signInAnonymously().addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                fab.setEnabled(true);
                delete.setEnabled(true);
            }
        });
    }
}
