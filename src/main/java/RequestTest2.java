import entity.BusStop;
import entity.FishnetCell;
import entity.Route;
import entity.Route2;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import utils.HibernateSessionFactoryUtil;

import java.util.ArrayList;
import java.util.List;

import static org.locationtech.jts.algorithm.Centroid.getCentroid;
import static service.BusStopService.getBusStopById;
import static service.FishnetCellService.getFishnetCellById;
import static service.Route2Service.getRoute2ByRId;
import static service.RouteService.getRouteById;
import static service.RouteService.getRouteByRId;

public class RequestTest2 {
    public static void main(String[] args) {

        Route route1 = getRouteById(62370);
        Route route2 = getRouteById(62371);

        //Route route1 = getRouteByRId("62370","4");
        //Route route2 = getRouteByRId("62371","5");

        List<BusStop> stops1 = new ArrayList<>();
        List<BusStop> stops2 = new ArrayList<>();

        List<String> near1 = new ArrayList<>();
        List<String> near2 = new ArrayList<>();

        for(String stop_id: route1.getStops())
        {
            BusStop stop = getBusStopById(Integer.parseInt(stop_id));
            stops1.add(stop);
            near1.add(getBusStopById(Integer.parseInt(stop.getNearestMetro())).getName());
        }
        for(String stop_id: route2.getStops())
        {
            BusStop stop = getBusStopById(Integer.parseInt(stop_id));
            stops2.add(stop);
            near2.add(getBusStopById(Integer.parseInt(stop.getNearestMetro())).getName());
        }

        //---------------
/*
        Route2 route3 = getRoute2ByRId("958","4");
        Route2 route4 = getRoute2ByRId("958","5");

        List<String> stops3 = new ArrayList<>();
        List<String> stops4 = new ArrayList<>();

        List<Double> dist3 = new ArrayList<>();
        List<Double> dist4 = new ArrayList<>();

        for(String stop_id: route3.getStops())
        {
            stops3.add(getBusStopById(Integer.parseInt(stop_id)).getName());
            dist3.add(getBusStopById(Integer.parseInt(stop_id)).getMinMetroDist());
        }
        for(String stop_id: route4.getStops())
        {
            stops4.add(getBusStopById(Integer.parseInt(stop_id)).getName());
            dist4.add(getBusStopById(Integer.parseInt(stop_id)).getMinMetroDist());
        }



 */
        System.out.printf("\nRoute #%s %s:\n",route1.getShortName(),route1.getLongName());
        for(BusStop s: stops1)
            System.out.printf("%s (%d):  %.2f simple    %.2f full   ---> %s\n",
                    s.getName(),s.getId(),s.getTripSimple()/60,s.getTripFull()/60,
                    near1.get(stops1.indexOf(s)));

        System.out.printf("\nRoute #%s %s:\n",route2.getShortName(),route2.getLongName());
        for(BusStop s: stops2)
        System.out.printf("%s (%d):   %.2f simple    %.2f full    ---> %s\n",
                s.getName(),s.getId(),s.getTripSimple()/60,s.getTripFull()/60,
                near2.get(stops2.indexOf(s)));

        /*
        System.out.printf("\n\n\n\nRoute #%s %s %d TRIPS %f INTERVAL:\n",
                route3.getShortName(),route3.getLongName(),route3.getTrips(),route3.getInterval());
        for(String s: stops3)
            System.out.printf("%s -- %f -- %f\n",
                    s,dist3.get(stops3.indexOf(s)),route3.getTimeP()[stops3.indexOf(s)]);

        System.out.printf("\nRoute #%s %s %d TRIPS %f INTERVAL:\n",
                route4.getShortName(),route4.getLongName(),route4.getTrips(),route4.getInterval());
        for(String s: stops4)
            System.out.printf("%s -- %f -- %f\n",
                    s,dist4.get(stops4.indexOf(s)),route4.getTimeP()[stops4.indexOf(s)]);


         */
    }
}
