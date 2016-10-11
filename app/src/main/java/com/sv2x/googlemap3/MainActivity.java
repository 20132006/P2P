package com.sv2x.googlemap3;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.sv2x.googlemap3.LoginAndRegister.SendPacketToMainThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;


// cjoo: we need the implementations,
//which also requires functions of onConnected, onConnectionSuspended, onConnectionFailed
public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,SendPacketToMainThread {


    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_KEY = "last-updated-time-key";
    private static boolean wifiConnected = false;   // state of WiFi
    private static boolean mobileConnected = false; // state of LTE/mobile
    public LatLng mLatLng;      // variable for (latitude, longitude)
    private double close_lat, close_lon,fLat,fLng;
    ////////////////////////written by me start/////////////////////////////

    MainActivity activity;
    String Message;

    int data_block = 100;
    OutputStreamWriter outputStreamWriter;
    FileOutputStream fileOutputStream;
    FileInputStream fileInputStream;
    InputStreamReader inputStreamReader;
    String Own_locations ="";
    String Leader_every_10_15_second_lacations = "";

    long last_leaders_send_locations_time;
    boolean amILearder=false;
    boolean first_time=true;


    boolean file_created = false;

    JSONArray array_of_points;
    JSONArray array_of_instruction;
    //HttpAsyncTask httpAsyncTask;
    String requesting_url;

    ////////////////////////written by me end/////////////////////////////

    //cjoo: services
    GoogleMap map;                      // need for google map
    UiSettings mapUi;                   // need for google map user interface
    GoogleApiClient mGoogleApiClient;   // need for google map and location service
    LocationRequest mLocationRequest;   // need for periodic location update
    com.google.android.gms.location.LocationListener mLocationListener; // need for periodic location update (provide callback)
    // cjoo: wireless
    DatagramSocket gSocket;
    Receive rxThread;
    // cjoo: user states
    private User MyState = new User();
    private Users uList = new Users();
    private String                          inputString;
    private WifiManager                     wifiManager;
    private WifiInfo                        wifiInfo;

    Vector<Double> Learder_lat = null;
    Vector<Double> Learder_lon = null;

    Vector<Double> Simulation_lat = null;
    Vector<Double> Simulation_lng = null;

    Vector<Integer> instruction_code = null;
    Vector<Integer> instruction_map = null;


    private ProvideInstructions ShowingInstruction;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String Phone_number = getIntent().getExtras().getString("arg1");
        String User_name = getIntent().getExtras().getString("arg2");
        // cjoo: initialization
        // cjoo: Get my ID & name
        getIdName(Phone_number, User_name);

        ImageButton inst_sign = (ImageButton) findViewById(R.id.image_sign);


        inst_sign.setVisibility(View.INVISIBLE);

        // restore old variables if exists
        //updateValuesFromBundle(savedInstanceState);
        // create map instance
        try {
            if (map == null) {
                map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();


                mapUi = map.getUiSettings();
            }
            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            map.setMyLocationEnabled(true);
            mapUi.setMyLocationButtonEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }


        //mHandler = new Handler();
        buildGoogleApiClient();     // create GoogleApiClient
        createLocationRequest();    // create LocationRequest
        mGoogleApiClient.connect(); // start "onConnected()" for map
        //httpAsyncTask = (HttpAsyncTask) new HttpAsyncTask(this);
        activity=this;

        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {
                // TODO Auto-generated method stub
                double lat = point.latitude;
                double lng = point.longitude;
                int i;
                double smallest_cost = -1.0;
                double cur_cost;
                Location str = new Location("Starting");
                Location end = new Location("Ending");

                Location location = new Location("Selected point");

                location.setLatitude(lat);
                location.setLongitude(lng);



                for (i = 0; i < Simulation_lat.size() - 1; i++) {
                    str.setLatitude(Simulation_lat.elementAt(i));
                    str.setLongitude(Simulation_lng.elementAt(i));

                    end.setLatitude(Simulation_lat.elementAt(i + 1));
                    end.setLongitude(Simulation_lng.elementAt(i + 1));

                    cur_cost = MatchingCost(str, end, location);
                    if (smallest_cost < 0) {
                        smallest_cost = cur_cost;
                        fLat = close_lat;
                        fLng = close_lon;
                    } else if (smallest_cost > cur_cost) {
                        smallest_cost = cur_cost;
                        fLat = close_lat;
                        fLng = close_lon;
                    }
                }
                Toast.makeText(MainActivity.this, "Selected Latitute and Logitute:" + lat + ", " + lng, Toast.LENGTH_SHORT).show();
                MarkerOptions marker = new MarkerOptions().position(new LatLng(fLat, fLng)).title("Matching selected point");

                map.addMarker(marker);
            }
        });

    }

    public double MatchingCost(Location starting, Location ending, Location loc)
    {
        double angle_A, angle_B;
        double a,b,c,h;
        double s,Area;
        double lat_dis,lon_dis;
        double c1,c2;

        lat_dis = ending.getLatitude() - starting.getLatitude();
        lon_dis = ending.getLongitude() - starting.getLongitude();

        a = ending.distanceTo(loc);
        b = starting.distanceTo(loc);
        c = starting.distanceTo(ending);

        a=Math.abs(a);
        b=Math.abs(b);
        c=Math.abs(c);

        s = (a+b+c)/2;

        Area = Math.sqrt( s * (s-a) * (s-b) * (s-c) );

        h = Area*2/c;

        angle_A = Math.toDegrees(Math.acos( ((b*b) + (c*c) - (a*a)) / (2*b*c) ));
        angle_B = Math.toDegrees(Math.acos( ((a*a) + (c*c) - (b*b)) / (2*a*c) ));

        c1 = Math.sqrt((b*b)-(h*h));
        c2 = Math.sqrt((a*a)-(h*h));

        double extra_lat,extra_lon,extra_ratio;

        if (90.0 - angle_A < 0.0)
        {
            close_lat = starting.getLatitude();
            close_lon = starting.getLongitude();
        }
        else if (90.0 - angle_B < 0.0)
        {
            close_lat = ending.getLatitude();
            close_lon = ending.getLongitude();
        }

        else
        {
            extra_ratio = c1/c;
            extra_lat = lat_dis * extra_ratio;
            extra_lon = lon_dis * extra_ratio;
            close_lat = starting.getLatitude() + extra_lat;
            close_lon = starting.getLongitude() + extra_lon;
        }

        Location matched_location = new Location("Matched");

        matched_location.setLatitude(close_lat);
        matched_location.setLongitude(close_lon);

        return matched_location.distanceTo(loc);
    }

    public void locate_all_points() throws JSONException
    {
        boolean first_pisition=true;
        LatLng last_position=null;
        for (int i = 0; i < array_of_points.length(); i++) {
            double loc_lat, loc_lng;

            loc_lat = (double) ((JSONArray) array_of_points.get(i)).get(0);
            loc_lng = (double) ((JSONArray) array_of_points.get(i)).get(1);
            LatLng marking_location = new LatLng(loc_lat, loc_lng);

            if (first_pisition == true) {
                MarkerOptions marker = new MarkerOptions().position(marking_location);
                map.addMarker(marker);
                last_position = marking_location;
                first_pisition = false;
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(marking_location, 18));
            } else {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(marking_location, 18));
                PolylineOptions line = new PolylineOptions()
                        .add(last_position)
                        .add(marking_location);
                map.addPolyline(line);
                last_position = marking_location;
            }
        }
        MarkerOptions marker = new MarkerOptions().position(last_position);

        map.addMarker(marker);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (Integer.parseInt(android.os.Build.VERSION.SDK) > 5
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 1) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void onBackPressed() {
        Log.d("CDA", "onBackPressed Called");
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Close App?")
                .setMessage("Do you want to exit?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    int until_index = 0;
    public void run_file(String file_name) {
        Simulation_lng = new Vector<>();
        Simulation_lat = new Vector<>();

        try {
            fileInputStream = openFileInput(file_name.toString());
            inputStreamReader = new InputStreamReader(fileInputStream);
            char[] data = new char[data_block];
            String final_data = "";
            int size;

            try {
                while ((size = inputStreamReader.read(data)) > 0) // it will return size of block if its no data then it will return 0
                {
                    String read_data = String.copyValueOf(data, 0 , size);
                    final_data += read_data;
                    data = new  char[data_block];
                }
                int i,index;
                LatLng last_position = null;
                boolean first_pisition = true;
                //String info = final_data;

                //String info= "35.572361,129.191254,35.572666,129.191422,35.572853,129.191467,35.572868,129.19165,35.572891,129.191788,35.572922,129.191986,35.572929,129.192123,35.572922,129.192261,35.572895,129.192383,35.572716,129.193054,35.572864,129.192501,35.572835,129.192648,35.572811,129.192785,35.572785,129.192909,35.572594,129.193436,35.572548,129.193588";
                String info= "35.573284,129.191605,35.573196,129.191574,35.573128,129.191559,35.57304,129.191528,35.572853,129.191467,35.572868,129.19165,35.572891,129.191788,35.572891,129.191788,35.572922,129.191986,35.572926,129.192047,";
                while (info.length() > 0)
                {
                    i = info.indexOf(",");
                    String cLatitude,cLongitude;
                    cLatitude = info.substring(0, i);
                    info = info.substring(i + 1, info.length());

                    i = info.indexOf(",");
                    cLongitude = info.substring(0, i);
                    info = info.substring(i+1,info.length());
                    LatLng marking_location = new LatLng(Double.parseDouble(cLatitude), Double.parseDouble(cLongitude));
                    if (first_pisition == true) {
                        MarkerOptions marker = new MarkerOptions().position(marking_location).title(info);

                        map.addMarker(marker);
                        last_position = marking_location;
                        first_pisition = false;

                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(marking_location, 18));
                    }
                    else {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(marking_location, 18));
                        PolylineOptions line = new PolylineOptions()
                                .add(last_position)
                                .add(marking_location);
                        map.addPolyline(line);
                        last_position = marking_location;
                    }
                    Simulation_lat.add(Double.parseDouble(cLatitude));
                    Simulation_lng.add(Double.parseDouble(cLongitude));
                }
                if (last_position!=null) {
                    MarkerOptions marker = new MarkerOptions().position(last_position).title(info);

                    map.addMarker(marker);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public boolean remove_file(String file_name) {
        int size =0;
        File dir = getFilesDir();//new File(path);
        File listFile[] = dir.listFiles();

        if (listFile != null && listFile.length > 0) {
            for (int i = 0; i < listFile.length; i++) {

                if (listFile[i].isDirectory()) {

                } else {

                    if (listFile[i].getName().endsWith(".txt") && file_name.equals( listFile[i].getName() ) ) {
                        return listFile[i].delete();
                    }
                }
            }
        }
        return false;
    }

    public CharSequence [] get_arrayAdapter() {

        int size =0;
        File dir = getFilesDir();//new File(path);
        File listFile[] = dir.listFiles();
        CharSequence [] filelist = new CharSequence[0];
        CharSequence [] temp = new CharSequence[listFile.length];

        if (listFile != null && listFile.length > 0) {
            for (int i = 0; i < listFile.length; i++) {

                if (listFile[i].isDirectory()) {

                } else {

                    if (listFile[i].getName().endsWith(".txt")) {
                        temp[size] = listFile[i].getName().toString();
                        size++;
                    }
                }
            }
            filelist = new CharSequence[size];
            for (int i=0; i < size;i++)
            {
                filelist[i] = temp[i];
            }
        }
        return filelist;
    }

    public boolean fileExistance(String fname) {
        File file = getBaseContext().getFileStreamPath(fname);
        return file.exists();
    }

    public void create_file() {
        getFilesDir();
        if (fileOutputStream != null)//We need to close file if we have using previouse file
        {
            close_and_save();
        }
        //show_list_of_files();
        Message = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        try {
            int num = 0;
            while (fileExistance(Message.toString() + num + ".txt")) {
                num++;
            }
            showToast(Message.toString() + num);
            fileOutputStream = openFileOutput(Message + num + ".txt", MODE_WORLD_READABLE);
            outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            Toast.makeText(getBaseContext(), "File Created", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void write_data(String data) {
        Own_locations += data;
    }

    public void close_and_save() {
        try {
            outputStreamWriter.write(Own_locations);
            outputStreamWriter.flush();
            outputStreamWriter.close();
            Toast.makeText(getBaseContext(), "Data saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // cjoo: start connection to the server
    public void startServerConnection() {
        if (checkConnection() == true) {
            MyState.isConnected = true;

            try {
                gSocket = new DatagramSocket(); // startServerConnection
            } catch (SocketException e) {
                e.printStackTrace();
            }

            sendRegisterRequest();

            rxThread = new Receive(gSocket, MyState, uList, this);
            rxThread.addListener(this);
            MyState = rxThread.getUser();
            new Thread(rxThread).start();
        } else {
            MyState.isConnected = false;
        }
    }

    // cjoo: remove the user from the server -- incomplete
    public void stopServerConnection() {
        MyState.isConnected = false;
        if (rxThread != null) {         // updateLocation thread will stop also.
            rxThread.requestStop();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //cjoo: message
        Context context = getApplicationContext();
        CharSequence text;
        Toast toast;
        if (id == R.id.file_handling) {
            FileHandling_Sharing();

        }
        else if (id == R.id.connect_server)
        {
            stopServerConnection();
            startServerConnection();
            text = MyState.myAddr.toString();
            text = "Connected: " + text.subSequence(1, text.length());
            showToast(text.toString());
            if (file_created == false)
            {
                create_file();
                file_created = true;
            }
            return true;
        }
        else if (id == R.id.create_space)
        {
            if (MyState.isConnected == true)
            {
                sendSpaceCreateRequest();
                text = "Space create requested";
                showToast(text.toString());
            }
            else
            {
                text = "No Internet Connection";
                toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                toast.show();
            }
            //////////////////////////////////
            // start send/receive your location
            //////////////////////////////////
            return true;
        }
        else if (id == R.id.join_space)
        {
            if (MyState.isConnected == true)
            {

                sendSpaceJoinRequest();
                text = "Join requested";
                showToast(text.toString());
                //stopSendingLeadersLocatons();

            }
            else
            {
                text = "No Internet Connection";
                toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                toast.show();
            }

            //////////////////////////////////
            // start send/recveive your location
            //////////////////////////////////
            return true;
        }
        else if (id == R.id.leave_space)
        {
            if (MyState.isConnected == true) {


                if (MyState.mySpaceId=="")
                {
                    text = "You are not joined any space";
                    showToast(text.toString());
                }
                else
                {
                    sendLeaveSpaceRequest();
                    text = "Leave space requested";
                    showToast(text.toString());
                    //stopSendingLeadersLocatons();
                }

            } else {
                text = "No Internet Connection";
                toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                toast.show();
            }
        }

        else if (id == R.id.quit) {
            if (file_created == true) {
                close_and_save();
            }
            //stopSendingLeadersLocatons();
            this.MyState.isLeader=false;
            this.MyState.isConnected = false;

            System.exit(0);
            return true;
        }
        else if (id == R.id.debug) {
            toast = Toast.makeText(context, MyState.debugMsg, Toast.LENGTH_SHORT);
            toast.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //cjoo: Remove the LocationUpdate service
    //      without this, the app keeps updating the location information,
    //      even if it is killed by the user.
    @Override
    protected void onStop() {
        super.onStop();
        stopLocationUpdate();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // stopLoationUpate()   // we want to keep updating the location in background
    }

    // cjoo: The following is necessary, to restart Location service after Pause (or background)
    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && MyState.mRequestLocationUpdates == false) {
            startLocationUpdates();
        }
    }

    // cjoo: to save the states
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, MyState.mRequestLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, MyState.mLastLocation);
        savedInstanceState.putLong(LAST_UPDATED_TIME_KEY, MyState.mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    ///cjoo

    @Override
    public void onConnected(Bundle bundle) {
        MyState.mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (MyState.mLastLocation != null)
        {
            mLatLng = new LatLng(MyState.mLastLocation.getLatitude(), MyState.mLastLocation.getLongitude());
            //cjoo: the following shows the location (latitude) info. on screen;
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, MyState.mCurrentCameraLevel));
            //CameraUpdateFactory.newLatLng(mLatLng);
        }

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        //
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //
    }


    // cjoo: Need to use map and location services
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
    // cjoo: periodic location updates
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // cjoo: the following is different from previous LocationListener
        mLocationListener = new com.google.android.gms.location.LocationListener()
        {
            @Override
            public void onLocationChanged(Location location)
            {
                // cjoo: this is where we can specify what we want to do
                //       when we update the location info.

                if (MyState.mLastLocation != null)
                {
                    //initCamera(location,bearing(MyState.mLastLocation.getLatitude(), MyState.mLastLocation.getLongitude(),location.getLatitude(),location.getLongitude()));
                }

                Location previous_location = MyState.mLastLocation;
                long previous_update = MyState.mLastUpdateTime;



                showInstruction(location);

                mLatLng = new LatLng(MyState.mLastLocation.getLatitude(), MyState.mLastLocation.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, MyState.mCurrentCameraLevel));

                if ( outputStreamWriter != null )
                {
                    write_data(MyState.mLastLocation.getLatitude() + ";" + MyState.mLastLocation.getLongitude() + ";");
                }


                if (amILearder && (MyState.mLastUpdateTime - last_leaders_send_locations_time) >= 15 )
                {
                    sendLeadersLastLocations(Leader_every_10_15_second_lacations);
                    last_leaders_send_locations_time = MyState.mLastUpdateTime;
                    Leader_every_10_15_second_lacations = previous_location.getLatitude() + ";" + previous_location.getLongitude() + ";" + previous_update + ";";
                }

                if (amILearder)
                {
                    Leader_every_10_15_second_lacations += location.getLatitude() + ";" + location.getLongitude() + ";" + MyState.mLastUpdateTime + ";";
                }
            }
        };
    }

    public void showInstruction(Location location) {
        MyState.mLastLocation = location;
        MyState.mLastUpdateTime = System.currentTimeMillis()/1000;
        EditText distanceBTW = (EditText) findViewById(R.id.distance);

        if (!MyState.isLeader && MyState.mySpaceId!="")
        {
            ImageButton inst_sign = (ImageButton) findViewById(R.id.image_sign);

            //location.setLatitude(35.57287);
            //location.setLongitude(129.191483);

            String instruction = ShowingInstruction.QueryInstructions(Learder_lat,Learder_lon,instruction_code,instruction_map,location);
            String distance = null;

            if (instruction != null) {

                if (instruction.indexOf(",") >= 0) {
                    distance = instruction.substring(instruction.indexOf(",") + 1);
                }

                if (instruction.indexOf("Left") >= 0) {
                    inst_sign.setBackgroundResource(R.drawable.turnleft);
                    inst_sign.setVisibility(View.VISIBLE);
                    distanceBTW.setText(distance);
                } else if (instruction.indexOf("Right") >= 0) {
                    inst_sign.setBackgroundResource(R.drawable.turnright);
                    inst_sign.setVisibility(View.VISIBLE);
                    distanceBTW.setText(distance);
                } else if (instruction.indexOf("GoStraight") >= 0) {
                    inst_sign.setBackgroundResource(R.drawable.gostraight);
                    inst_sign.setVisibility(View.VISIBLE);
                    distanceBTW.setText(distance);
                } else if (instruction.indexOf("RoundAbout") >= 0) {
                    inst_sign.setBackgroundResource(R.drawable.roundaboutsign);
                    inst_sign.setVisibility(View.VISIBLE);
                    distanceBTW.setText(distance);
                } else if (instruction.indexOf("UTurn") >= 0) {
                    inst_sign.setBackgroundResource(R.drawable.u_turn);
                    inst_sign.setVisibility(View.VISIBLE);
                    distanceBTW.setText(distance);
                } else if (instruction.indexOf("ReachViaLocation") >= 0) {
                    inst_sign.setBackgroundResource(R.drawable.reach_via_location);
                    inst_sign.setVisibility(View.VISIBLE);
                    distanceBTW.setText(distance);
                } else if (instruction.indexOf("NoTurn") >= 0) {
                    inst_sign.setBackgroundResource(R.drawable.no_turn);
                    inst_sign.setVisibility(View.VISIBLE);
                    distanceBTW.setText(distance);
                } else if (instruction.indexOf("HeadOn") >= 0) {
                    inst_sign.setBackgroundResource(R.drawable.head_on);
                    inst_sign.setVisibility(View.VISIBLE);
                    distanceBTW.setText(distance);
                } else {
                    inst_sign.setVisibility(View.INVISIBLE);
                    distanceBTW.setText("No Instruction");
                }
            }

        }
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, mLocationListener);
        MyState.mRequestLocationUpdates = true;
    }

    protected void stopLocationUpdate() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, mLocationListener);
        MyState.mRequestLocationUpdates = false;
    }

    // cjoo: to restore previous value
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null)
        {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY))
            {
                MyState.mRequestLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
            }
            if (savedInstanceState.keySet().contains(LOCATION_KEY))
            {
                MyState.mLastLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_KEY))
            {
                MyState.mLastUpdateTime = savedInstanceState.getLong(LAST_UPDATED_TIME_KEY);
            }
        }
    }


    // cjoo: check connectivity
    //      (Soon, we will not support Wi-Fi...)
    private boolean checkConnection() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected())
        {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            MyState.getLocalIpAddress();
            return true;
        }
        else
        {
            return false;
        }
    }

    // get Id & name
    public void getIdName(String ID, String NAME) {
        // cjoo: initial user input
        MyState.name = NAME;
        MyState.id = ID;
    }

    // register new user!
    private void sendRegisterRequest() {
        final CharSequence text;
        text = String.valueOf(MyState.id) + ";" + "New User" + ";";
        MyState.send(gSocket, text.toString());
    }

    public void sendSpaceJoinRequest() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Space name");
        alert.setMessage("Type the space name to join");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton)
            {
                ShowingInstruction = new ProvideInstructions(getBaseContext());

                Learder_lat = new Vector<>();
                Learder_lon = new Vector<>();
                instruction_code = new Vector<>();
                instruction_map = new Vector<>();

                //temp_First("{\"status\":200,\"status_message\":\"Found matchings\",\"matchings\":[{\"matched_names\":[\"\",\"\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\"],\"matched_points\":[[35.573284,129.191605],[35.573128,129.191559],[35.572891,129.191788],[35.572926,129.192047]],\"route_summary\":{\"total_time\":16,\"total_distance\":103},\"indices\":[0,1,2,3],\"instructions\":[[\"10\",\"\",18,0,1,\"18m\",\"S\",196,1,\"N\",16],[\"9\",\"\",32,2,10,\"31m\",\"S\",196,1,\"N\",16],[\"7\",\"유니스트길 (UNIST-gil)\",29,4,2,\"28m\",\"E\",83,1,\"W\",263],[\"9\",\"유니스트길 (UNIST-gil)\",24,6,1,\"23m\",\"N\",0,1,\"N\",0],[\"15\",\"\",0,9,0,\"0m\",\"N\",0,\"N\",0]],\"geometry\":[[35.573284,129.191605],[35.573196,129.191574],[35.573128,129.191559],[35.57304,129.191528],[35.572853,129.191467],[35.572868,129.19165],[35.572891,129.191788],[35.572891,129.191788],[35.572922,129.191986],[35.572926,129.192047]],\"hint_data\":{\"locations\":[\"_____w4aBwAAAAAADAAAAAMAAAAuAAAAFAAAAJZGBABOAQAAI84eAq1OswcCAAEB\",\"_____w4aBwAAAAAACwAAAAkAAAAZAAAAJAAAAJZGBABOAQAAhs0eAoFOswcBAAEB\",\"ABoHAP____8KHQAAFAAAABQAAAAZAAAAygEAAE9HBABOAQAAm8weAnJPswcBAAEB\",\"ABoHAP____8KHQAACAAAABMAAABLAAAAmQEAAE9HBABOAQAAvcweAnFQswcDAAEB\"],\"checksum\":1726661290}}]}\n");

                MyState.isLeader = false;
                String value = input.getText().toString();
                MyState.mySpaceId = value;
                final CharSequence text;
                text = String.valueOf(MyState.id) + ";"
                        + getString(R.string.join_space) + ";"
                        + value + ";";
                MyState.send(gSocket, text.toString());
            }

        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    public void sendLeaveSpaceRequest() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Do you want from space?");

        // Set an EditText view to get user input
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton)
            {


                final CharSequence text;
                text = String.valueOf(MyState.id) + ";"
                        + getString(R.string.leave_space) + ";"
                        + String.valueOf(MyState.mySpaceId) + ";";

                Init_space_info();

                MyState.send(gSocket, text.toString());
            }

        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    void Init_space_info() {
        MyState.isLeader = false;
        MyState.mLeadersLastLatLng = null;
        MyState.mySpaceId="";
        MyState.activeToUseLeadersLocations=false;
        MyState.firstLeadersMark=false;
        MyState.mLeardersLastUpdateTime=0;
        map.clear();
    }
    // request Space Creation to the server
    private void sendSpaceCreateRequest() {
        final SpaceRegisterDialog dialog = new SpaceRegisterDialog();

        final Thread first = new Thread(new Runnable() {
            @Override
            public void run() {
                dialog.show(getSupportFragmentManager(),"PopUpDialog");
            }
        });

        MyState.isLeader=true;
        first.start();

        Thread second = new Thread(new Runnable()
        {
            public void run()
            {
                if (first.isAlive())
                {
                    try {
                        first.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                while (true)
                {
                    if (dialog.get_decision() == 1)
                    {
                        System.out.println("Unknown message type");
                        showToast("Pressed OK");
                        //Toast.makeText(getApplicationContext(), "Pressed OK", Toast.LENGTH_LONG).show();
                        String sending_location_type = "Create Space Matched";

                        if (!dialog.whichOneIsChecked())
                            sending_location_type = "Create Space Actual";

                        amILearder = true;
                        MyState.isLeader=true;
                        last_leaders_send_locations_time = System.currentTimeMillis() / 1000;

                        MyState.mySpaceId = dialog.get_name();

                        final CharSequence text;
                        text = String.valueOf(MyState.id) + ";" + sending_location_type + ";"
                                + String.valueOf(MyState.mySpaceId) + ";"; // currently, space id = owner's user name
                        MyState.send(gSocket, text.toString());
                        break;
                    }
                    else if (dialog.get_decision() == 2 || dialog.get_decision() == 3)
                    {
                        MyState.isLeader=false;
                        showToast("Pressed CANCEL");
                        break;
                    }
                    try {
                        Thread.sleep( 1000 );
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
        second.start();

    }

    /**************************************************************************************************************************/
    /**************************************************Sending Leaders Locations***********************************************/
    /**************************************************************************************************************************/


    private void sendLeadersLastLocations(String Locations) {

        final CharSequence text;
        text = String.valueOf(MyState.id) + ";" + "Leader's Locations" + ";"
                + String.valueOf(MyState.id) + ";" + Locations ;
        MyState.send(gSocket, text.toString());

    }

    /**************************************************************************************************************************/
    /**************************************************Handling Files**********************************************************/
    /**************************************************************************************************************************/


    private void FileHandling_Sharing() {
        final CharSequence List_of_files[] = get_arrayAdapter();
        final CharSequence List_of_handling[] = {"Email","Show up in map","SMS", "Edit","Remove","Share"};

        AlertDialog.Builder builder_listOFfiles = new AlertDialog.Builder(this);
        builder_listOFfiles.setTitle("Pick a File");
        builder_listOFfiles.setItems(List_of_files, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, final int which) {
                //run_file(List_of_files[which].toString());

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Pick a File");
                builder.setItems(List_of_handling, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which_file) {

                        //run_file(List_of_files[which].toString());
                        switch (which_file) {
                            case 0:
                                break;
                            case 1:
                                run_file(List_of_files[which].toString());
                                break;
                            case 2:
                                break;
                            case 3:
                                break;
                            case 4:
                                if (remove_file(List_of_files[which].toString())){
                                    Context context = getApplicationContext();
                                    CharSequence text = "File removed.";
                                    int duration = Toast.LENGTH_SHORT;

                                    Toast toast = Toast.makeText(context, text, duration);
                                    toast.show();
                                }else {
                                    Context context = getApplicationContext();
                                    CharSequence text = "You can't remove this file.";
                                    int duration = Toast.LENGTH_SHORT;

                                    Toast toast = Toast.makeText(context, text, duration);
                                    toast.show();
                                }
                                break;
                            case 5:
                                if (remove_file(List_of_files[which].toString())){
                                    Context context = getApplicationContext();
                                    CharSequence text = "File removed.";
                                    int duration = Toast.LENGTH_SHORT;

                                    Toast toast = Toast.makeText(context, text, duration);
                                    toast.show();
                                }else {
                                    Context context = getApplicationContext();
                                    CharSequence text = "You can't remove this file.";
                                    int duration = Toast.LENGTH_SHORT;

                                    Toast toast = Toast.makeText(context, text, duration);
                                    toast.show();
                                }
                                break;
                        }
                    }
                });
                builder.show();
            }
        });
        builder_listOFfiles.show();
    }

    public void showToast(final String toast) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void showMessage(String title, String Message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(Message);
        builder.show();
    }

    public void temp_First(String message) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = null;
        if (jsonObject!=null)
        {
            try {
                jsonArray = (JSONArray) jsonObject.get(/*"matched_points"*/ "matchings");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        JSONObject jsonObject2 = null;
        if (jsonArray!=null)
        {
            try {
                jsonObject2 = (JSONObject) jsonArray.get(/*"matched_points"*/ 0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        array_of_points = null;
        if (jsonObject2!=null)
        {
            try {
                array_of_points = (JSONArray) jsonObject2.get("geometry");
                array_of_instruction = (JSONArray) jsonObject2.get("instructions");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        JSONArray instructionOnIndex = null;
        JSONArray latlon = null;
        LatLng latLng = null;
        int old_len;
        if (Learder_lat!=null)
        {
            old_len = Learder_lat.size();
        }
        else
        {
            old_len = 0;
        }
        for (int i=0;i<array_of_points.length();i++)
        {

            try {
                latlon = (JSONArray) array_of_points.get(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                Learder_lat.add((double) latlon.get(0));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                Learder_lon.add((double) latlon.get(1));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        for (int i=0;i<array_of_points.length();i++)
        {

            try {
                latlon = (JSONArray) array_of_points.get(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                Learder_lat.add((double) latlon.get(0));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                Learder_lon.add((double) latlon.get(1));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        for (int i=0;i<array_of_instruction.length();i++)
        {
            int code_inst = 0,mapped_instructions = 0;
            try {
                instructionOnIndex = (JSONArray) array_of_instruction.get(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                mapped_instructions = (int) instructionOnIndex.get(3);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                code_inst = Integer.parseInt(instructionOnIndex.get(0).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            instruction_code.add(code_inst);
            instruction_map.add(old_len + mapped_instructions);

        }
    }

    @Override
    public void onTextParsed(JSONArray array_of_point, JSONArray array_of_instruc) {
        JSONArray instructionOnIndex = null;
        JSONArray latlon = null;
        LatLng latLng = null;
        int old_len = Learder_lat.size();
        for (int i=0;i<array_of_point.length();i++)
        {

            try {
                latlon = (JSONArray) array_of_point.get(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                Learder_lat.add((double) latlon.get(0));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                Learder_lon.add((double) latlon.get(1));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        for (int i=0;i<array_of_instruc.length();i++)
        {
            int code_inst = 0,mapped_instructions = 0;
            try {
                instructionOnIndex = (JSONArray) array_of_instruc.get(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                mapped_instructions = (int) instructionOnIndex.get(3);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                code_inst = Integer.parseInt(instructionOnIndex.get(0).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            instruction_code.add(code_inst);
            instruction_map.add(old_len + mapped_instructions);

        }
    }
}