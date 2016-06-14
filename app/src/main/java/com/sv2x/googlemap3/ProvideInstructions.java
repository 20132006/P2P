package com.sv2x.googlemap3;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;

/**
 * Created by netlab on 6/3/16.
 */
public class ProvideInstructions
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

    HttpAsyncTask httpAsyncTask;

    Boolean first_time = true;

    Boolean GetNextStatus;

    String finished;

    private Integer index_instruction;

    private Context baseContext;

    public ProvideInstructions(Context baseContext1)
    {

        baseContext = baseContext1;
        OsrmQueryData = new LinkedList<String>() ;
        geometry_points = null;
        instruction_points = null;
        instructionOnIndex = null;
        index_instruction = 0;
        GetNextStatus = true;
        first_time = true;
    }

    public void setOsrmQueryData(String QueryData)
    {
        OsrmQueryData.addLast(QueryData);
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



    public String QueryInstructions(Location FollowersLoc) throws JSONException, IOException
    {
        if (OsrmInstructionsCondition())
        {
            if (GetNextStatus && !GetNextInstructions())
            {
                return "";
            }

            instructionOnIndex = (JSONArray) instruction_points.get(index_instruction);
            int linked_instructions = (int) instructionOnIndex.get(3);
            int which_inst = Integer.parseInt(instructionOnIndex.get(0).toString());

            JSONArray latlon = null;
            latlon = (JSONArray) geometry_points.get(linked_instructions);

            String instruction_latLng = String.valueOf(latlon.get(0)) + "," + String.valueOf(latlon.get(1));

            Location locationA;
            Location locationB = new Location("point B");

            locationA = FollowersLoc;


            JSONArray latlon1 = null;
            latlon1 = (JSONArray) geometry_points.get(geometry_points.length()-1);

            locationB.setLatitude((Double) latlon1.get(0));
            locationB.setLongitude((Double) latlon1.get(1));


            String latStr_dest = String.valueOf(locationB.getLatitude()) + "," + String.valueOf(locationB.getLongitude());
            String latStr_start = String.valueOf(locationA.getLatitude()) + "," + String.valueOf(locationA.getLongitude());




            String query_string = "http://router.project-osrm.org/match?loc="+ latStr_start +"&t=1424684612loc="+ latStr_dest +"&t=1424684616&instructions=true&compression=false";//"http://router.project-osrm.org/viaroute?loc=" + latStr_start + "&loc=" + latStr_dest + "&instructions=true&compression=false";

            finished="";

            httpAsyncTask = (HttpAsyncTask) new HttpAsyncTask(this);
            httpAsyncTask.execute(query_string);

            while (finished=="")
            {

            }

            if ( !Check_Existence(Double.parseDouble( String.valueOf( latlon.get(0)) ),Double.parseDouble( String.valueOf( latlon.get(1)) )))
            {
                GetNextStatus=true;
                index_instruction++;
                return QueryInstructions(FollowersLoc);
            }

            Location Instruction_point;

            Instruction_point = new Location("Instruction");

            Instruction_point.setLatitude((Double) latlon.get(0));
            Instruction_point.setLongitude((Double) latlon.get(1));


            Toast.makeText(baseContext, TurnInstruction[which_inst] + " " + FollowersLoc.distanceTo(Instruction_point), Toast.LENGTH_SHORT).show();
            return TurnInstruction[which_inst] + "," + String.valueOf(FollowersLoc.distanceTo(Instruction_point));
        }
        return "";
    }

    private Boolean GetNextInstructions() throws JSONException
    {

        if (instruction_points != null )
        {
            first_time = false;
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
                if ( ( 0<=which_inst && which_inst <=8 ) || ( 11<=which_inst && which_inst<=13 ) )
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
        else if ( index_instruction >= instruction_points.length())
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
        return true;
    }




    public Boolean Check_Existence(Double lat1, Double lgt1) throws IOException
    {
        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(finished);
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
        JSONArray array_of_points = null;
        if (jsonObject2!=null)
        {
            try {
                array_of_points = (JSONArray) jsonObject2.get("geometry");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


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

            Location location2 = new Location("blabla");

            location2.setLatitude(lat);
            location2.setLongitude(lgt);

            Location needed = new Location("blabla");

            needed.setLatitude(lat1);
            needed.setLongitude(lgt1);

            double dist = location2.distanceTo(needed);

            if ( dist < 2.0)
            {
                return true;
            }
        }
        return false;

    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String>
    {
        public ProvideInstructions activity;
        public HttpAsyncTask(ProvideInstructions a)
        {
            this.activity = a;
        }

        @Override
        protected String doInBackground(String... urls) {

            try {
                return GET(urls[0]);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        private String GET(String url) throws JSONException {
            InputStream inputStream = null;
            String result = "";

            try {

                // create HttpClient
                HttpClient httpclient = new DefaultHttpClient();

                // make GET request to the given URL
                HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

                // receive response as inputStream
                inputStream = httpResponse.getEntity().getContent();

                // convert inputstream to string
                if(inputStream != null) {
                    result = convertInputStreamToString(inputStream);
                }
                else
                    result = "Did not work!";


            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
            }
            return result;
        }

        private String convertInputStreamToString(InputStream inputStream) throws IOException {
            BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
            String line = "";
            String result = "";
            while((line = bufferedReader.readLine()) != null)
                result += line;
            inputStream.close();
            finished = result;
            return result;
        }

        // onPostExecute displays the results of the AsyncTask.

        protected void onPostExecute(String result)
        {
            finished=result;
        }
    }


}