package com.sv2x.googlemap3;

import android.os.AsyncTask;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.LinkedList;

/**
 * Created by netlab on 6/3/16.
 */
public class Receive implements Runnable {

    JSONArray array_of_points;
    JSONObject jsonObject = null;
    String followersID;
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

    public Receive(DatagramSocket sck, User state, Users uList, MainActivity myActivity)
    {
        this.rSocket = sck;
        this.MyState = state;
        this.uList = uList;
        updateThread = null;
        stopRequested = false;
        activity = myActivity;
        map = ((SupportMapFragment) activity.getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        complete_msg="";
        OsrmQueryData = new LinkedList<String>() ;
    }

    public User getUser()
    {
        return MyState;
    }

    public void requestStop()
    {
        stopRequested = true;
        if (updateThread != null)
        {
            updateThread.requestStop();
        }

    }

    public void run()
    {
        while (stopRequested == false)
        {
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

    public void handlePacket(DatagramPacket pkt)
    {
        String msg;
        int index;
        String msgType;

        msg = new String(rPacket.getData(), 0, pkt.getLength());

        index = msg.indexOf(";");
        if (index <= 0)
        {
            return;
        }
        else
        {
            msgType = msg.substring(0, index);
            msg = msg.substring(index + 1, msg.length());

        }

        // now received something, which means connectivity
        // if connected (to the Server), then start sending location info.
        if (updateThread == null || wrapUpdateThread.getState() == Thread.State.TERMINATED)
        {
            MyState.isSendingUpates = true;
            updateThread = new thread_sendLocation(MyState, rSocket);
            wrapUpdateThread = new Thread(updateThread);
            wrapUpdateThread.start();
        }

        if (msgType.equals("New User OK"))// response to New User registration
        {
            // do nothing
        }
        else if (msgType.equals("Create Space OK"))// response to Create Space
        {
            // remember space id
            MyState.isLeader = true;
        } else if (msgType.equals("Join Space OK"))// response to Join Space
        {
            // remember space id
            if (MyState.isLeader == true)
            {
                MyState.isLeader=false;
                MyState.mLeardersLastUpdateTime = 0;
                MyState.firstLeadersMark = true;
                MyState.mLeadersLastLatLng = null;
            }
        }
        else if (msgType.equals("Location Update"))// periodic info.
        {
            // location & user id
            updateUserInfo(msg, uList);
        }
        else if (msgType.equals(("Leader's Locations")))
        {
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

    class HttpAsyncTask extends AsyncTask<String, Void, String>
    {
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
            if (index < 0)
            {
                return;
            }
            else
            {
                LeadersID = message.substring(0, index);
                message = message.substring(index + 1);
            }

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
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            LatLng location = null;
            double lat = 0., lgt=0.;

            for (int i = 0; i < array_of_points.length(); i++)
            {

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
                if (MyState.isLeader == false)
                {
                    if (MyState.firstLeadersMark == true)
                    {
                        map.addMarker(new MarkerOptions().position(location).title("Leader's Locatoin" + LeadersID));
                        MyState.LeadersMark = map.addMarker(new MarkerOptions().position(location).title("Leader's Locatoin" + LeadersID));
                        MyState.mLeadersLastLatLng = location;
                        MyState.firstLeadersMark = false;
                    }
                    else
                    {
                        PolylineOptions line = new PolylineOptions()
                                .add(MyState.mLeadersLastLatLng)
                                .add(location);
                        map.addPolyline(line);
                        MyState.mLeadersLastLatLng = location;
                    }

                    if (MyState.LeadersMark != null)
                    {

                        MyState.LeadersMark.remove();
                        MyState.LeadersMark = map.addMarker(new MarkerOptions().position(location).title("Leader's Locatoin" + LeadersID));
                    }
                }
            }

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
                if ( MyState.mLeardersLastUpdateTime < Long.parseLong(update_TIME) )
                {
                    if ( MyState.isLeader == false )
                    {
                        if ( MyState.firstLeadersMark == true )
                        {
                            map.addMarker(new MarkerOptions().position(location).title("Leader's Locatoin" + LeadersID));
                            MyState.LeadersMark = map.addMarker(new MarkerOptions().position(location).title("Leader's Locatoin" + LeadersID));
                            MyState.mLeadersLastLatLng = location;
                            MyState.firstLeadersMark = false;
                        }
                        else
                        {
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

    public void updateUserInfo(String msg, Users uList)
    {
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

        while (text.length() > 10)
        {
            index = text.indexOf(";");
            if (index <= 0) {
                return;
            } else {
                clientId = text.substring(0, index);
                text = text.substring(index + 1, text.length());
            }

            // the first client is the space owner
            if (spaceOwner.equals(""))
            {
                spaceOwner = clientId;
            }

            // if non-existing user, add it
            user = uList.findUser(clientId);
            if (user == null)
            {
                user = new User();
                user.id = clientId;
                uList.addUser(user);
            }

            index = text.indexOf(";");
            if (index <= 0)
            {
                return;
            }
            else
            {
                cLatitude = text.substring(0, index);
                text = text.substring(index + 1, text.length());
            }
            index = text.indexOf(";");
            if (index <= 0)
            {
                return;
            }
            else
            {
                cLongitude = text.substring(0, index);
                text = text.substring(index + 1, text.length());
            }
            index = text.indexOf(";");
            if (index <= 0)
            {
                return;
            }
            else
            {
                cLastUpdateTime = text.substring(0, index);
                text = text.substring(index + 1, text.length());
            }
            user.mLatLng = new LatLng(Double.parseDouble(cLatitude), Double.parseDouble(cLongitude));
            user.mLastUpdateTime = Long.parseLong(cLastUpdateTime);
        }
    }

}