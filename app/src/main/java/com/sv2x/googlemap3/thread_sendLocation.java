package com.sv2x.googlemap3;

import java.net.DatagramSocket;

/**
 * Created by netlab on 6/3/16.
 */

public class thread_sendLocation implements Runnable {
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