package com.abed.moka7app;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.nfc.Tag;
import android.os.Handler;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.abed.moka7app.moka7.*;

public class SecondActivity extends AppCompatActivity {

    private Button backButton, monitorButton;
    private static final String TAG = "SecondActivity";
    public String ip;
    public int slot;
    public int rack;
    private TextView test_read_variable;
    private Handler cyclicHandler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        initViews();
        saveSharedData();
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openMainActivity();
            }
        });

        monitorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readDB();
            }
        });
    }

    private void initViews() {
        //button
        backButton = findViewById(R.id.backButton);
        monitorButton = findViewById(R.id.monitorButton);
        test_read_variable = findViewById(R.id.test_read_variable);
    }

    public void saveSharedData()
    {
        ip = getIntent().getExtras().getString("ip");
        rack=getIntent().getExtras().getInt("rack");
        slot=getIntent().getExtras().getInt("slot");
        test_read_variable.setText(ip+" "+rack+" "+slot);
    }

    public void openMainActivity()
    {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void readDB()
    {
        PlcConnection p = new PlcConnection();
        new Thread(p).start();
        //cyclicHandler.post(p);

    }

    private class PlcConnection implements Runnable {
        private final S7Client Client;
        int res;
        int selectedArea = S7.S7AreaDB;
        int dBNumber = 3;
        int offset = 14;
        int length =4;

        public PlcConnection() {
            Client = new S7Client();

            Log.d(TAG, "Constructor");
        }

        @Override
        public void run() {

            Client.SetConnectionType(S7.OP);
            res=Client.ConnectTo(ip, rack, slot);

            if (res==0){
                byte[] data = new byte[4];
                res = Client.ReadArea(selectedArea, dBNumber, offset, length, data);
                if (res==0) {
                    final int val = S7.GetDIntAt(data,0);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    test_read_variable.setText(val+"");
                                }
                            });

                    /*Intent i = new Intent("notifyText");
                    i.putExtra("Message",String.format("Value DB %d",val));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);*/
                }else {
                    Intent i = new Intent("notifyText");
                    i.putExtra("Message", String.format("Read error: %d - %s", res, S7Client.ErrorText(res)));
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
                }
                Log.d(TAG, "run");
            }
            //cyclicHandler.postDelayed(this,2000);
        } // end run
    }

    public void scheduleJob(View v) {
        ComponentName componentName = new ComponentName(this, PlcJobService.class);
        JobInfo info = new JobInfo.Builder(123, componentName).setRequiresCharging(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setPersisted(true)
                .setPeriodic(15 * 60 * 1000)
                .build();

        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = scheduler.schedule(info);
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job scheduled");
        } else {
            Log.d(TAG, "Job scheduling failed");
        }
    }

    public void cancelJob(View v) {
        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        scheduler.cancel(123);
        Log.d(TAG, "Job cancelled");
    }
}