package me.coffee.uhf.soten;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv = findViewById(R.id.tv);
        tv.setMovementMethod(ScrollingMovementMethod.getInstance());

        SotenUHF.getInstance().init(this);
        SotenUHF.getInstance().start();
        SotenUHF.getInstance().setListener(value -> {
            Log.d("UHF-TID", "TID:" + value);
            runOnUiThread(() -> {
                tv.append(value + "\r\n");
            });
        });

        findViewById(R.id.start_btn).setOnClickListener(v -> {
            tv.setText(null);
            SotenUHF.getInstance().read(1);
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