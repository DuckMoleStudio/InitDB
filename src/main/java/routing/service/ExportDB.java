package routing.service;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.util.BusFlagEncoder;
import com.graphhopper.routing.util.NGPTFlagEncoder;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;
import entity.RouteName;
import entity.Trip;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import routing.entity.WayPoint;
import routing.entity.result.Itinerary;
import routing.entity.result.Result;
import routing.entity.storage.MatrixLineMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static loader.Calculation.*;
import static routing.service.Matrix.DistanceBetweenMap;
import static routing.service.Matrix.TimeBetweenMap;
import static service.RouteNameService.updateRouteName;
import static service.TripService.updateTrip;

public class ExportDB {
    public static void ExportRoutes(int versionId,
                                    Result result,
                                    Map<WayPoint, MatrixLineMap> matrix,
                                    String osmFile,
                                    String dirGH)
    {
        final int MetroCriteria = 150; // meters, if stop is within, then it's a metro stop
        final int StopDelay = 30; // sec, time loss for stopping
        final int PedestrianSpeed = 1; // in m/s, 1 equals 3.6 km/h but we have air distances so ok
        final int IntervalDummy = 600; // in case not available
        final int Radius = 500; // meters, looking for stops in this radius
        final double speedRatio = 2.0;
        final int RadiusMetro = 6000; // meters, looking for metro in this radius
        final int SnapDistance = 500; // no road radius

        int countRoutes=0;
        int countTrips=0;
        long startTime = System.currentTimeMillis();

        Map<Integer, RouteName> routes = new HashMap<>();

        for(Itinerary it: result.getItineraries())
        {

            if(!routes.containsKey(it.getRoute()))
            {
                RouteName route = new RouteName();
                route.setShortName("M-"+it.getRoute());
                route.setLongName(it.getName());
                route.setId(versionId*100000+it.getRoute());
                route.getTrips().put(versionId,new ArrayList<>());
                routes.put(it.getRoute(),route);
                countRoutes++;
            }

            RouteName route = routes.get(it.getRoute());
            Trip trip = new Trip();
            trip.setId(versionId*10000000+it.getRoute()*1000+it.getId()*10+it.getDir());
            route.getTrips().get(versionId).add(trip.getId());
            trip.setDir(String.valueOf(it.getDir()));
            trip.setRid(String.valueOf(route.getId()));
            trip.setVersions(new HashMap<>()); // we assume it will exist in this version only!
            trip.getVersions().put(versionId,true);
            trip.setInterval(IntervalDummy);

            // stops
            List<String> stops = new ArrayList<>();
            for (WayPoint wp: it.getWayPointList())
                stops.add(String.valueOf(wp.getIndex()));
            trip.setStops(stops.toArray(new String[0]));

            // hops & totals
            double totalTime = 0;
            double totalDistance = 0;
            List<WayPoint> wps = it.getWayPointList();
            double[] hops = new double[wps.size()-1];
            for (int i = 0; i < wps.size()-1; i++)
            {
                double hop = (TimeBetweenMap(wps.get(i), wps.get(i+1),matrix)/1000)*speedRatio;
                totalTime+=hop;
                totalDistance+=DistanceBetweenMap(wps.get(i), wps.get(i+1),matrix);
                hops[i]=hop;
            }
            trip.setHops(hops);
            trip.setTotalTime(totalTime);
            trip.setTotalDistance(totalDistance);

            // geometry
            //--- gh prep
            GraphHopper hopper = new GraphHopper();
            hopper.setOSMFile(osmFile);
            hopper.setGraphHopperLocation(dirGH);

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

            //--- routing
            GHRequest req = new GHRequest().setAlgorithm(Parameters.Algorithms.ASTAR_BI);
            req.setProfile("ngpt2");

            List<String> curbSides = new ArrayList<>();
            for (WayPoint wp : wps)
            {
                req.addPoint(new GHPoint(wp.getLat(), wp.getLon()));
                curbSides.add("right");
            }
            req.setCurbsides(curbSides);
            req.putHint(Parameters.Routing.FORCE_CURBSIDE, false);

            GHResponse res = hopper.route(req);

            if (res.hasErrors()) {
                throw new RuntimeException(res.getErrors().toString());
            }
            PointList pl = res.getBest().getPoints();

            //--- geometries themselves
            List<LineString> lines = new ArrayList<>();
            GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);
            for (int i = 0; i < pl.size()-1; i++)
            {
                Coordinate[] cc = new Coordinate[2];
                cc[0] = new Coordinate(pl.getLon(i),pl.getLat(i));
                cc[1] = new Coordinate(pl.getLon(i+1),pl.getLat(i+1));
                lines.add(gf.createLineString(cc));
            }
            trip.setGeomML(gf.createMultiLineString(lines.toArray(new LineString[0])));

            // and finally
            updateTrip(trip);
            countTrips++;
        }

        for(Map.Entry<Integer,RouteName> me: routes.entrySet())
            updateRouteName(me.getValue());


        System.out.printf("\n\n===== Exported %d routes & %d trips in %d seconds ======\n\n"
                , countRoutes, countTrips, (System.currentTimeMillis()-startTime)/1000);

        CalcStopToMetroVer(MetroCriteria, StopDelay, PedestrianSpeed, IntervalDummy, versionId);
        CalcCellMinDistToStopHS(versionId,true,6);
        CalcCellMetroAllHS(Radius, RadiusMetro, PedestrianSpeed, osmFile, dirGH, speedRatio, SnapDistance, versionId,true,6);
    }
}
