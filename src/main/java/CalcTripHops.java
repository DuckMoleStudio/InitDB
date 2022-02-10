import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.shapes.GHPoint;
import entity.BusStop;
import entity.Route2;

import java.util.ArrayList;
import java.util.List;

import static service.BusStopService.getBusStopById;
import static service.Route2Service.listRoutes2;
import static service.Route2Service.updateRoute2;

public class CalcTripHops {
    public static void main(String[] args) {

        //----- HARDCODE CONTROLS -------
        double speedRatio = 1.5;
        final String osmFile = "C:/matrix/RU-MOW.osm.pbf";
        final String dir = "local/graphhopper";
        //----------------------

        long startTime = System.currentTimeMillis();
        int badCount=0;

        List<Route2> routes = listRoutes2();

        // GH preparation
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFile);
        hopper.setGraphHopperLocation(dir);

        hopper.setProfiles(
                new Profile("car1").setVehicle("car").setWeighting("fastest").setTurnCosts(false),
                new Profile("car2").setVehicle("car").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 60)
        );
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car1"), new CHProfile("car2"));
        hopper.importOrLoad();

        for(Route2 route: routes) {
            List<Double> hops = new ArrayList<>();
            for (int i = 0; i < route.getStops().length - 1; i++) {

                // now get coordinates & run GH
                BusStop stop1 = getBusStopById(Integer.parseInt(route.getStops()[i]));
                BusStop stop2 = getBusStopById(Integer.parseInt(route.getStops()[i + 1]));


                double destLon = stop2.getGeom().getCoordinate().getX();
                double destLat = stop2.getGeom().getCoordinate().getY();

                double startLon = stop1.getGeom().getCoordinate().getX();
                double startLat = stop1.getGeom().getCoordinate().getY();

                GHRequest req = new GHRequest().setAlgorithm(Parameters.Algorithms.ASTAR_BI);
                req.setProfile("car1");
                req.addPoint(new GHPoint(startLat, startLon));
                req.addPoint(new GHPoint(destLat, destLon));

                //req.setCurbsides(Arrays.asList("right", "right"));
                req.putHint("instructions", false);
                req.putHint("calc_points", false);
                //req.putHint(Parameters.Routing.FORCE_CURBSIDE, false);

                GHResponse res = hopper.route(req);

                if (res.hasErrors()) {
                    System.out.println(route.getStops()[i] + " -> " + route.getStops()[i+1]);
                    hops.add(60d);
                    badCount++;
                    //throw new RuntimeException(res.getErrors().toString());
                }
                else
                hops.add((double) (res.getBest().getTime() / 1000));
            }
            double[] dd = new double[hops.size()];
            for(int i=0;i< hops.size();i++) dd[i]=hops.get(i)*speedRatio;
            route.setHops(dd);
            updateRoute2(route);
        }

        System.out.printf("\n\n===== Calculated hop times for %d routes in %d seconds with %d errors ======\n\n"
                , routes.size(), (System.currentTimeMillis()-startTime)/1000, badCount);
    }
}
