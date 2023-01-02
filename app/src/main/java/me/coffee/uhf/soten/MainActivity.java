package me.coffee.uhf.soten;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SotenUHF.getInstance().init(this);
        SotenUHF.getInstance().start();
        SotenUHF.getInstance().setListener(value -> {
            Log.d("UHF-TID", value);
        });

        findViewById(R.id.start_btn).setOnClickListener(v -> {
            SotenUHF.getInstance().read(Integer.MAX_VALUE);
        });
        findViewById(R.id.stop_btn).setOnClickListener(v -> {
            SotenUHF.getInstance().stop();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SotenUHF.getInstance().close();
    }
}