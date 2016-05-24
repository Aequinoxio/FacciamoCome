package com.example.utente.facciamocome;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

public class ShareActivity extends AppCompatActivity {

    // ID per lo share picker
    static final int SHARE_PICKER=777;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Rimuovo la chiamata a setContentView per non renderla visibile. Ho anche impostato lo stile invisibile nel manifest
       // setContentView(R.layout.activity_share);

        // Questa roba è stata generata da Android Studio e non serve
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        String s = getIntent().getStringExtra(getString(R.string.IntentExtraPhrase));

        scegliShareMethod(s);

        // Termino subito l'attività
        this.finish();
    }
    public void scegliShareMethod(String phrase){
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);

        sharingIntent.setType(getString(R.string.ShareMimeType));
        sharingIntent.putExtra(Intent.EXTRA_TEXT, phrase);

        startActivityForResult(Intent.createChooser(sharingIntent, getString(R.string.CondividiCon)), SHARE_PICKER);
    }
}
