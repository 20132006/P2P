package com.sv2x.googlemap3;

import android.content.Context;
import android.location.Location;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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


    private float recentDistance;


    Location lastLocation = null;


    Boolean GetNextStatus;

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


            recentDistance = locationA.distanceTo(locationB);


            if ( lastLocation!=null && locationB.distanceTo(lastLocation) < locationB.distanceTo(FollowersLoc)  )
            {
                GetNextStatus=true;
                index_instruction++;
                return QueryInstructions(FollowersLoc);
            }


            if ( lastLocation == null || (lastLocation != null && FollowersLoc.distanceTo(lastLocation) > 3.0f))
                lastLocation = FollowersLoc;

            Toast.makeText(baseContext, TurnInstruction[which_inst] + " " + recentDistance, Toast.LENGTH_SHORT).show();
            //if (recentDistance <= 30.0f)
            return TurnInstruction[which_inst];
            //return "";

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

                if ( ( 0<=which_inst && which_inst <=8 ) || ( 11<=which_inst && which_inst<=13 ) )
                {
                    GetNextStatus = false;
                    return true;
                }
                index_instruction++;

            }
        }

        else if (recentDistance >= 0.0f && instruction_points != null)
        {

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
        else if ( instruction_points.length() <= index_instruction)
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
}