package com.example.influxdb_dataviewer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.influxdb_dataviewer.ApiRelated.RefreshOption;
import com.example.influxdb_dataviewer.Fragments.Graph_Fragment;
import com.example.influxdb_dataviewer.Fragments.Login_Fragment;
import com.example.influxdb_dataviewer.Fragments.Setting_Fragment;
import com.example.influxdb_dataviewer.Fragments.Table_Fragment;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CustomResponse{
    public InfluxDBClient currentClient;
    public RefreshOption CurrentRefreshOption;
    public List<FluxTable> CurrentTables;
    private Fragment CurrentFragment;
    private boolean RefreshPressedCheck=false;
    public int CurrentFragmentID =1;
    public CustomResponse PostReceivedDB=new CustomResponse() {
        @Override
        public void OnResponse(Object obj) {
            CurrentTables=(List<FluxTable>) obj;
            //Log.d("testTransferTables", CurrentTables.get(0).getRecords().get(0).getMeasurement());
            if(CurrentFragmentID !=2 && CurrentFragmentID !=3)
            {
                ChangeFragmentTo(2,false);
            }
            else {
                switch (CurrentFragmentID){
                    case 2:
                        if(RefreshPressedCheck==true){
                            ChangeFragmentTo(2,true);
                            RefreshPressedCheck=false;
                        }
                        break;
                    case 3:
                        ChangeFragmentTo(3,true);
                        break;
                }
            }
        }
    };
    LinearLayout NavMenu;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //untouched

        this.NavMenu=findViewById(R.id.NavMenu);
        State1();
        this.OnResponse(CurrentFragmentID);
        ImageButton RefreshButton=findViewById(R.id.Refresh_BTN),TableButton=findViewById(R.id.TableFragmentBTN),GraphButton=findViewById(R.id.GraphFragmentBTN),SettingButton=findViewById(R.id.SettingBTN);
        RefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RefreshPressedCheck=true;
                ReceiveDataFromInfluxDB(PostReceivedDB,true);
            }
        });
        TableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ChangeFragmentTo(2,false);
            }
        });
        GraphButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChangeFragmentTo(3,false);
            }
        });
        SettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChangeFragmentTo(4,false);
            }
        });
    }
    public void ChangeFragmentTo(int FragmentID,boolean ForceFull)
    {
        if(CurrentFragmentID!=FragmentID)
        {
            CurrentFragmentID =FragmentID;
            MainActivity.this.OnResponse(CurrentFragmentID);
        }
        else if (ForceFull==true)
        {
            CurrentFragmentID =FragmentID;
            MainActivity.this.OnResponse(CurrentFragmentID);
        }
    }
    public void ReceiveDataFromInfluxDB(CustomResponse CR,boolean UiThreadLock)
    {
        if(CurrentRefreshOption==null) return;
        String BucketName=CurrentRefreshOption.BucketName;
        String ORG=CurrentRefreshOption.OrgName;
        String SampleFrom=CurrentRefreshOption.SampleTime;
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
                    if(UiThreadLock==false) MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.OnResponse(-3);
                            CR.OnResponse(tables);
                        }
                    });
                    else CR.OnResponse(tables);
                }
                catch (Exception ex)
                {
                    Log.e("Query_Exception", ex.getMessage());
                    if(UiThreadLock==false) MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.OnResponse(-4);
                        }
                    });
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
            CurrentFragment=theFragment;
        }
        catch (Exception ex)
        {
            Log.d("Attach Fragment Failed",ex.getMessage());
        }
    }
    public void State1()
    {
        NavMenu.setVisibility(View.GONE);
    }
    public void State2()
    {
        NavMenu.setVisibility(View.VISIBLE);
    }
    public void StartLoadingEffect(ImageView srcLoadingImage)
    {
        Log.d("Loading","Start");
        srcLoadingImage.setVisibility(View.VISIBLE);
        AnimationDrawable LoadingAnimation;
        ImageView loadingImage=srcLoadingImage;
        LoadingAnimation=(AnimationDrawable) loadingImage.getDrawable();
        LoadingAnimation.start();
    }
    public void Stop_andGONE_LoadingEffect(ImageView srcLoadingImage)
    {
        Log.d("Loading","Stop");
        AnimationDrawable LoadingAnimation;
        ImageView loadingImage=srcLoadingImage;
        LoadingAnimation=(AnimationDrawable) loadingImage.getDrawable();
        LoadingAnimation.stop();
        srcLoadingImage.setVisibility(View.GONE);
    }

    @Override
    public void OnResponse(Object obj) {
        int FragmentOption=(int) obj;
        switch (FragmentOption)
        {
            case -4:
                ((Setting_Fragment)CurrentFragment).FailedNotification.setVisibility(View.VISIBLE);
                Stop_andGONE_LoadingEffect(((Setting_Fragment)CurrentFragment).LoadingEffect);
                break;
            case -3:
                ((Setting_Fragment)CurrentFragment).FailedNotification.setVisibility(View.GONE);
                Stop_andGONE_LoadingEffect(((Setting_Fragment)CurrentFragment).LoadingEffect);
                break;
            case 1:
                SetFragmentToFocus(new Login_Fragment(new CustomResponse() {
                    @Override
                    public void OnResponse(Object obj) {
                        currentClient=(InfluxDBClient) obj;
                        Log.e("TestTransferClient", "Success");
                        State2();
                        ChangeFragmentTo(4,false);
                    }
                }));
                break;
            case 2:
                SetFragmentToFocus(new Table_Fragment(CurrentTables));
                break;
            case 3:
                SetFragmentToFocus(new Graph_Fragment(CurrentTables));
                break;
            case 4:
                Setting_Fragment setting_fragment=new Setting_Fragment(currentClient, new CustomResponse() {
                    @Override
                    public void OnResponse(Object obj) {
                        if(obj==null)
                        {
                            State1();
                            ChangeFragmentTo(1,false);
                        }
                        else
                        {
                            CurrentRefreshOption=(RefreshOption) obj;
                            ReceiveDataFromInfluxDB(PostReceivedDB,false);
                        }
                    }
                });
                SetFragmentToFocus(setting_fragment);
                break;
        }

    }

    public Date ConvertTimeToGMT_Plus7(Instant instant)
    {
        Date date=Date.from(instant);
        // Convert to ZonedDateTime with UTC time zone
        ZonedDateTime zonedDateTime = date.toInstant().atZone(ZoneOffset.UTC);

        // Change the time zone to America/New_York
        zonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"));

        // Convert back to Date
        Date newDate = Date.from(zonedDateTime.toInstant());
        return newDate;
    }
}