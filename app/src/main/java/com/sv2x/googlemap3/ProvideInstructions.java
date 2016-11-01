package com.sv2x.googlemap3;

import android.content.Context;
import android.location.Location;
import java.util.Vector;

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

    private double close_lat, close_lon,fLat,fLng;


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
            close_lat = starting.getLatitude();// - extra_lat;
            close_lon = starting.getLongitude();// - extra_lon;
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


    public String QueryInstructions( Vector<Double> learder_lat, Vector<Double> learder_lon, Vector<Integer> instruction_code, Vector<Integer> instruction_map, Location location)
    {
        int i,indexOF = 0;
        double smallest_cost = -1.0;
        double cur_cost;
        Location str = new Location("Starting");
        Location end = new Location("Ending");

        for (i=0;i<learder_lat.size()-1;i++)
        {
            str.setLatitude(learder_lat.elementAt(i));
            str.setLongitude(learder_lon.elementAt(i));

            end.setLatitude(learder_lat.elementAt(i + 1));
            end.setLongitude( learder_lon.elementAt(i+1));

            cur_cost = MatchingCost(str,end,location);
            if (smallest_cost < 0 )
            {
                smallest_cost = cur_cost;
                indexOF = i;
                fLat = close_lat;
                fLng = close_lon;
            }
            else if (smallest_cost > cur_cost)
            {
                smallest_cost = cur_cost;
                indexOF = i;
                fLat = close_lat;
                fLng = close_lon;
            }
        }

        for (i=0;i<instruction_map.size();i++)
        {
            if (instruction_map.elementAt(i) > indexOF)
            {
                return TurnInstruction[instruction_code.elementAt(i)];
            }
        }
        return null;
    }
}