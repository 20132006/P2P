package com.sv2x.googlemap3;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

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
import java.util.LinkedList;


// cjoo: we need the implementations,
//which also requires functions of onConnected, onConnectionSuspended, onConnectionFailed
public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    int inpor = 0;

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_KEY = "last-updated-time-key";
    private static boolean wifiConnected = false;   // state of WiFi
    private static boolean mobileConnected = false; // state of LTE/mobile
    public LatLng mLatLng;      // variable for (latitude, longitude)
    ////////////////////////written by me start/////////////////////////////\
    ListView listView;
    MainActivity activity;
    boolean place_points=false;
    boolean geometry=false;
    boolean nearest=false;
    String Message;

    int data_block = 100;
    OutputStreamWriter outputStreamWriter;
    FileOutputStream fileOutputStream;
    FileInputStream fileInputStream;
    InputStreamReader inputStreamReader;
    String Own_locations;
    String Leader_every_10_15_second_lacations = "";

    long last_leaders_send_locations_time;
    boolean amILearder=false;

    boolean file_created = false;

    JSONArray array_of_points;
    JSONArray nearest_point;
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


    private int interval_send_leaders_locations = 15000; // 15 seconds by default, can be changed later
    private Handler mHandler;
    private ProvideInstructions ShowingInstruction;


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String Phone_number = getIntent().getExtras().getString("arg1");
        String User_name = getIntent().getExtras().getString("arg2");
        // cjoo: initialization
        // cjoo: Get my ID & name
        getIdName(Phone_number,User_name);

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


    public void run_file(String file_name)
    {
        try {
            fileInputStream = openFileInput(file_name.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        inputStreamReader = new InputStreamReader(fileInputStream);
        char[] data = new char[data_block];
        String final_data = "";
        requesting_url = "http://10.20.17.173:5000/match?";
        int size;

        try {
            while ((size = inputStreamReader.read(data)) > 0) // it will return size of block if its no data then it will return 0
            {
                String read_data = String.copyValueOf(data, 0 , size);
                final_data += read_data;
                data = new  char[data_block];
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        int i,index;
        String info = null;
        while (final_data.length() > 0) {
            index = final_data.indexOf("\n");
            info = final_data.substring(0, index + 1);
            final_data = final_data.substring(index + 1, final_data.length());

            String cLatitude,cLongitude,ctime;

            i = info.indexOf(";");
            cLatitude = info.substring(0, i);
            info = info.substring(i + 1, info.length());

            i = info.indexOf(";");
            cLongitude = info.substring(0, i);
            info = info.substring(i + 1, info.length());

            i = info.indexOf(";");
            ctime = info.substring(0, i);
            info = info.substring(i + 1, info.length());

            requesting_url +="loc="+cLatitude+","+cLongitude+"&t="+ctime+"&"+"instructions=true&compression=false";
        }
    }


    public boolean remove_file(String file_name)
    {
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

    public void locate_all_points() throws JSONException {
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
        else if (id == R.id.connect_server) {
            stopServerConnection();
            startServerConnection();
            text = MyState.myAddr.toString();
            text = "Connected: " + text.subSequence(1, text.length());
            showToast(text.toString());
            if (file_created == false) {
                create_file();
                file_created = true;
            }
            return true;
        }
        else if (id == R.id.create_space) {
            if (MyState.isConnected == true) {
                sendSpaceCreateRequest();
                text = "Space create requested";
                showToast(text.toString());
            } else {
                text = "No Internet Connection";
                toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                toast.show();
            }
            //////////////////////////////////
            // start send/receive your location
            //////////////////////////////////
            return true;
        }
        else if (id == R.id.join_space) {
            if (MyState.isConnected == true) {

                sendSpaceJoinRequest();
                text = "Join requested";
                showToast(text.toString());
                //stopSendingLeadersLocatons();

            } else {
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
        if (MyState.mLastLocation != null) {
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
        mLocationListener = new com.google.android.gms.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // cjoo: this is where we can specify what we want to do
                //       when we update the location info.
                MyState.mLastLocation = location;
                MyState.mLastUpdateTime = System.currentTimeMillis()/1000;



                /*
                String Data1 = "{\"status\":200,\"status_message\":\"Found matchings\",\"matchings\":[{\"matched_names\":[\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\"],\"matched_points\":[[35.572816,129.193406],[35.57286,129.193269],[35.572904,129.193094],[35.572949,129.192918],[35.572998,129.192745],[35.573048,129.192571],[35.573085,129.192433],[35.573103,129.192365]],\"hint_data\":{\"locations\":[\"TVPJCP____-U604AEgAAACcAAADfAAAAAgEAADQ9nQayPZ0Gm7IAAFDMHgK-VbMHBwABAQ\",\"TVPJCP____-U604AJwAAACcAAADfAAAAAgEAADQ9nQayPZ0Gm7IAAHzMHgI1VbMHBwABAQ\",\"TVPJCP____-U604AGQAAADUAAAAGAQAAzQAAADQ9nQayPZ0Gm7IAAKjMHgKGVLMHCAABAQ\",\"TVPJCP____-U604AMwAAADUAAAAGAQAAzQAAADQ9nQayPZ0Gm7IAANXMHgLWU7MHCAABAQ\",\"TVPJCP____-U604AGAAAADcAAAA7AQAAlgAAADQ9nQayPZ0Gm7IAAAbNHgIpU7MHCQABAQ\",\"TVPJCP____-U604AMgAAADcAAAA7AQAAlgAAADQ9nQayPZ0Gm7IAADjNHgJ7UrMHCQABAQ\",\"TVPJCP____-U604ADwAAACIAAAByAQAAdAAAADQ9nQayPZ0Gm7IAAF3NHgLxUbMHCgABAQ\",\"TVPJCP____-U604AGQAAACIAAAByAQAAdAAAADQ9nQayPZ0Gm7IAAG_NHgKtUbMHCgABAQ\"],\"checksum\":476095336},\"geometry\":[[35.572816,129.193406],[35.57286,129.193269],[35.57286,129.193269],[35.572904,129.193094],[35.572949,129.192918],[35.572952,129.19291],[35.572998,129.192745],[35.573048,129.192571],[35.573057,129.19254],[35.573085,129.192433],[35.573103,129.192365]],\"indices\":[0,1,2,3,4,5,6,7],\"instructions\":[[\"10\",\"유니스트길 (UNIST-gil)\",13,0,4,\"13m\",\"W\",292,1,\"E\",112],[\"9\",\"유니스트길 (UNIST-gil)\",17,1,3,\"16m\",\"N\",0,1,\"N\",0],[\"9\",\"유니스트길 (UNIST-gil)\",17,3,5,\"16m\",\"W\",287,1,\"E\",107],[\"9\",\"유니스트길 (UNIST-gil)\",17,4,2,\"16m\",\"NW\",295,1,\"SE\",115],[\"9\",\"유니스트길 (UNIST-gil)\",17,6,5,\"16m\",\"W\",289,1,\"E\",109],[\"9\",\"유니스트길 (UNIST-gil)\",13,7,2,\"13m\",\"W\",290,1,\"E\",110],[\"9\",\"유니스트길 (UNIST-gil)\",6,9,3,\"6m\",\"W\",288,1,\"E\",108],[\"15\",\"\",0,10,0,\"0m\",\"N\",0,1,\"N\",0]],\"route_summary\":{\"total_time\":15,\"total_distance\":99}}],\"debug\":{\"breakage\":[0,0,0,0,0,0,0,0],\"states\":[[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-2.657298,-2.658607,-1.668095,13.32546,13.618747],\"to\":[1,0]}],\"pruned\":0,\"coordinate\":[35.572816,129.193406],\"viterbi\":-2.657298}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-6.984,-2.684153,-1.61324,16.571193,16.552181],\"to\":[2,0]}],\"pruned\":0,\"coordinate\":[35.57286,129.193269],\"viterbi\":-6.984}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-11.281393,-2.699387,-1.619922,16.690656,16.638234],\"to\":[3,0]}],\"pruned\":0,\"coordinate\":[35.572904,129.193094],\"viterbi\":-11.281393}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-15.600702,-2.6645,-1.631568,16.576798,16.46615],\"to\":[4,0]}],\"pruned\":0,\"coordinate\":[35.572949,129.192918],\"viterbi\":-15.600702}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-19.896769,-2.619019,-1.615294,16.695046,16.724325],\"to\":[5,0]}],\"pruned\":0,\"coordinate\":[35.572998,129.192745],\"viterbi\":-19.896769}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-24.131082,-3.111022,-1.695092,13.146613,13.574886],\"to\":[6,0]}],\"pruned\":0,\"coordinate\":[35.573048,129.192571],\"viterbi\":-24.131082}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-28.937196,-5.784948,-2.278213,6.469446,9.813321],\"to\":[7,0]}],\"pruned\":0,\"coordinate\":[35.573085,129.192433],\"viterbi\":-28.937196}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.573103,129.192365],\"viterbi\":-37.000358}]]}}";
                String Data2 = "{\"status\":200,\"status_message\":\"Found matchings\",\"matchings\":[{\"matched_names\":[\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\"],\"matched_points\":[[35.572238,129.194929],[35.57232,129.194714],[35.5724,129.194542],[35.572443,129.194437],[35.572486,129.194328],[35.572539,129.194193],[35.572593,129.194054],[35.572648,129.193898],[35.572704,129.193741],[35.572778,129.193526]],\"hint_data\":{\"locations\":[\"TVPJCP____-U604AAgAAABMAAAAAAAAA9QEAADQ9nQayPZ0Gm7IAAA7KHgKxW7MHAAABAQ\",\"TVPJCP____-U604AEAAAABQAAAATAAAA4QEAADQ9nQayPZ0Gm7IAAGDKHgLaWrMHAQABAQ\",\"TVPJCP____-U604AGQAAAB0AAAAnAAAAxAEAADQ9nQayPZ0Gm7IAALDKHgIuWrMHAgABAQ\",\"TVPJCP____-U604ADAAAACAAAABEAAAApAEAADQ9nQayPZ0Gm7IAANvKHgLFWbMHAwABAQ\",\"TVPJCP____-U604AHQAAACAAAABEAAAApAEAADQ9nQayPZ0Gm7IAAAbLHgJYWbMHAwABAQ\",\"TVPJCP____-U604AEwAAACcAAABkAAAAfQEAADQ9nQayPZ0Gm7IAADvLHgLRWLMHBAABAQ\",\"TVPJCP____-U604AAgAAACkAAACLAAAAVAEAADQ9nQayPZ0Gm7IAAHHLHgJGWLMHBQABAQ\",\"TVPJCP____-U604AGgAAACkAAACLAAAAVAEAADQ9nQayPZ0Gm7IAAKjLHgKqV7MHBQABAQ\",\"TVPJCP____-U604ACQAAACsAAAC0AAAAKQEAADQ9nQayPZ0Gm7IAAODLHgINV7MHBgABAQ\",\"TVPJCP____-U604AAAAAACcAAADfAAAAAgEAADQ9nQayPZ0Gm7IAACrMHgI2VrMHBwABAQ\"],\"checksum\":476095336},\"geometry\":[[35.572238,129.194929],[35.572276,129.194819],[35.57232,129.194714],[35.572328,129.194696],[35.5724,129.194542],[35.572411,129.194519],[35.572443,129.194437],[35.572486,129.194328],[35.572492,129.194315],[35.572539,129.194193],[35.572588,129.194069],[35.572593,129.194054],[35.572648,129.193898],[35.572682,129.193806],[35.572704,129.193741],[35.572778,129.193526]],\"indices\":[0,1,2,3,4,5,6,7,8,9],\"instructions\":[[\"10\",\"유니스트길 (UNIST-gil)\",21,0,2,\"21m\",\"NW\",293,1,\"SE\",113],[\"9\",\"유니스트길 (UNIST-gil)\",18,2,3,\"17m\",\"NW\",299,1,\"SE\",119],[\"9\",\"유니스트길 (UNIST-gil)\",11,4,1,\"10m\",\"NW\",300,1,\"SE\",120],[\"9\",\"유니스트길 (UNIST-gil)\",11,6,3,\"10m\",\"NW\",296,1,\"SE\",116],[\"9\",\"유니스트길 (UNIST-gil)\",14,7,2,\"13m\",\"NW\",300,1,\"SE\",120],[\"9\",\"유니스트길 (UNIST-gil)\",14,9,0,\"13m\",\"NW\",296,1,\"SE\",116],[\"9\",\"유니스트길 (UNIST-gil)\",15,11,3,\"15m\",\"NW\",293,1,\"SE\",113],[\"9\",\"유니스트길 (UNIST-gil)\",16,12,1,\"15m\",\"NW\",294,1,\"SE\",114],[\"9\",\"유니스트길 (UNIST-gil)\",21,14,0,\"21m\",\"NW\",293,1,\"SE\",113],[\"15\",\"\",0,15,0,\"0m\",\"N\",0,1,\"N\",0]],\"route_summary\":{\"total_time\":22,\"total_distance\":141}}],\"debug\":{\"breakage\":[0,0,0,0,0,0,0,0,0,0],\"states\":[[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-2.740834,-2.583451,-1.650626,21.497857,21.291915],\"to\":[1,0]}],\"pruned\":0,\"coordinate\":[35.572238,129.194929],\"viterbi\":-2.740834},{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.572234,129.194944],\"viterbi\":-2.766316},{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.572234,129.194944],\"viterbi\":-2.766316},{\"suspicious\":0,\"transitions\":[{\"properties\":[-2.766316,-2.583451,-1.935342,22.921434,21.291915],\"to\":[1,0]}],\"pruned\":0,\"coordinate\":[35.572234,129.194944],\"viterbi\":-2.766316},{\"suspicious\":0,\"transitions\":[{\"properties\":[-2.766316,-2.583451,-1.935342,22.921434,21.291915],\"to\":[1,0]}],\"pruned\":0,\"coordinate\":[35.572234,129.194944],\"viterbi\":-2.766316}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-6.974911,-2.537181,-1.613765,17.92559,17.903956],\"to\":[2,0]}],\"pruned\":0,\"coordinate\":[35.57232,129.194714],\"viterbi\":-6.974911}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-11.125857,-2.608087,-1.637827,10.641973,10.78392],\"to\":[3,0]}],\"pruned\":0,\"coordinate\":[35.5724,129.194542],\"viterbi\":-11.125857}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-15.371771,-2.769666,-1.631768,10.959784,11.071434],\"to\":[4,0]}],\"pruned\":0,\"coordinate\":[35.572443,129.194437],\"viterbi\":-15.371771}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-19.773205,-2.879878,-1.621704,13.564907,13.626235],\"to\":[5,0]}],\"pruned\":0,\"coordinate\":[35.572486,129.194328],\"viterbi\":-19.773205}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-24.274786,-2.914518,-1.628373,13.93858,14.033256],\"to\":[6,0]}],\"pruned\":0,\"coordinate\":[35.572539,129.194193],\"viterbi\":-24.274786}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-28.817677,-2.957848,-1.610904,15.381972,15.389303],\"to\":[7,0]}],\"pruned\":0,\"coordinate\":[35.572593,129.194054],\"viterbi\":-28.817677}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-33.386429,-2.9769,-1.618049,15.511286,15.554342],\"to\":[8,0]}],\"pruned\":0,\"coordinate\":[35.572648,129.193898],\"viterbi\":-33.386429}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-37.981378,-2.597306,-1.640544,21.120607,21.276139],\"to\":[9,0]}],\"pruned\":0,\"coordinate\":[35.572704,129.193741],\"viterbi\":-37.981378}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.572778,129.193526],\"viterbi\":-42.219229}]]}}";
                String Data3 = "{\"status\":200,\"status_message\":\"Found matchings\",\"matchings\":[{\"matched_names\":[\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"\",\"\",\"\",\"\",\"\",\"\"],\"matched_points\":[[35.572222,129.194445],[35.57218,129.194542],[35.572134,129.194641],[35.572085,129.194737],[35.572019,129.194822],[35.57195,129.194926],[35.571954,129.195046],[35.572003,129.195124],[35.572088,129.19516],[35.572184,129.195118]],\"hint_data\":{\"locations\":[\"YlPJCP____-U604AIQAAACsAAACbAQAAMQAAAN09nQY4PZ0Gm7IAAP7JHgLNWbMHCgABAQ\",\"YlPJCP____-U604ABQAAABoAAADGAQAAFwAAAN09nQY4PZ0Gm7IAANTJHgIuWrMHCwABAQ\",\"YlPJCP____-U604AFQAAABoAAADGAQAAFwAAAN09nQY4PZ0Gm7IAAKbJHgKRWrMHCwABAQ\",\"YlPJCP____-U604ACwAAABcAAADgAQAAAAAAAN09nQY4PZ0Gm7IAAHXJHgLxWrMHDAABAQ\",\"_____39TyQgAAAAAAQAAAAMAAAAFAAAAAAAAAM49nQYqPZ0Gm7IAADPJHgJGW7MHAQABAQ\",\"_____31TyQgAAAAAAQAAAAQAAAASAAAAAAAAANc9nQa6PZ0Gm7IAAO7IHgKuW7MHBAABAQ\",\"_____31TyQgAAAAAAQAAAAMAAAAJAAAACgAAANc9nQa6PZ0Gm7IAAPLIHgImXLMHAgABAQ\",\"_____31TyQgAAAAAAgAAAAEAAAAAAAAAFAAAANc9nQa6PZ0Gm7IAACPJHgJ0XLMHAAABAQ\",\"_____3pTyQgAAAAAAAAAAAYAAAAYAAAAAAAAAKI9nQaePZ0Gm7IAAHjJHgKYXLMHBQABAQ\",\"_____3pTyQgAAAAABgAAAAAAAAAJAAAADwAAAKI9nQaePZ0Gm7IAANjJHgJuXLMHAgABAQ\"],\"checksum\":476095336},\"geometry\":[[35.572222,129.194445],[35.572197,129.194508],[35.57218,129.194542],[35.572134,129.194641],[35.572121,129.194669],[35.572085,129.194737],[35.572051,129.194804],[35.572019,129.194822],[35.572005,129.194831],[35.571968,129.194876],[35.57195,129.194926],[35.571944,129.194947],[35.571943,129.195003],[35.571954,129.195046],[35.571959,129.195064],[35.571994,129.195117],[35.572003,129.195124],[35.57203,129.195145],[35.572088,129.19516],[35.572088,129.19516],[35.572134,129.195152],[35.572184,129.195118]],\"indices\":[0,1,2,3,4,5,6,7,8,9],\"instructions\":[[\"10\",\"유니스트길 (UNIST-gil)\",10,0,1,\"9m\",\"SE\",116,1,\"NW\",296],[\"9\",\"유니스트길 (UNIST-gil)\",10,2,2,\"10m\",\"SE\",120,1,\"NW\",300],[\"9\",\"유니스트길 (UNIST-gil)\",10,3,1,\"10m\",\"SE\",120,1,\"NW\",300],[\"9\",\"유니스트길 (UNIST-gil)\",7,5,2,\"7m\",\"SE\",122,1,\"NW\",302],[\"1\",\"\",4,6,0,\"3m\",\"SE\",155,1,\"NW\",335],[\"9\",\"\",8,7,1,\"7m\",\"SE\",152,1,\"NW\",332],[\"7\",\"\",5,9,0,\"4m\",\"SE\",114,1,\"NW\",294],[\"9\",\"\",11,10,0,\"11m\",\"E\",109,1,\"W\",289],[\"9\",\"\",9,13,0,\"9m\",\"E\",71,1,\"W\",251],[\"9\",\"\",4,16,1,\"3m\",\"NE\",32,1,\"SW\",212],[\"8\",\"\",7,17,1,\"6m\",\"N\",12,1,\"S\",192],[\"9\",\"\",12,18,0,\"11m\",\"N\",0,1,\"N\",0],[\"15\",\"\",0,21,0,\"0m\",\"N\",0,1,\"N\",0]],\"route_summary\":{\"total_time\":11,\"total_distance\":96}}],\"debug\":{\"breakage\":[0,0,0,0,0,0,0,0,0,0],\"states\":[[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-2.665156,-2.583451,-1.636071,9.952373,10.085538],\"to\":[1,0]}],\"pruned\":0,\"coordinate\":[35.572222,129.194445],\"viterbi\":-2.665156}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-6.884677,-2.544593,-1.618366,10.314865,10.359506],\"to\":[2,0]},{\"properties\":[-6.884677,-6.730602,-6.361382,34.119226,10.359506],\"to\":[2,2]}],\"pruned\":0,\"coordinate\":[35.57218,129.194542],\"viterbi\":-6.884677}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-11.047636,-2.531258,-1.612691,10.257049,10.273314],\"to\":[3,0]},{\"properties\":[-11.047636,-3.102159,-3.953429,21.993268,10.273314],\"to\":[3,2]},{\"properties\":[-11.047636,-3.558716,-3.034934,17.400797,10.273314],\"to\":[3,4]}],\"pruned\":0,\"coordinate\":[35.572134,129.194641],\"viterbi\":-11.047636},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.572108,129.194801],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"suspicious\":0,\"transitions\":[{\"properties\":[-19.976661,-3.558716,-18.762082,96.036533,10.273314],\"to\":[3,3]}],\"pruned\":0,\"coordinate\":[35.572108,129.194801],\"viterbi\":-19.976661}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-15.191585,-2.569368,-20.497838,105.671969,11.229969],\"to\":[4,0]},{\"properties\":[-15.191585,-2.569368,-1.643732,11.058501,11.229969],\"to\":[4,1]},{\"properties\":[-15.191585,-2.863261,-21.280318,109.584372,11.229969],\"to\":[4,2]},{\"properties\":[-15.191585,-2.863261,-2.426549,7.144415,11.229969],\"to\":[4,3]},{\"properties\":[-15.191585,-2.863261,-2.426549,7.144415,11.229969],\"to\":[4,4]},{\"properties\":[-15.191585,-3.594176,-18.988633,98.125947,11.229969],\"to\":[4,6]},{\"properties\":[-15.191585,-3.594176,-3.084012,18.602839,11.229969],\"to\":[4,7]},{\"properties\":[-15.191585,-3.594176,-3.084012,18.602839,11.229969],\"to\":[4,8]}],\"pruned\":0,\"coordinate\":[35.572085,129.194737],\"viterbi\":-15.191585},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.572092,129.194798],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"suspicious\":0,\"transitions\":[{\"properties\":[-18.103224,-2.569368,-18.151394,93.939752,11.229969],\"to\":[4,0]},{\"properties\":[-18.103224,-3.594176,-16.64219,86.39373,11.229969],\"to\":[4,6]},{\"properties\":[-18.103224,-3.594176,-16.64219,86.39373,11.229969],\"to\":[4,7]}],\"pruned\":0,\"coordinate\":[35.572092,129.194798],\"viterbi\":-18.103224},{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.572051,129.194804],\"viterbi\":-42.297458},{\"suspicious\":0,\"transitions\":[{\"properties\":[-17.641286,-2.863261,-19.851435,102.439957,11.229969],\"to\":[4,2]},{\"properties\":[-17.641286,-3.594176,-1.655129,11.458424,11.229969],\"to\":[4,7]},{\"properties\":[-17.641286,-3.594176,-1.655129,11.458424,11.229969],\"to\":[4,8]}],\"pruned\":0,\"coordinate\":[35.572051,129.194804],\"viterbi\":-17.641286}],[{\"suspicious\":0,\"transitions\":[{\"properties\":[-38.258791,-2.569899,-17.368219,89.953324,11.159421],\"to\":[5,0]},{\"properties\":[-38.258791,-3.050634,-18.356677,94.895618,11.159421],\"to\":[5,3]},{\"properties\":[-38.258791,-3.050634,-18.356677,94.895618,11.159421],\"to\":[5,4]}],\"pruned\":0,\"coordinate\":[35.572019,129.194822],\"viterbi\":-38.258791},{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-19.404685,-2.569899,-1.876107,12.492767,11.159421],\"to\":[5,1]},{\"properties\":[-19.404685,-3.050634,-2.332118,7.546022,11.159421],\"to\":[5,4]},{\"properties\":[-19.404685,-3.050634,-2.332118,7.546022,11.159421],\"to\":[5,5]}],\"pruned\":0,\"coordinate\":[35.572019,129.194822],\"viterbi\":-19.404685},{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.572051,129.194804],\"viterbi\":-39.335164},{\"suspicious\":0,\"transitions\":[{\"properties\":[-20.481395,-2.569899,-16.585401,86.039238,11.159421],\"to\":[5,0]},{\"properties\":[-20.481395,-2.569899,-2.658588,16.40517,11.159421],\"to\":[5,1]},{\"properties\":[-20.481395,-3.050634,-17.57386,90.981532,11.159421],\"to\":[5,3]},{\"properties\":[-20.481395,-3.050634,-1.669239,11.458424,11.159421],\"to\":[5,4]},{\"properties\":[-20.481395,-3.050634,-1.669239,11.458424,11.159421],\"to\":[5,5]}],\"pruned\":0,\"coordinate\":[35.572051,129.194804],\"viterbi\":-20.481395},{\"suspicious\":0,\"transitions\":[{\"properties\":[-20.481395,-2.569899,-16.585401,86.039238,11.159421],\"to\":[5,0]},{\"properties\":[-20.481395,-3.050634,-17.57386,90.981532,11.159421],\"to\":[5,3]},{\"properties\":[-20.481395,-3.050634,-17.57386,90.981532,11.159421],\"to\":[5,4]}],\"pruned\":0,\"coordinate\":[35.572051,129.194804],\"viterbi\":-20.481395},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.571968,129.194876],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"suspicious\":0,\"transitions\":[{\"properties\":[-37.774395,-3.050634,-3.841322,0,11.159421],\"to\":[5,3]}],\"pruned\":0,\"coordinate\":[35.571968,129.194876],\"viterbi\":-37.774395},{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.571968,129.194876],\"viterbi\":-21.869773},{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.571968,129.194876],\"viterbi\":-21.869773}],[{\"suspicious\":0,\"transitions\":[{\"properties\":[-39.636695,-2.675634,-17.519299,91.285498,11.736195],\"to\":[6,0]},{\"properties\":[-39.636695,-6.250031,-14.992261,78.65031,11.736195],\"to\":[6,2]},{\"properties\":[-39.636695,-6.250031,-14.992261,78.65031,11.736195],\"to\":[6,5]}],\"pruned\":0,\"coordinate\":[35.57195,129.194926],\"viterbi\":-39.636695},{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-23.850691,-2.675634,-1.724823,11.159269,11.736195],\"to\":[6,1]},{\"properties\":[-23.850691,-6.250031,-4.021018,23.794098,11.736195],\"to\":[6,4]},{\"properties\":[-23.850691,-6.250031,-4.021018,23.794098,11.736195],\"to\":[6,5]}],\"pruned\":0,\"coordinate\":[35.57195,129.194926],\"viterbi\":-23.850691},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.571968,129.194876],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"suspicious\":0,\"transitions\":[{\"properties\":[-41.105889,-2.675634,-16.529949,86.338753,11.736195],\"to\":[6,0]},{\"properties\":[-41.105889,-6.250031,-14.002912,73.703564,11.736195],\"to\":[6,2]}],\"pruned\":0,\"coordinate\":[35.571968,129.194876],\"viterbi\":-41.105889},{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.571968,129.194876],\"viterbi\":-24.787437},{\"suspicious\":0,\"transitions\":[{\"properties\":[-24.787437,-2.675634,-2.482511,16.101563,11.736195],\"to\":[6,1]},{\"properties\":[-24.787437,-6.250031,-5.009477,28.736392,11.736195],\"to\":[6,4]},{\"properties\":[-24.787437,-6.250031,-5.009477,28.736392,11.736195],\"to\":[6,5]}],\"pruned\":0,\"coordinate\":[35.571968,129.194876],\"viterbi\":-24.787437}],[{\"suspicious\":0,\"transitions\":[{\"properties\":[-59.831627,-3.094747,-17.972628,93.972375,12.156423],\"to\":[7,1]},{\"properties\":[-59.831627,-3.217538,-17.849901,93.35874,12.156423],\"to\":[7,2]},{\"properties\":[-59.831627,-3.466072,-17.139179,89.805127,12.156423],\"to\":[7,4]}],\"pruned\":0,\"coordinate\":[35.571954,129.195046],\"viterbi\":-59.831627},{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-28.251148,-3.094747,-2.53864,16.802436,12.156423],\"to\":[7,1]},{\"properties\":[-28.251148,-3.217538,-2.224407,9.081576,12.156423],\"to\":[7,3]},{\"properties\":[-28.251148,-3.466072,-1.705191,12.635189,12.156423],\"to\":[7,5]}],\"pruned\":0,\"coordinate\":[35.571954,129.195046],\"viterbi\":-28.251148},{\"suspicious\":0,\"transitions\":[{\"properties\":[-60.878987,-3.217538,-3.33,3.553612,12.156423],\"to\":[7,2]},{\"properties\":[-60.878987,-3.466072,-4.040723,0,12.156423],\"to\":[7,4]}],\"pruned\":0,\"coordinate\":[35.57203,129.195145],\"viterbi\":-60.878987},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.57203,129.195145],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.57203,129.195145],\"viterbi\":-34.121741},{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.57203,129.195145],\"viterbi\":-34.121741}],[{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.572022,129.19519],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"suspicious\":0,\"transitions\":[{\"properties\":[-33.884536,-3.942374,-4.219546,0.732255,13.782794],\"to\":[8,3]}],\"pruned\":0,\"coordinate\":[35.572022,129.19519],\"viterbi\":-33.884536},{\"suspicious\":0,\"transitions\":[{\"properties\":[-67.426525,-3.072347,-17.311685,92.294031,13.782794],\"to\":[8,0]},{\"properties\":[-67.426525,-4.422348,-18.630148,98.886344,13.782794],\"to\":[8,4]}],\"pruned\":0,\"coordinate\":[35.572003,129.195124],\"viterbi\":-67.426525},{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-33.693094,-3.072347,-2.336812,10.145926,13.782794],\"to\":[8,1]},{\"properties\":[-33.693094,-3.942374,-2.675618,8.451891,13.782794],\"to\":[8,3]},{\"properties\":[-33.693094,-4.422348,-3.655274,3.553612,13.782794],\"to\":[8,5]}],\"pruned\":0,\"coordinate\":[35.572003,129.195124],\"viterbi\":-33.693094},{\"suspicious\":0,\"transitions\":[{\"properties\":[-68.385781,-3.072347,-18.022408,95.847643,13.782794],\"to\":[8,0]},{\"properties\":[-68.385781,-4.422348,-4.365997,0,13.782794],\"to\":[8,4]}],\"pruned\":0,\"coordinate\":[35.57203,129.195145],\"viterbi\":-68.385781},{\"suspicious\":0,\"transitions\":[{\"properties\":[-33.422411,-3.072347,-3.047534,6.592313,13.782794],\"to\":[8,1]},{\"properties\":[-33.422411,-3.942374,-20.320526,107.338235,13.782794],\"to\":[8,3]},{\"properties\":[-33.422411,-4.422348,-19.340871,102.439957,13.782794],\"to\":[8,5]}],\"pruned\":0,\"coordinate\":[35.57203,129.195145],\"viterbi\":-33.422411}],[{\"suspicious\":0,\"transitions\":[{\"properties\":[-87.810557,-3.390606,-16.752102,90.917313,15.203992],\"to\":[9,0]}],\"pruned\":0,\"coordinate\":[35.572088,129.19516],\"viterbi\":-87.810557},{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-39.102252,-3.390606,-2.345708,11.522644,15.203992],\"to\":[9,1]}],\"pruned\":0,\"coordinate\":[35.572088,129.19516],\"viterbi\":-39.102252},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.572021,129.195198],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.572021,129.195198],\"viterbi\":-40.311086},{\"suspicious\":0,\"transitions\":[{\"properties\":[-77.174126,-3.390606,-15.433639,84.324999,15.203992],\"to\":[9,0]}],\"pruned\":0,\"coordinate\":[35.57203,129.195145],\"viterbi\":-77.174126},{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.57203,129.195145],\"viterbi\":-41.770716}],[{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.572184,129.195118],\"viterbi\":-95.998371},{\"chosen\":1,\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.572184,129.195118],\"viterbi\":-44.838566}]]}}";
                String Data4 = "{\"status\":200,\"status_message\":\"Found matchings\",\"matchings\":[{\"matched_names\":[\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\"],\"matched_points\":[[35.572588,129.193451],[35.572557,129.19355],[35.572527,129.193648],[35.572497,129.193744],[35.572461,129.193841],[35.572424,129.193938],[35.572384,129.19404],[35.572344,129.194143],[35.572309,129.194235],[35.572264,129.194343]],\"hint_data\":{\"locations\":[\"YlPJCP____-U604AAgAAADMAAAAgAQAApAAAAN09nQY4PZ0Gm7IAAGzLHgLrVbMHCAABAQ\",\"YlPJCP____-U604AEQAAADMAAAAgAQAApAAAAN09nQY4PZ0Gm7IAAE3LHgJOVrMHCAABAQ\",\"YlPJCP____-U604AIAAAADMAAAAgAQAApAAAAN09nQY4PZ0Gm7IAAC_LHgKwVrMHCAABAQ\",\"YlPJCP____-U604ALgAAADMAAAAgAQAApAAAAN09nQY4PZ0Gm7IAABHLHgIQV7MHCAABAQ\",\"YlPJCP____-U604ACgAAAEgAAABTAQAAXAAAAN09nQY4PZ0Gm7IAAO3KHgJxV7MHCQABAQ\",\"YlPJCP____-U604AGQAAAEgAAABTAQAAXAAAAN09nQY4PZ0Gm7IAAMjKHgLSV7MHCQABAQ\",\"YlPJCP____-U604AKQAAAEgAAABTAQAAXAAAAN09nQY4PZ0Gm7IAAKDKHgI4WLMHCQABAQ\",\"YlPJCP____-U604AOQAAAEgAAABTAQAAXAAAAN09nQY4PZ0Gm7IAAHjKHgKfWLMHCQABAQ\",\"YlPJCP____-U604ASAAAAEgAAABTAQAAXAAAAN09nQY4PZ0Gm7IAAFXKHgL7WLMHCQABAQ\",\"YlPJCP____-U604AEQAAACsAAACbAQAAMQAAAN09nQY4PZ0Gm7IAACjKHgJnWbMHCgABAQ\"],\"checksum\":476095336},\"geometry\":[[35.572588,129.193451],[35.572557,129.19355],[35.572527,129.193648],[35.572497,129.193744],[35.572488,129.193774],[35.572461,129.193841],[35.572424,129.193938],[35.572384,129.19404],[35.572344,129.194143],[35.572309,129.194235],[35.572309,129.194235],[35.572264,129.194343]],\"indices\":[0,1,2,3,4,5,6,7,8,9],\"instructions\":[[\"10\",\"유니스트길 (UNIST-gil)\",10,0,2,\"9m\",\"E\",111,1,\"W\",291],[\"9\",\"유니스트길 (UNIST-gil)\",9,1,3,\"9m\",\"E\",111,1,\"W\",291],[\"9\",\"유니스트길 (UNIST-gil)\",9,2,5,\"9m\",\"E\",111,1,\"W\",291],[\"9\",\"유니스트길 (UNIST-gil)\",10,3,1,\"9m\",\"E\",110,1,\"W\",290],[\"9\",\"유니스트길 (UNIST-gil)\",10,5,3,\"9m\",\"SE\",115,1,\"NW\",295],[\"9\",\"유니스트길 (UNIST-gil)\",10,6,4,\"10m\",\"SE\",116,1,\"NW\",296],[\"9\",\"유니스트길 (UNIST-gil)\",10,7,6,\"10m\",\"SE\",116,1,\"NW\",296],[\"9\",\"유니스트길 (UNIST-gil)\",9,8,7,\"9m\",\"SE\",115,1,\"NW\",295],[\"9\",\"유니스트길 (UNIST-gil)\",11,9,2,\"10m\",\"N\",0,1,\"N\",0],[\"15\",\"\",0,11,0,\"0m\",\"N\",0,1,\"N\",0]],\"route_summary\":{\"total_time\":14,\"total_distance\":88}}],\"debug\":{\"breakage\":[0,0,0,0,0,0,0,0,0,0],\"states\":[[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-2.963804,-2.802984,-1.615436,9.597227,9.627218],\"to\":[1,0]}],\"pruned\":0,\"coordinate\":[35.572588,129.193451],\"viterbi\":-2.963804}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-7.382224,-2.667937,-1.624125,9.47311,9.546544],\"to\":[2,0]}],\"pruned\":0,\"coordinate\":[35.572557,129.19355],\"viterbi\":-7.382224}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-11.674286,-2.592068,-1.613233,9.303989,9.285011],\"to\":[3,0]}],\"pruned\":0,\"coordinate\":[35.572527,129.193648],\"viterbi\":-11.674286}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-15.879588,-2.580668,-1.64234,9.657396,9.821908],\"to\":[4,0]}],\"pruned\":0,\"coordinate\":[35.572497,129.193744],\"viterbi\":-15.879588}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-20.102596,-2.580668,-1.609438,9.692581,9.692579],\"to\":[5,0]}],\"pruned\":0,\"coordinate\":[35.572461,129.193841],\"viterbi\":-20.102596}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-24.292702,-2.608087,-1.614452,10.244401,10.269471],\"to\":[6,0]}],\"pruned\":0,\"coordinate\":[35.572424,129.193938],\"viterbi\":-24.292702}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-28.515241,-2.661063,-1.621427,10.325972,10.266025],\"to\":[7,0]}],\"pruned\":0,\"coordinate\":[35.572384,129.19404],\"viterbi\":-28.515241}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-32.797732,-2.724086,-1.791486,9.188597,10.098836],\"to\":[8,0]}],\"pruned\":0,\"coordinate\":[35.572344,129.194143],\"viterbi\":-32.797732}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-37.313303,-2.756058,-1.765737,10.978094,10.196598],\"to\":[9,0]}],\"pruned\":0,\"coordinate\":[35.572309,129.194235],\"viterbi\":-37.313303}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.572264,129.194343],\"viterbi\":-41.835098}]]}}";
                String Data5 = "{\"status\":200,\"status_message\":\"Found matchings\",\"matchings\":[{\"matched_names\":[\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\"],\"matched_points\":[[35.572929,129.192144],[35.572922,129.192248],[35.572877,129.192444],[35.572851,129.192541],[35.57275,129.192924],[35.572725,129.193018],[35.572698,129.193107],[35.572669,129.193198],[35.572642,129.193284],[35.572615,129.193367]],\"hint_data\":{\"locations\":[\"YlPJCP____-U604AAQAAABEAAABeAAAAiAEAAN09nQY4PZ0Gm7IAAMHMHgLQULMHBAABAQ\",\"YlPJCP____-U604AEAAAABEAAABeAAAAiAEAAN09nQY4PZ0Gm7IAALrMHgI4UbMHBAABAQ\",\"YlPJCP____-U604ACQAAAGUAAACBAAAAEQEAAN09nQY4PZ0Gm7IAAI3MHgL8UbMHBgABAQ\",\"YlPJCP____-U604AGAAAAGUAAACBAAAAEQEAAN09nQY4PZ0Gm7IAAHPMHgJdUrMHBgABAQ\",\"YlPJCP____-U604AUQAAAGUAAACBAAAAEQEAAN09nQY4PZ0Gm7IAAA7MHgLcU7MHBgABAQ\",\"YlPJCP____-U604AXwAAAGUAAACBAAAAEQEAAN09nQY4PZ0Gm7IAAPXLHgI6VLMHBgABAQ\",\"YlPJCP____-U604ACAAAADoAAADmAAAA1wAAAN09nQY4PZ0Gm7IAANrLHgKTVLMHBwABAQ\",\"YlPJCP____-U604AFgAAADoAAADmAAAA1wAAAN09nQY4PZ0Gm7IAAL3LHgLuVLMHBwABAQ\",\"YlPJCP____-U604AIwAAADoAAADmAAAA1wAAAN09nQY4PZ0Gm7IAAKLLHgJEVbMHBwABAQ\",\"YlPJCP____-U604ALwAAADoAAADmAAAA1wAAAN09nQY4PZ0Gm7IAAIfLHgKXVbMHBwABAQ\"],\"checksum\":476095336},\"geometry\":[[35.572929,129.192144],[35.572922,129.192248],[35.572922,129.192255],[35.572895,129.192378],[35.572877,129.192444],[35.572851,129.192541],[35.57275,129.192924],[35.572725,129.193018],[35.572716,129.193054],[35.572698,129.193107],[35.572669,129.193198],[35.572642,129.193284],[35.572615,129.193367]],\"indices\":[0,1,2,3,4,5,6,7,8,9],\"instructions\":[[\"10\",\"유니스트길 (UNIST-gil)\",9,0,2,\"9m\",\"E\",95,1,\"W\",275],[\"9\",\"유니스트길 (UNIST-gil)\",18,1,1,\"18m\",\"E\",90,1,\"W\",270],[\"9\",\"유니스트길 (UNIST-gil)\",9,4,2,\"9m\",\"E\",108,1,\"W\",288],[\"9\",\"유니스트길 (UNIST-gil)\",36,5,8,\"36m\",\"E\",108,1,\"W\",288],[\"9\",\"유니스트길 (UNIST-gil)\",9,6,10,\"8m\",\"E\",108,1,\"W\",288],[\"9\",\"유니스트길 (UNIST-gil)\",9,7,1,\"8m\",\"E\",107,1,\"W\",287],[\"9\",\"유니스트길 (UNIST-gil)\",9,9,2,\"8m\",\"E\",111,1,\"W\",291],[\"9\",\"유니스트길 (UNIST-gil)\",8,10,4,\"8m\",\"E\",111,1,\"W\",291],[\"9\",\"유니스트길 (UNIST-gil)\",8,11,5,\"8m\",\"E\",112,1,\"W\",292],[\"15\",\"\",0,12,0,\"0m\",\"N\",0,1,\"N\",0]],\"route_summary\":{\"total_time\":18,\"total_distance\":116}}],\"debug\":{\"breakage\":[0,0,0,0,0,0,0,0,0,0],\"states\":[[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-2.600537,-2.55897,-1.625097,9.440912,9.519209],\"to\":[1,0]}],\"pruned\":0,\"coordinate\":[35.572929,129.192144],\"viterbi\":-2.600537}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-6.784604,-2.675634,-1.681513,18.456712,18.817085],\"to\":[2,0]}],\"pruned\":0,\"coordinate\":[35.572922,129.192248],\"viterbi\":-6.784604}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-11.141751,-2.760033,-1.611671,9.239697,9.228534],\"to\":[3,0]}],\"pruned\":0,\"coordinate\":[35.572877,129.192444],\"viterbi\":-11.141751}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-15.513455,-3.269604,-1.626313,36.425183,36.509558],\"to\":[4,0]}],\"pruned\":0,\"coordinate\":[35.572851,129.192541],\"viterbi\":-15.513455}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-20.409372,-3.418856,-1.613084,8.947151,8.96538],\"to\":[5,0]}],\"pruned\":0,\"coordinate\":[35.57275,129.192924],\"viterbi\":-20.409372},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.57293,129.192994],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-25.441312,-3.699141,-1.699387,8.603305,9.053049],\"to\":[6,0]}],\"pruned\":0,\"coordinate\":[35.572725,129.193018],\"viterbi\":-25.441312},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.572906,129.193089],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-30.83984,-4.095426,-1.628721,8.842021,8.938435],\"to\":[7,0]}],\"pruned\":0,\"coordinate\":[35.572698,129.193107],\"viterbi\":-30.83984},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.572881,129.193184],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-36.563987,-3.699141,-1.622378,8.339807,8.404506],\"to\":[8,0]}],\"pruned\":0,\"coordinate\":[35.572669,129.193198],\"viterbi\":-36.563987},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.572854,129.193287],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-41.885506,-3.274239,-1.645127,8.087199,8.265642],\"to\":[9,0]}],\"pruned\":0,\"coordinate\":[35.572642,129.193284],\"viterbi\":-41.885506},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.572826,129.193373],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.572615,129.193367],\"viterbi\":-46.804872}]]}}";
                String Data6 = "{\"status\":200,\"status_message\":\"Found matchings\",\"matchings\":[{\"matched_names\":[\"\",\"\",\"\",\"\",\"\",\"\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\",\"유니스트길 (UNIST-gil)\"],\"matched_points\":[[35.573308,129.191599],[35.573216,129.191573],[35.573131,129.191552],[35.573047,129.191532],[35.572961,129.191507],[35.572898,129.191488],[35.572864,129.191609],[35.572879,129.191724],[35.572896,129.191829],[35.572913,129.191934],[35.572924,129.192039]],\"hint_data\":{\"locations\":[\"_____3JTyQgAAAAADwAAAAAAAAAuAAAAFAAAACQ9nQbmPZ0Gm7IAADzOHgKvTrMHAgABAQ\",\"_____3JTyQgAAAAAAgAAAA0AAAAuAAAAFAAAACQ9nQbmPZ0Gm7IAAODNHgKVTrMHAgABAQ\",\"_____3JTyQgAAAAADAAAAAgAAAAZAAAAJAAAACQ9nQbmPZ0Gm7IAAIvNHgKATrMHAQABAQ\",\"_____3JTyQgAAAAAAQAAABMAAAAZAAAAJAAAACQ9nQbmPZ0Gm7IAADfNHgJsTrMHAQABAQ\",\"_____3JTyQgAAAAADgAAAAoAAAAAAAAAOQAAACQ9nQbmPZ0Gm7IAAOHMHgJTTrMHAAABAQ\",\"_____3JTyQgAAAAABgAAABIAAAAAAAAAOQAAACQ9nQbmPZ0Gm7IAAKLMHgJATrMHAAABAQ\",\"YlPJCP____-U604AEgAAABkAAAAAAAAA3gEAAN09nQY4PZ0Gm7IAAIDMHgK5TrMHAAABAQ\",\"YlPJCP____-U604ACgAAABQAAAAZAAAAygEAAN09nQY4PZ0Gm7IAAI_MHgIsT7MHAQABAQ\",\"YlPJCP____-U604ABQAAABwAAAAtAAAArgEAAN09nQY4PZ0Gm7IAAKDMHgKVT7MHAgABAQ\",\"YlPJCP____-U604AFAAAABwAAAAtAAAArgEAAN09nQY4PZ0Gm7IAALHMHgL-T7MHAgABAQ\",\"YlPJCP____-U604ABwAAABUAAABJAAAAmQEAAN09nQY4PZ0Gm7IAALzMHgJnULMHAwABAQ\"],\"checksum\":476095336},\"geometry\":[[35.573308,129.191599],[35.573216,129.191573],[35.573196,129.191568],[35.573131,129.191552],[35.573047,129.191532],[35.573039,129.191531],[35.572961,129.191507],[35.572898,129.191488],[35.572853,129.191475],[35.572864,129.191609],[35.572868,129.191653],[35.572879,129.191724],[35.572891,129.191794],[35.572896,129.191829],[35.572913,129.191934],[35.572922,129.191987],[35.572924,129.192039]],\"indices\":[0,1,2,3,4,5,6,7,8,9,10],\"instructions\":[[\"10\",\"\",10,0,1,\"10m\",\"S\",193,1,\"N\",13],[\"9\",\"\",10,1,1,\"9m\",\"S\",191,1,\"N\",11],[\"9\",\"\",10,3,2,\"9m\",\"S\",191,1,\"N\",11],[\"9\",\"\",10,4,1,\"9m\",\"S\",186,1,\"N\",6],[\"9\",\"\",7,6,2,\"7m\",\"S\",194,1,\"N\",14],[\"9\",\"\",5,7,4,\"5m\",\"S\",193,1,\"N\",13],[\"7\",\"유니스트길 (UNIST-gil)\",12,8,2,\"12m\",\"E\",84,1,\"W\",264],[\"9\",\"유니스트길 (UNIST-gil)\",11,9,1,\"10m\",\"E\",84,1,\"W\",264],[\"9\",\"유니스트길 (UNIST-gil)\",10,11,1,\"9m\",\"E\",78,1,\"W\",258],[\"9\",\"유니스트길 (UNIST-gil)\",10,13,2,\"9m\",\"E\",79,1,\"W\",259],[\"9\",\"유니스트길 (UNIST-gil)\",10,14,1,\"9m\",\"E\",78,1,\"W\",258],[\"15\",\"\",0,16,0,\"0m\",\"N\",0,1,\"N\",0]],\"route_summary\":{\"total_time\":16,\"total_distance\":104}}],\"debug\":{\"breakage\":[0,0,0,0,0,0,0,0,0,0,0],\"states\":[[{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.573308,129.191599],\"viterbi\":-2.52854},{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-2.52854,-2.552938,-1.618096,10.499683,10.542971],\"to\":[1,1]}],\"pruned\":0,\"coordinate\":[35.573308,129.191599],\"viterbi\":-2.52854}],[{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.573216,129.191573],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-6.699573,-2.56145,-1.616241,9.643241,9.609224],\"to\":[2,1]}],\"pruned\":0,\"coordinate\":[35.573216,129.191573],\"viterbi\":-6.699573}],[{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.573131,129.191552],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-10.877264,-2.5391,-1.611464,9.516599,9.52673],\"to\":[3,1]}],\"pruned\":0,\"coordinate\":[35.573131,129.191552],\"viterbi\":-10.877264}],[{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.573047,129.191532],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-15.027829,-2.52985,-1.609744,9.837621,9.836091],\"to\":[4,1]},{\"properties\":[-15.027829,-5.232198,-4.954025,26.559025,9.836091],\"to\":[4,2]},{\"properties\":[-15.027829,-5.614874,-4.080977,22.193788,9.836091],\"to\":[4,4]}],\"pruned\":0,\"coordinate\":[35.573047,129.191532],\"viterbi\":-15.027829}],[{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.572961,129.191507],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-19.167422,-2.56145,-1.619606,7.215004,7.265846],\"to\":[5,1]},{\"properties\":[-19.167422,-2.948392,-3.208249,15.259903,7.265846],\"to\":[5,2]},{\"properties\":[-19.167422,-3.105198,-2.627542,12.356365,7.265846],\"to\":[5,4]}],\"pruned\":0,\"coordinate\":[35.572961,129.191507],\"viterbi\":-19.167422},{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.572857,129.191523],\"viterbi\":-25.214051},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.572853,129.191475],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.572853,129.191475],\"viterbi\":-24.72368}],[{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.572898,129.191488],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-23.348478,-2.534562,-3.047561,17.325913,10.135297],\"to\":[6,0]},{\"properties\":[-23.348478,-5.107288,-3.523809,0.563442,10.135297],\"to\":[6,2]},{\"properties\":[-23.348478,-5.530975,-2.608197,5.141504,10.135297],\"to\":[6,4]}],\"pruned\":0,\"coordinate\":[35.572898,129.191488],\"viterbi\":-23.348478},{\"suspicious\":0,\"transitions\":[{\"properties\":[-25.324063,-2.534562,-1.780105,9.28196,10.135297],\"to\":[6,0]}],\"pruned\":0,\"coordinate\":[35.572855,129.191507],\"viterbi\":-25.324063},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.572853,129.191475],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"suspicious\":0,\"transitions\":[{\"properties\":[-24.900162,-5.530975,-3.636497,0,10.135297],\"to\":[6,4]}],\"pruned\":0,\"coordinate\":[35.572853,129.191475],\"viterbi\":-24.900162}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-28.930601,-2.564661,-1.615925,10.544187,10.511754],\"to\":[7,0]}],\"pruned\":0,\"coordinate\":[35.572864,129.191609],\"viterbi\":-28.930601},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.572893,129.191487],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.572893,129.191487],\"viterbi\":-31.979575},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.572853,129.191475],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.572853,129.191475],\"viterbi\":-31.48765}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-33.111186,-2.58552,-1.613171,9.686818,9.668154],\"to\":[8,0]}],\"pruned\":0,\"coordinate\":[35.572879,129.191724],\"viterbi\":-33.111186}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-37.309878,-2.601356,-1.618612,9.685585,9.731457],\"to\":[9,0]}],\"pruned\":0,\"coordinate\":[35.572896,129.191829],\"viterbi\":-37.309878}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-41.529846,-2.600046,-1.6393,9.607859,9.757171],\"to\":[10,0]}],\"pruned\":0,\"coordinate\":[35.572913,129.191934],\"viterbi\":-41.529846}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.572924,129.192039],\"viterbi\":-45.769192}]]}}";


                String Osrm_DATA = "{\"status\":200,\"status_message\":\"Found matchings\",\"matchings\":[{\"matched_names\":[\"송현길 Songhyeon-gil\",\"송현길 Songhyeon-gil\",\"당앞로 (Dangap-ro)\"],\"matched_points\":[[35.568375,129.23154],[35.568238,129.231739],[35.567867,129.231957]],\"hint_data\":{\"locations\":[\"_____xsCggQR4zIAFwAAAAMAAAAVAAAA4wAAAMamzwPFps8DULEAAPe6HgK06rMHAQABAQ\",\"_____xsCggQR4zIABwAAABMAAAAVAAAA4wAAAMamzwPFps8DULEAAG66HgJ767MHAQABAQ\",\"ccuBBP____90604AEQAAADEAAAAAAAAAOwAAABTInAYfyJwGULEAAPu4HgJV7LMHAAABAQ\"],\"checksum\":1465161250},\"geometry\":[[35.568375,129.23154],[35.568238,129.231739],[35.56817,129.23184],[35.568,129.23212],[35.567867,129.231957]],\"indices\":[0,1,2],\"instructions\":[[\"10\",\"송현길 Songhyeon-gil\",24,0,2,\"23m\",\"SE\",130,1,\"NW\",310],[\"9\",\"송현길 Songhyeon-gil\",43,1,6,\"43m\",\"SE\",130,1,\"NW\",310],[\"3\",\"당앞로 (Dangap-ro)\",21,3,2,\"20m\",\"SW\",225,1,\"NE\",45],[\"15\",\"\",0,4,0,\"0m\",\"N\",0,1,\"N\",0]],\"route_summary\":{\"total_time\":7,\"total_distance\":88}}],\"debug\":{\"breakage\":[0,0,0],\"states\":[[{\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.568375,129.23154],\"viterbi\":-2.608959},{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-2.608959,-2.530021,-1.628658,23.587124,23.683226],\"to\":[1,1]}],\"pruned\":0,\"coordinate\":[35.568375,129.23154],\"viterbi\":-2.608959}],[{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.568238,129.231739],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368},{\"chosen\":1,\"suspicious\":0,\"transitions\":[{\"properties\":[-6.767638,-2.91161,-6.130394,64.361605,41.756824],\"to\":[2,0]}],\"pruned\":0,\"coordinate\":[35.568238,129.231739],\"viterbi\":-6.767638}],[{\"chosen\":1,\"suspicious\":0,\"transitions\":[],\"pruned\":0,\"coordinate\":[35.567867,129.231957],\"viterbi\":-15.809643},{\"suspicious\":1,\"transitions\":[],\"pruned\":1,\"coordinate\":[35.567867,129.231957],\"viterbi\":-179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368}]]}}";


                ShowingInstruction = new ProvideInstructions(getBaseContext());
                ShowingInstruction.setOsrmQueryData(Data6);
                ShowingInstruction.setOsrmQueryData(Data5);
                ShowingInstruction.setOsrmQueryData(Data4);
                ShowingInstruction.setOsrmQueryData(Data3);
                ShowingInstruction.setOsrmQueryData(Data2);
                ShowingInstruction.setOsrmQueryData(Data1);

                for (int i=0;i<latLat.length;i+=2) {
                    Location tempLoc = null;

                    tempLoc = new Location("Temp");

                    tempLoc.setLatitude(latLat[i]);
                    tempLoc.setLongitude(latLat[i + 1]);
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latLat[i],latLat[i+1]), MyState.mCurrentCameraLevel));
                    try {
                        ShowingInstruction.QueryInstructions(tempLoc);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                */
                //double[] latLat = {35.573308,129.191601,35.573214,129.191585,35.573129,129.191566,35.573046,129.19154,35.572961,129.19151,35.572896,129.191502,35.572869,129.191609,35.572891,129.191722,35.572911,129.191827,35.57293,129.191931,35.572941,129.192038,35.572946,129.192146,35.572933,129.19225,35.5729,129.192454,35.57288,129.192553,35.572802,129.192945,35.572782,129.193042,35.572762,129.193138,35.572743,129.193235,35.572706,129.193315,35.572666,129.193393,35.572627,129.193471,35.572588,129.193565,35.572549,129.193659,35.572512,129.193752,35.572474,129.193849,35.572437,129.193946,35.5724,129.19405,35.572365,129.194155,35.57233,129.194259,35.572291,129.194361,35.572243,129.194458,35.572193,129.194552,35.572141,129.194646,35.572088,129.194739,35.572025,129.194836,35.571962,129.194933,35.571931,129.195056,35.571975,129.195179,35.572095,129.195217,35.572226,129.195169,35.572265,129.194943,35.572333,129.194723,35.572405,129.194546,35.572459,129.194447,35.572514,129.194345,35.572573,129.194214,35.572629,129.194074,35.572686,129.193919,35.572743,129.193763,35.572793,129.193535,35.572837,129.193417,35.572882,129.193278,35.572928,129.193103,35.572974,129.192929,35.57302,129.192755,35.573066,129.19258,35.573131,129.192452,35.573212,129.192409,35.573319,129.19232,35.573382,129.192269};
                if (!MyState.isLeader && MyState.mySpaceId!="")
                {
                    String Osrm_Data = null;
                    if (rxThread != null) {
                        Osrm_Data = rxThread.getLastReceivedOsrmData();
                        if (Osrm_Data.indexOf("empty") < 0)
                            ShowingInstruction.setOsrmQueryData(Osrm_Data);
                    }

                    try {

                        //location.setLatitude(latLat[inpor++]);
                        //location.setLongitude(latLat[inpor++]);


                        String instruction = ShowingInstruction.QueryInstructions(location);

                        ImageButton inst_sign = (ImageButton) findViewById(R.id.image_sign);

                        if (instruction.indexOf("Left") >= 0)
                        {
                            inst_sign.setBackgroundResource(R.drawable.turnleft);
                        }
                        else if (instruction.indexOf("Right") >= 0)
                        {
                            inst_sign.setBackgroundResource(R.drawable.turnright);
                        }
                        else if (instruction.indexOf("Goraight") >= 0)
                        {
                            inst_sign.setBackgroundResource(R.drawable.gostraight);
                        }
                        else if (instruction.indexOf("RoundAbout") >=0 )
                        {
                            inst_sign.setBackgroundResource(R.drawable.roundaboutsign);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                mLatLng = new LatLng(MyState.mLastLocation.getLatitude(), MyState.mLastLocation.getLongitude());
                //map.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, MyState.mCurrentCameraLevel));

                if ( outputStreamWriter != null ) {
                    //first_time=true;
                    write_data(MyState.mLastLocation.getLatitude() + ";" + MyState.mLastLocation.getLongitude() + ";" + MyState.id.toString() + ";" + MyState.mLastUpdateTime);
                    //write_data("35.573023;129.192266;1424684612;ggg");
                    //write_data("35.572164;129.203585;1424684616;ggg");
                }

                /*if (MyState.activeToUseLeadersLocations == true) {

                    startSendingLeadersLocatons();
                    MyState.activeToUseLeadersLocations = false;

                }*/

                if (amILearder && (MyState.mLastUpdateTime - last_leaders_send_locations_time) >= 15 ) {
                    sendLeadersLastLocations(Leader_every_10_15_second_lacations);
                    last_leaders_send_locations_time = MyState.mLastUpdateTime;
                    Leader_every_10_15_second_lacations = "";
                    //MyState.sendLeadersLocaion();
                }
                Leader_every_10_15_second_lacations += location.getLatitude() + ";" + location.getLongitude() + ";" + MyState.mLastUpdateTime + ";";
            }
        };

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
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                MyState.mRequestLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
            }
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                MyState.mLastLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_KEY)) {
                MyState.mLastUpdateTime = savedInstanceState.getLong(
                        LAST_UPDATED_TIME_KEY);
            }
        }
    }


    // cjoo: check connectivity
    //      (Soon, we will not support Wi-Fi...)
    private boolean checkConnection() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            MyState.getLocalIpAddress();
            return true;
        } else {
            return false;
        }
    }

    /////////////////////////////////////////// This is an example (remove)
    private void runUDPExample() {
        new Thread(new Runnable() {
            String msg = "Hello Server";
            byte[] recvMsg = new byte[1000];
            //int port = 8001;

            @Override
            public void run() {
                try {
                    DatagramSocket s = new DatagramSocket();
                    //MyState.serverAddr = InetAddress.getByName("140.254.222.199");
                    DatagramPacket p = new DatagramPacket(msg.getBytes(), msg.length(), MyState.serverAddr, MyState.serverPort);
                    s.send(p);
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /////////////////////////////////////////////

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


        ShowingInstruction = new ProvideInstructions(getBaseContext());


        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Space name");
        alert.setMessage("Type the space name to join");

        // Set an EditText view to get user input

        final EditText input = new EditText(this);
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton)
            {
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

    void Init_space_info()
    {
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

}
/**************************************************************************************************************************/
/*****************************************************Receive Thread*******************************************************/
/**************************************************************************************************************************/

class Receive implements Runnable {

    JSONArray array_of_points;
    JSONObject jsonObject = null;
    String followersID;
    String LeadersID;
    GoogleMap map;
    MainActivity activity;
    User MyState;
    Users uList;
    String spaceOwner;
    DatagramSocket rSocket = null;
    DatagramPacket rPacket = null;
    byte[] rMessage = new byte[12000];
    thread_sendLocation updateThread;
    //thread_sendLeadersLocations send_leaders_locations;
    Thread wrapUpdateThread;
    private volatile boolean stopRequested;
    String complete_msg;
    private LinkedList<String> OsrmQueryData;


    public String getLastReceivedOsrmData()
    {
        if (OsrmQueryData.isEmpty())
            return "empty";
        String temp =OsrmQueryData.getFirst();
        OsrmQueryData.removeFirst();
        return temp;
    }

    public void setLastReceivedOsrmData(String lastReceivedOsrmData) {
        OsrmQueryData.addLast(lastReceivedOsrmData);
    }

    String LastReceivedOsrmData;



    public Receive(DatagramSocket sck, User state, Users uList, MainActivity myActivity) {
        this.rSocket = sck;
        this.MyState = state;
        this.uList = uList;
        updateThread = null;
        stopRequested = false;
        activity = myActivity;
        //((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        map = ((SupportMapFragment) activity.getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        complete_msg="";
        OsrmQueryData = new LinkedList<String>() ;
    }

    public User getUser()
    {
        return MyState;
    }

    public void requestStop() {
        stopRequested = true;
        if (updateThread != null) {
            updateThread.requestStop();
        }

    }

    public void run() {
        while (stopRequested == false) {
            try {// cjoo: debug
                rPacket = new DatagramPacket(rMessage, rMessage.length);
                rSocket.receive(rPacket);
                handlePacket(rPacket);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void handlePacket(DatagramPacket pkt) {
        String msg;
        int index;
        String msgType;

        msg = new String(rPacket.getData(), 0, pkt.getLength());
        //MyState.debugMsg = msg;
        //MyState.mySpaceId = new String(recvMsg, 0, pkt.getLength());
        //MyState.debugMsg = MyState.mySpaceId;

        index = msg.indexOf(";");
        if (index <= 0) {
            return;
        } else {
            msgType = msg.substring(0, index);
            msg = msg.substring(index + 1, msg.length());

        }
        // now received something, which means connectivity
        // if connected (to the Server), then start sending location info.
        if (updateThread == null || wrapUpdateThread.getState() == Thread.State.TERMINATED) {
            MyState.isSendingUpates = true;
            updateThread = new thread_sendLocation(MyState, rSocket);
            wrapUpdateThread = new Thread(updateThread);
            wrapUpdateThread.start();
        }


        if (msgType.equals("New User OK")) {  // response to New User registration
            // do nothing
        } else if (msgType.equals("Create Space OK")) {  // response to Create Space
            // remember space id

            MyState.isLeader = true;

        } else if (msgType.equals("Join Space OK")) {  // response to Join Space
            // remember space id
            if (MyState.isLeader == true)
            {
                MyState.isLeader=false;
                MyState.mLeardersLastUpdateTime = 0;
                MyState.firstLeadersMark = true;
                MyState.mLeadersLastLatLng = null;
            }
        } else if (msgType.equals("Location Update")) {  // periodic info.
            // location & user id
            updateUserInfo(msg, uList);
        }else if (msgType.equals(("Leader's Locations"))) {

            updateLeadersLocation(msg, uList);

        }


    }




    public void updateLeadersLocation(String message, Users uList)
    {

        int index;
        User follower;
        String messageState;
        HttpAsyncTask httpAsyncTask;


        // parsing & retrieving info
        index = message.indexOf(";");

        if (index <= 0) {
            return;
        }

        else {
            followersID = message.substring(0, index);
            message = message.substring(index + 1, message.length());
        }

        // if non-existing user, add it
        follower = uList.findUser(followersID);


        if (follower == null) {
            follower = new User();
            follower.id = followersID;
            uList.addUser(follower);
        }

        follower.LeaderLocations = message;
        follower.activeToUseLeadersLocations = true;

        index = message.indexOf(";");
        if (index < 0){
            return;
        }
        else
        {
            messageState = message.substring(0,index);
            message = message.substring( index+1 );
        }

        if (messageState.equals("finish"))
        {

            int until;
            until = message.indexOf("*****");
            complete_msg += message.substring(0,until);

            setLastReceivedOsrmData(complete_msg);

            httpAsyncTask = (HttpAsyncTask) new HttpAsyncTask();
            httpAsyncTask.execute(complete_msg);


            complete_msg="";
        }
        else if (messageState.equals("to be continue"))
        {
            int until;
            until = message.indexOf("*****");
            complete_msg += message.substring(0,until);;
        }


    }


    class HttpAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... arg) {

            return arg[0];

        }

        // onPostExecute displays the results of the AsyncTask.

        protected void onPostExecute(String message)
        {
            if (message.indexOf("geometry") < 0)
            {
                actual_locations(message);
            }
            else
            {
                matched_locations(message);
            }
        }

        void matched_locations(String message)
        {
            int index;
            String LeadersID;

            index = message.indexOf(";");
            if (index < 0) {
                return;
            } else {
                LeadersID = message.substring(0, index);
                message = message.substring(index + 1);
            }

            try {
                jsonObject = new JSONObject(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JSONArray jsonArray = null;
            if (jsonObject!=null) {
                try {
                    jsonArray = (JSONArray) jsonObject.get(/*"matched_points"*/ "matchings");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            JSONObject jsonObject2 = null;
            if (jsonArray!=null) {
                try {
                    jsonObject2 = (JSONObject) jsonArray.get(/*"matched_points"*/ 0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            array_of_points = null;
            if (jsonObject2!=null) {
                try {
                    array_of_points = (JSONArray) jsonObject2.get("geometry");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            LatLng location = null;
            double lat = 0., lgt=0.;

            for (int i = 0; i < array_of_points.length(); i++) {

                try {
                    lat = (double) ((JSONArray) array_of_points.get(i)).get(0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    lgt = (double) ((JSONArray) array_of_points.get(i)).get(1);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                location = new LatLng(lat, lgt);
                //if (MyState.mLeardersLastUpdateTime < Long.parseLong(update_TIME)) {
                if (MyState.isLeader == false) {
                    if (MyState.firstLeadersMark == true) {
                        map.addMarker(new MarkerOptions().position(location).title("Leader's Locatoin" + LeadersID));
                        MyState.LeadersMark = map.addMarker(new MarkerOptions().position(location).title("Leader's Locatoin" + LeadersID));
                        MyState.mLeadersLastLatLng = location;
                        MyState.firstLeadersMark = false;
                    } else {
                        PolylineOptions line = new PolylineOptions()
                                .add(MyState.mLeadersLastLatLng)
                                .add(location);
                        map.addPolyline(line);
                        MyState.mLeadersLastLatLng = location;
                    }

                    if (MyState.LeadersMark != null) {

                        MyState.LeadersMark.remove();
                        MyState.LeadersMark = map.addMarker(new MarkerOptions().position(location).title("Leader's Locatoin" + LeadersID));
                    }
                }
                //}
            }
            //MarkerOptions marker = new MarkerOptions().position(location);

           // map.addMarker(marker);

        }
        void actual_locations(String message)
        {
            int index;
            String LeadersID;
            index = message.indexOf(";");
            if (index < 0) {
                return;
            } else {
                LeadersID = message.substring(0, index);
                message = message.substring(index + 1);
            }
            while (message.length() > 0) {
                LatLng location;
                String lat, lgt, update_TIME;

                index = message.indexOf(";");
                if (index < 0) {
                    return;
                } else {
                    lat = message.substring(0, index);
                    message = message.substring(index + 1);
                }

                index = message.indexOf(";");
                if (index < 0) {
                    return;
                } else {
                    lgt = message.substring(0, index);
                    message = message.substring(index + 1);
                }

                index = message.indexOf(";");
                if (index < 0) {
                    return;
                } else {
                    update_TIME = message.substring(0, index);
                    message = message.substring(index + 1);
                }

                location = new LatLng(Double.parseDouble(lat), Double.parseDouble(lgt));
                if (MyState.mLeardersLastUpdateTime < Long.parseLong(update_TIME)) {
                    if (MyState.isLeader == false) {
                        if (MyState.firstLeadersMark == true) {
                            map.addMarker(new MarkerOptions().position(location).title("Leader's Locatoin" + LeadersID));
                            MyState.LeadersMark = map.addMarker(new MarkerOptions().position(location).title("Leader's Locatoin" + LeadersID));
                            MyState.mLeadersLastLatLng = location;
                            MyState.firstLeadersMark = false;
                        } else {
                            PolylineOptions line = new PolylineOptions()
                                    .add(MyState.mLeadersLastLatLng)
                                    .add(location);
                            map.addPolyline(line);
                            MyState.mLeadersLastLatLng = location;
                        }

                        if (MyState.LeadersMark != null) {

                            MyState.LeadersMark.remove();
                            MyState.LeadersMark = map.addMarker(new MarkerOptions().position(location).title("Leader's Locatoin" + LeadersID));
                        }
                    }
                }
            }
        }

    }

    public void updateUserInfo(String msg, Users uList) {
        String text;
        int index;
        User user;
        String clientId;
        String cLatitude;
        String cLongitude;
        String cLastUpdateTime;

        spaceOwner = "";
        // parsing & retrieving info
        text = msg;
        while (text.length() > 10) {
            index = text.indexOf(";");
            if (index <= 0) {
                return;
            } else {
                clientId = text.substring(0, index);
                text = text.substring(index + 1, text.length());
            }

            // the first client is the space owner
            if (spaceOwner.equals("")) {
                spaceOwner = clientId;
            }

            // if non-existing user, add it
            user = uList.findUser(clientId);
            if (user == null) {
                user = new User();
                user.id = clientId;
                uList.addUser(user);
            }

            index = text.indexOf(";");
            if (index <= 0) {
                return;
            } else {
                cLatitude = text.substring(0, index);
                text = text.substring(index + 1, text.length());
            }
            index = text.indexOf(";");
            if (index <= 0) {
                return;
            } else {
                cLongitude = text.substring(0, index);
                text = text.substring(index + 1, text.length());
            }
            index = text.indexOf(";");
            if (index <= 0) {
                return;
            } else {
                cLastUpdateTime = text.substring(0, index);
                text = text.substring(index + 1, text.length());
            }
            user.mLatLng = new LatLng(Double.parseDouble(cLatitude), Double.parseDouble(cLongitude));
            user.mLastUpdateTime = Long.parseLong(cLastUpdateTime);
        }
    }

}


/**************************************************************************************************************************/
/*****************************************************Send Thread**********************************************************/
/**************************************************************************************************************************/


class thread_sendLocation implements Runnable {
    User MyState;
    DatagramSocket tSocket;
    String MSGTYPE;
    private volatile boolean stopRequested;

    public thread_sendLocation(User state, DatagramSocket skt) {
        this.MyState = state;
        this.tSocket = skt;
        this.MSGTYPE = "Location";
        stopRequested = false;
    }


    public void requestStop() {
        stopRequested = true;
    }

    public void run() {
        while (stopRequested == false) {
            try {
                Thread.sleep(MyState.updateInterval * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Caution: sendLocation should come after sleep
            //          otherwise, the app will terminate.
            //          (it seems to need some delay for TX)
            if (MyState.isConnected == true)
                MyState.sendLocation(tSocket, MSGTYPE);
        }
    }
}


/**************************************************************************************************************************/
/**********************************************Provide Instructions Class**************************************************/
/**************************************************************************************************************************/


class   ProvideInstructions
{

    private String TurnInstruction[]=
            {
                    "NoTurn", //0
                    "GoStraight",//1
                    "TurnSlightRight",//2
                    "TurnRight",//3
                    "TurnSharpRight",//4
                    "UTurn",//5
                    "TurnSharpLeft",//6
                    "TurnLeft",//7
                    "TurnSlightLeft",//8
                    "ReachViaLocation",//9
                    "HeadOn",//10
                    "EnterRoundAbout",//11
                    "LeaveRoundAbout",//12
                    "StayOnRoundAbout",//13
                    "StartAtEndOfStreet",//14
                    "ReachedYourDestination",//15
                    "EnterAgainstAllowedDirection",//16
                    "LeaveAgainstAllowedDirection",//17
                    "InverseAccessRestrictionFlag",//18
                    "AccessRestrictionFlag",//19
                    "AccessRestrictionPenalty" };

    private LinkedList<String> OsrmQueryData;
    private JSONArray geometry_points;
    private JSONArray instruction_points;
    private JSONArray instructionOnIndex;


    private float recentDistance;

    Boolean GetNextStatus;

    private Integer index_geometry;
    private Integer index_instruction;

    private Context baseContext;

    public ProvideInstructions(Context baseContext1)
    {

        baseContext = baseContext1;
        OsrmQueryData = new LinkedList<String>() ;
        geometry_points = null;
        instruction_points = null;
        instructionOnIndex=null;
        index_instruction=-1;
        recentDistance = -1.0f;
        GetNextStatus=true;

    }

    public void setOsrmQueryData(String QueryData)
    {
        OsrmQueryData.addLast(QueryData);
    }

    private void removeFirstOsrmData()
    {
        OsrmQueryData.removeFirst();
        instruction_points=null;
        geometry_points=null;
        instructionOnIndex=null;
        index_instruction=-1;
    }

    private Boolean getFirstOsrmData() throws JSONException
    {
        JSONObject osrmData = null;
        if (OsrmQueryData.isEmpty())
            return false;
        String OsrqData = OsrmQueryData.getFirst();
        OsrmQueryData.removeFirst();

        if (OsrqData.indexOf("instructions")<=0)
            return false;

        osrmData = new JSONObject(OsrqData);

        if (osrmData == null)
            return false;

        JSONArray osrmGeometryAndInstructions = null;

        osrmGeometryAndInstructions = osrmData.getJSONArray("matchings");
        instruction_points = (JSONArray) ((JSONObject)(osrmGeometryAndInstructions.get(0))).get("instructions");
        geometry_points = (JSONArray) ((JSONObject)(osrmGeometryAndInstructions.get(0))).get("geometry");

        index_instruction=0;

        return true;
    }



    public String QueryInstructions(Location FollowersLoc) throws JSONException {

        if (OsrmInstructionsCondition())
        {
            Boolean contin = true;


            if (GetNextStatus && !GetNextInstructions())
            {
                return "";
            }


            instructionOnIndex = (JSONArray) instruction_points.get(index_instruction);
            int linked_instructions = (int) instructionOnIndex.get(3);
            int which_inst = Integer.parseInt(instructionOnIndex.get(0).toString());


            JSONArray latlon = null;
            latlon = (JSONArray) geometry_points.get(linked_instructions);


            Location locationA;
            Location locationB = new Location("point B");

            locationA = FollowersLoc;


            locationB.setLatitude((Double) latlon.get(0));
            locationB.setLongitude((Double) latlon.get(1));


            float lastDistance = recentDistance;

            recentDistance = locationA.distanceTo(locationB);


            if ( recentDistance < 30.0f)
            {
                if (lastDistance>=0.0f && lastDistance < recentDistance)
                {
                    GetNextStatus=true;
                    index_instruction++;
                    return QueryInstructions(FollowersLoc);
                }

                Toast.makeText(baseContext, TurnInstruction[which_inst], Toast.LENGTH_SHORT).show();
                return TurnInstruction[which_inst];
                //index_instruction++;
            }
        }

        return "";
    }

    private Boolean GetNextInstructions() throws JSONException
    {

        if (recentDistance < 0.0f && instruction_points != null)
        {
            index_instruction = 0;
            Boolean contin = true;
            while (contin)
            {


                if (index_instruction >= instruction_points.length() && !OsrmInstructionsCondition())
                {
                    return false;
                }

                instructionOnIndex = (JSONArray) instruction_points.get(index_instruction);
                String temp = instructionOnIndex.get(0).toString();
                int which_inst = Integer.parseInt(temp);

                if (which_inst <=15)
                {
                    GetNextStatus = false;
                    return true;
                }
                index_instruction++;

            }
        }

        else if (recentDistance >= 0.0f && instruction_points != null)
        {

            //index_instruction++;
            recentDistance = -1.0f;

            Boolean contin = true;
            while (contin)
            {

                if (index_instruction >= instruction_points.length() && !OsrmInstructionsCondition() )
                {
                    return false;
                }

                instructionOnIndex = (JSONArray) instruction_points.get(index_instruction);
                int which_inst = Integer.parseInt(instructionOnIndex.get(0).toString());

                if (which_inst <=15)
                {
                    GetNextStatus = false;
                    return true;
                }

                index_instruction++;

            }
        }
        return false;
    }

    private Boolean OsrmInstructionsCondition() throws JSONException
    {

        if (instruction_points==null)
        {

            if (getFirstOsrmData())
            {
                return true;
            }
            else
            {
                return false;
            }

        }
        else if ( instruction_points.length() <= index_instruction)
        {

            removeFirstOsrmData();

            if (getFirstOsrmData())
            {
                return true;
            }
            else
            {
                return false;
            }

        }

        return true;
    }
}
