package com.example.influxdb_dataviewer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

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

        InfluxDBClient client = InfluxDBClientFactory.create("http://192.168.1.11:3004", token.toCharArray());
        String query = "from(bucket: \""+bucket+"\") |> range(start: "+StartSampleTime+")";

        new Thread()
        {
            public void run()
            {
                try {
                    List<FluxTable> tables = client.getQueryApi().query(query, org);
                    for (FluxTable table : tables) {
                        Log.d("testTables", table.toString());
                        for (FluxRecord record : table.getRecords()) {
                            Log.d("testQuery", record.getMeasurement()+" : "+record.getValue().toString());
                        }
                    }
                }
                catch (Exception ex)
                {
                    
                }
            }
        }.start();
    }
}