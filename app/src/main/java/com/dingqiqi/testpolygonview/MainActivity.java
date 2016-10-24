package com.dingqiqi.testpolygonview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private PolygonView mPolygonView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPolygonView = (PolygonView) findViewById(R.id.polygonView);

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPolygonView.setPolygonCountText(5, new String[]{"德玛西亚1", "德玛西亚2", "德玛西亚3", "德玛西亚4", "德玛西亚5", "德玛西亚6"});
                mPolygonView.setRadiusCount(3);
                mPolygonView.setValue(new float[]{1f, 0.8f, 0.5f, 0.9f, 0.6f});
            }
        });

    }
}
