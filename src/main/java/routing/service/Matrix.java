package routing.service;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;

import com.graphhopper.routing.util.BusFlagEncoder;
import com.graphhopper.routing.util.NGPTFlagEncoder;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.StopWatch;
import routing.entity.WayPoint;
import routing.entity.storage.MatrixElement;
import routing.entity.storage.MatrixLineMap;
import routing.entity.storage.TimeDistancePair;

import java.util.*;

public class Matrix {

    public static Map<WayPoint, MatrixLineMap> FillGHMulti4Map(
            List<WayPoint> wayPoints, String osmFile, String dir, boolean turns, boolean curbs)
    // using Graph Hopper on real map, with 4 threads, store in HashMap
    {
        StopWatch sw = new StopWatch().start();
        Map<WayPoint, MatrixLineMap> matrix = new HashMap<>();


        //---
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFile);
        hopper.setGraphHopperLocation(dir);

        hopper.getEncodingManagerBuilder().add(new BusFlagEncoder(5,5,1));
        hopper.getEncodingManagerBuilder().add(new NGPTFlagEncoder(5,5,1));

        hopper.setProfiles(
                new Profile("ngpt1").setVehicle("ngpt").setWeighting("fastest").setTurnCosts(false),
                new Profile("ngpt2").setVehicle("ngpt").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 60),
                new Profile("bus1").setVehicle("bus").setWeighting("fastest").setTurnCosts(false),
                new Profile("bus2").setVehicle("bus").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 60),
                new Profile("car1").setVehicle("car").setWeighting("fastest").setTurnCosts(false),
                new Profile("car2").setVehicle("car").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 60)
        );
        hopper.getCHPreparationHandler().setCHProfiles(
                new CHProfile("ngpt1"),
                new CHProfile("ngpt2"),
                new CHProfile("bus1"),
                new CHProfile("bus2"),
                new CHProfile("car1"),
                new CHProfile("car2")
        );
        hopper.importOrLoad();
        //---


        String profile = "ngpt1";
        if(turns) profile = "ngpt2";

        int section = (int)Math.ceil(wayPoints.size() / 4);

        String finalProfile = profile;
        class sub {
            void fill (int from, int to){
                for(int i=from; i < to; i++)
                {
                    MatrixLineMap line = new MatrixLineMap();
                    line.setDistances(new HashMap<>());

                    for(int j=0; j < wayPoints.size(); j++)
                    {
                        if(i==j)
                        {
                            line.getDistances().put(wayPoints.get(j),
                                    new TimeDistancePair(Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY));
                        }
                        else
                        {
                            GHRequest req = new GHRequest(
                                    wayPoints.get(i).getLat()
                                    , wayPoints.get(i).getLon()
                                    , wayPoints.get(j).getLat()
                                    , wayPoints.get(j).getLon())
                                    .setProfile(finalProfile)
                                    .setAlgorithm(Parameters.Algorithms.ASTAR_BI);

                            if(curbs)
                            {
                                req.setCurbsides(Arrays.asList("right", "right"));
                                req.putHint("u_turn_costs", 6000);
                            }

                            req.setSnapPreventions(Arrays.asList("bridge", "tunnel"));
                            req.putHint("instructions", false);
                            req.putHint("calc_points", false);
                            req.putHint(Parameters.Routing.FORCE_CURBSIDE, false);

                            GHResponse res = hopper.route(req);

                            double distance = Math.round(res.getBest().getDistance()); // to fit in storage line
                            double time = res.getBest().getTime();

                            if (res.hasErrors()) {
                                throw new RuntimeException(res.getErrors().toString());
                            }

                            line.getDistances().put(wayPoints.get(j),new TimeDistancePair(time,distance));
                        }
                    }

                    int div = 50;
                    if(wayPoints.size() <= 50) div = wayPoints.size()-1;
                    if(i%(wayPoints.size()/div) == 0)
                        System.out.print("."); // progress indicator, optimized for 50 dots or fewer

                    synchronized (matrix) {matrix.put(wayPoints.get(i),line);}
                }
            }
        }

        Thread one = new Thread(() -> {
            sub sub1 = new sub();
            sub1.fill(0,section);
        });

        Thread two = new Thread(() -> {
            sub sub2 = new sub();
            sub2.fill(section,section*2);
        });

        Thread three = new Thread(() -> {
            sub sub3 = new sub();
            sub3.fill(section*2,section*3);
        });

        Thread four = new Thread(() -> {
            sub sub4 = new sub();
            sub4.fill(section*3,wayPoints.size());
        });

// start threads
        one.start();
        two.start();
        three.start();
        four.start();

// Wait for threads above to finish
        try{
            one.join();
            two.join();
            three.join();
            four.join();
        }
        catch (InterruptedException e)
        {
            System.out.println("Interrupt Occurred");
            e.printStackTrace();
        }
        System.out.println("\nMatrix calculated in: " + sw.stop().getSeconds() + " s\n");
        return matrix;
    }




    public static double DistanceBetweenMap(WayPoint start, WayPoint end, Map<WayPoint,MatrixLineMap> matrix)
    {
        return matrix.get(start).getDistances().get(end).getDistance();
    }

    public static double TimeBetweenMap(WayPoint start, WayPoint end, Map<WayPoint,MatrixLineMap> matrix)
    {
        return matrix.get(start).getDistances().get(end).getTime();
    }

    public static MatrixElement NearestMap(
            WayPoint start,
            Map<WayPoint,MatrixLineMap> matrix,
            List<WayPoint> existing)
    {
        MatrixLineMap ml = matrix.get(start);
        Set<Map.Entry<WayPoint,TimeDistancePair>> distances = ml.getDistances().entrySet();
        MatrixElement result = new MatrixElement();
        double minTime = Double.POSITIVE_INFINITY;

        for (Map.Entry<WayPoint,TimeDistancePair> me: distances)
        {
            if(me.getValue().getTime() < minTime && existing.contains(me.getKey()))
            {
                minTime = me.getValue().getTime();
                result.setDistance(me.getValue().getDistance());
                result.setTime(me.getValue().getTime());
                result.setWayPoint(me.getKey());
            }
        }


        return result;
    }

    public static MatrixElement NearestMapFrom(
            WayPoint end,
            Map<WayPoint,MatrixLineMap> matrix,
            List<WayPoint> existing)
    {
        double minTime = Double.POSITIVE_INFINITY;
        MatrixElement result = new MatrixElement();
        for(WayPoint wp: existing)
        {
            double tryTime = matrix.get(wp).getDistances().get(end).getTime();
            double tryDistance = matrix.get(wp).getDistances().get(end).getDistance();
            if (tryTime<minTime)
            {
                minTime = tryTime;
                result.setDistance(tryDistance);
                result.setTime(tryTime);
                result.setWayPoint(wp);
            }
        }


        return result;
    }



}
