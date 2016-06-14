package com.sv2x.googlemap3;

import android.content.Context;
import android.location.Location;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
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

    Boolean first_time = true;

    Boolean GetNextStatus;

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
    public ProvideInstructions()
    {

        //baseContext = baseContext1;
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


            locationB.setLatitude((Double) latlon.get(0));
            locationB.setLongitude((Double) latlon.get(1));


            String latStr_dest = String.valueOf(locationB.getLatitude()) + "," + String.valueOf(locationB.getLongitude());
            String latStr_start = String.valueOf(locationA.getLatitude()) + "," + String.valueOf(locationA.getLongitude());



            String query_string = "http://10.20.17.242:5000/viaroute?loc=" + latStr_start + "&loc=" + latStr_dest + "&instructions=true&compression=false";



            if ( !Check_Existence(query_string, instruction_latLng))
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

    public Boolean Check_Existence(String url_query , String Exist_Instruction) throws IOException
    {
        URL oracle = new URL(url_query);
        URLConnection yc = oracle.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
        String inputLine;
        String JsonO = "";
        while ((inputLine = in.readLine()) != null) {
            JsonO+=inputLine;
            System.out.println(inputLine);
        }

        if (JsonO.indexOf(Exist_Instruction) >= 0)
        {
            return true;
        }

        return false;
    }

}