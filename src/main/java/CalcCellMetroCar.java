import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.util.Parameters;

import com.graphhopper.util.shapes.GHPoint;
import entity.BusStop;
import entity.FishnetCell2;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

import static org.locationtech.jts.algorithm.Centroid.getCentroid;
import static service.BusStopService.getBusStopById;
import static service.FishnetCell2Service.listFishnetCells2;
import static service.FishnetCell2Service.updateFishnetCell2;

// DEPRECATED

public class CalcCellMetroCar {
    public static void main(String[] args) {

        //----- HARDCODE CONTROLS -------
        final int Radius = 500; // meters, looking for stops in this radius
        final String osmFile = "C:/matrix/RU-MOW.osm.pbf";
        final String dir = "local/graphhopper";
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
            if(!cell.getNearestMetro().equals("no routes")&&!cell.getNearestMetro().equals("no stops"))
            {
                BusStop stop = getBusStopById(Integer.parseInt(cell.getNearestMetro()));

                // now get coordinates & run GH
                double destLon = stop.getGeom().getCoordinate().getX();
                double destLat = stop.getGeom().getCoordinate().getY();

                double startLon = getCentroid(cell.getGeom()).getX();
                double startLat = getCentroid(cell.getGeom()).getY();

                GHRequest req = new GHRequest().setAlgorithm(Parameters.Algorithms.ASTAR_BI);
                req.setProfile("car1");
                req.addPoint(new GHPoint(startLat, startLon));
                req.addPoint(new GHPoint(destLat, destLon));

                GHResponse res = hopper.route(req);

                if (res.hasErrors()) {
                    throw new RuntimeException(res.getErrors().toString());
                }

                cell.setMetroCar(res.getBest().getTime()/1000);
                updateFishnetCell2(cell);
            }
        }

        System.out.printf("\n\n===== Calculated metro distances for %d cells in %d seconds ======\n\n"
                , cells.size(), (System.currentTimeMillis()-startTime)/1000);
    }
}
