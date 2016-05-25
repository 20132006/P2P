package com.sv2x.googlemap3;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by cjoo on 4/25/15.
 */
public class User
{

    // cjoo: id
    String id;
    CharSequence name;

    User()
    {
        id = "";
        name = "";


        mySpaceId = "";
        //mCurrentCameraLevel = 17;     // initial camera level
        mCurrentCameraLevel = 18;
        mLatLng = null;

        mLeardersLastUpdateTime = 0;
        mLeadersLastLatLng = null;

        try
        {
            //serverAddr = InetAddress.getByName("192.168.1.103");140.254.203.132
            //serverAddr = InetAddress.getByName("192.168.1.102");
            serverAddr = InetAddress.getByName("114.70.9.118"); //out of UNIST
            //serverAddr = InetAddress.getByName("10.20.16.131");//in UNIST
            //serverAddr = InetAddress.getByName("10.20.17.247");//My lab LINUX server in UNIST
            //serverAddr = InetAddress.getByName("10.20.17.4");//My lab MAC server in UNIST
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
            isConnected = false;
        }

        serverPort = 8002;
        debugMsg = "null";
        isConnected = false;
        isLeader = false;
        firstLeadersMark=true;

        isSendingUpates = false;
        updateInterval = 5;

    }

    // cjoo: location
    boolean mRequestLocationUpdates;        // true if location is being updated, false otherwise
    Location mLastLocation;                 // variable for location info.
    long mLastUpdateTime;                   // variable for location update time
    long mCurrentCameraLevel;               // variable for camera view level
    LatLng mLatLng;                         // variable for (latitude, longitude)


    LatLng mLeadersLastLatLng;              //variable for (latitude, longitude) last updated location
    String LeaderLocations;                 //variable for leader's locations last 15sec
    Marker LeadersMark;                     //periodically marking positions
    boolean firstLeadersMark;               //marking initial location of leaders
    boolean activeToUseLeadersLocations;    //whether we can use leaders locations
    long mLeardersLastUpdateTime;           //variable for leaders location update time

    // cjoo: network
    InetAddress myAddr;
    InetAddress serverAddr;
    int serverPort;
    boolean isConnected;                    //isUser Connected to the internet
    boolean isLeader;                       //whether user leader or not

    // cjoo: operation
    boolean isSendingUpates;                // true if update thread is running
    int updateInterval;                     // seconds

    // cjoo: App operations
    String mySpaceId;                       // A user can belong to one space at a time

    // cjoo: debug
    String debugMsg;

    public void getLocalIpAddress()
    {
        try
        {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces)
            {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs)
                {
                    if (!addr.isLoopbackAddress())
                    {
                        myAddr = addr;
                    }
                }
            }
        }
        catch (Exception ex) { } // for now eat exceptions
        return;
    }

    public void send( final DatagramSocket skt, final String msg ) {
        if( serverAddr == null || serverPort == 0 ) return;
        final DatagramPacket pkt;
        pkt = new DatagramPacket( msg.getBytes(), msg.length(), serverAddr, serverPort );

        new Thread(new Runnable() {
            public void run() {
                try {
                    skt.send( pkt );
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // send location information
    public void sendLocation( DatagramSocket skt, String MSGTYPE) {
        final CharSequence text;
        text = id + ";"
                + MSGTYPE + ";"
                + String.valueOf(mLastLocation.getLatitude()) + ";"
                + String.valueOf(mLastLocation.getLongitude()) + ";"
                + String.valueOf(mLastUpdateTime) + ";";
        send(skt, text.toString());
    }
};

class Users
{
    List<User> uList = new ArrayList<User>();

    public void addUser( User u ) { uList.add(u); }
    public void removeUser( User u ) { uList.remove(u); }
    public User findUser( String uid ) {
        for(User u : uList ) {
            if( u.id.equals(uid) ) {
                return u;
            }
        }
        return null;
    }
};

