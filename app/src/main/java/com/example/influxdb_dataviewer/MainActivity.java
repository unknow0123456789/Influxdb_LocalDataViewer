package com.example.influxdb_dataviewer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String token = "PkeQ6CfuvBDqGIayS83szsN9jHwLO-ZaP6_E520_OsLxrJUG0kyJYuXKBw6_0JbuXo37dSo6ouaGhUs4iimHlQ==";
        String bucket = "unknowndb";
        String org = "UbuntuVM";
        String StartSampleTime="-1h";
        ImageButton RefreshButton=findViewById(R.id.Refresh_BTN);
        RefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InfluxDBClient client = InfluxDBClientFactory.create("http://192.168.1.11:3004", token.toCharArray());
                ReceiveDataFromInfluxDB(client, bucket, org, StartSampleTime, new CustomResponse() {
                    @Override
                    public void OnResponse(Object obj) {

                    }
                });
            }
        });
    }
    public void ReceiveDataFromInfluxDB(InfluxDBClient currentClient, String BucketName, String ORG, String SampleFrom,CustomResponse CR)
    {
        new Thread()
        {
            public void run()
            {
                try {
                    String query = "from(bucket: \""+BucketName+"\") |> range(start: "+SampleFrom+")";
                    List<FluxTable> tables = currentClient.getQueryApi().query(query, ORG);
                    for (FluxTable table : tables) {
                        Log.d("ReceivedTables", table.toString());
                        for (FluxRecord record : table.getRecords()) {
                            Log.d("ReceivedQuery", record.getMeasurement()+" : "+record.getValue().toString());
                        }
                    }
                    CR.OnResponse(tables);
                }
                catch (Exception ex)
                {
                    Log.e("Query_Exception", ex.getMessage());
                }
            }
        }.start();
    }
    public void SetFragmentToFocus(Fragment theFragment)
    {
        try {
            FragmentManager fragmentManager=getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.FragmentHolder,theFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit();
        }
        catch (Exception ex)
        {
            Log.d("Attach Fragment Failed",ex.getMessage());
        }
    }
}