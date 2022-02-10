import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.shapes.GHPoint;
import entity.FishnetCell2;
import entity.Metro;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import utils.HibernateSessionFactoryUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.locationtech.jts.algorithm.Centroid.getCentroid;
import static service.FishnetCell2Service.listFishnetCells2;
import static service.FishnetCell2Service.updateFishnetCell2;

public class CalcCellMetroAll {
    public static void main(String[] args) {

        //----- HARDCODE CONTROLS -------
        final int Radius = 500; // meters, looking for stops in this radius
        final int RadiusMetro = 4000; // meters, looking for metro in this radius
        final int PedestrianSpeed = 1; // in m/s, equals 3.6 km/h but we have air distances so ok
        final String osmFile = "C:/matrix/RU-MOW.osm.pbf";
        final String dir = "local/graphhopper";
        final double speedRatio = 1.5;
        final int SnapDistance = 300; // no road radius
        //----------------------

        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        List<FishnetCell2> cells = listFishnetCells2();

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

        for(FishnetCell2 cell: cells)
        {
            Query query = session.createQuery(
                    "select p.tripSimple, p.tripFull, p.nearestMetro," +
                            "distance(transform(p.geom,98568), transform(:cell,98568)) from BusStop p " +
                            "where dwithin(transform(p.geom,98568), transform(:cell,98568), :radius) = true and p.active = true");
            query.setParameter("cell", gf.createPoint(getCentroid(cell.getGeom())));
            query.setParameter("radius", Radius);
            List<Object[]> stops = (List<Object[]>)query.list();

            if(!stops.isEmpty())
            {
                double minSimple = Double.POSITIVE_INFINITY;
                double minFull = Double.POSITIVE_INFINITY;
                String nearest = "no routes";
                for(Object[] stop: stops)
                {
                    if((Double)stop[0]<minSimple)
                        minSimple=(Double)stop[0];

                    if((Double)stop[1]+(Double)stop[3]*PedestrianSpeed<minFull)
                    {
                        minFull=(Double)stop[1]+(Double)stop[3]*PedestrianSpeed;
                        nearest=(String) stop[2];
                    }
                }
                cell.setMetroSimple(minSimple);
                cell.setMetroFull(minFull);
                cell.setNearestMetro(nearest);
            }
            else
            {
                cell.setMetroSimple(Double.POSITIVE_INFINITY);
                cell.setMetroFull(Double.POSITIVE_INFINITY);
                cell.setNearestMetro("no stops");
            }

            // check road availability
            double startLon = getCentroid(cell.getGeom()).getX();
            double startLat = getCentroid(cell.getGeom()).getY();

            Snap snap = hopper.getLocationIndex().findClosest(startLat,startLon, EdgeFilter.ALL_EDGES);
            if(snap.isValid()&&snap.getQueryDistance()<SnapDistance) {


                query = session.createQuery(
                        "select p from Metro p " +
                                "where dwithin(transform(p.geom,98568), transform(:cell,98568), :radius) = true ", Metro.class);
                query.setParameter("cell", gf.createPoint(getCentroid(cell.getGeom())));
                query.setParameter("radius", RadiusMetro);

                List<Metro> metros = query.getResultList();

                List<Double> distances = new ArrayList<>();
                boolean isRoad = true;

                if (!metros.isEmpty()) {
                    for (Metro metro : metros) {
                        // now get coordinates & run GH
                        double destLon = metro.getGeom().getCoordinate().getX();
                        double destLat = metro.getGeom().getCoordinate().getY();


                        GHRequest req = new GHRequest().setAlgorithm(Parameters.Algorithms.ASTAR_BI);
                        req.setProfile("car1");
                        req.addPoint(new GHPoint(startLat, startLon));
                        req.addPoint(new GHPoint(destLat, destLon));

                        GHResponse res = hopper.route(req);

                        if (res.hasErrors()) {
                            isRoad = false;
                            //throw new RuntimeException(res.getErrors().toString());
                        } else
                            distances.add((double) (res.getBest().getTime() / 1000) * speedRatio);
                    }


                    if (isRoad) {
                        Collections.sort(distances);
                        cell.setMetroCar(distances.get(0));
                    } else {
                        cell.setMetroCar(Double.POSITIVE_INFINITY);
                        cell.setNearestMetro("no nothing");
                    }
                } else {
                    cell.setMetroCar(Double.POSITIVE_INFINITY);
                    cell.setNearestMetro("no metro");
                }
            }
            else
            {
                cell.setMetroCar(Double.POSITIVE_INFINITY);
                cell.setNearestMetro("no road");
            }

            updateFishnetCell2(cell);
        }

        System.out.printf("\n\n===== Calculated metro distances for %d cells in %d seconds ======\n\n"
                , cells.size(), (System.currentTimeMillis()-startTime)/1000);
    }
}
