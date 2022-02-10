import entity.BusStop;
import entity.Route2;

import java.util.*;

import static service.BusStopService.listBusStops;
import static service.BusStopService.updateBusStop;
import static service.Route2Service.listRoutes2;

public class CalcStopToMetro {
    public static void main(String[] args)
    {
        //----- HARDCODE CONTROLS -------
        final int MetroCriteria = 150; // meters, if stop is within, then it's a metro stop
        final int StopDelay = 30; // sec, time loss for stopping
        final int PedestrianSpeed = 1; // in m/s, equals 3.6 km/h but we have air distances so ok
        final int IntervalDummy = 600; // in case not available
        //----------------------

        long startTime = System.currentTimeMillis();

        List<BusStop> stops = listBusStops();
        List<Route2> routes = listRoutes2();
        Map<Integer, BusStop> stopMap = new HashMap<>();

        for(BusStop stop: stops)
        {
            stop.setTripSimple(0);
            stop.setTripFull(0);
            stop.setActive(false);
            stopMap.put(stop.getId(), stop);
        }


        for(Route2 route: routes)
        {
            List<String> routeStops = new ArrayList<>();
            if(route.getStops()!=null) // some routes may be crippled
                routeStops = Arrays.asList(route.getStops());

            double interval = route.getInterval()/2;
            if(Double.isNaN(interval)) interval = IntervalDummy;

            for (String curStopId: routeStops)
            {
                BusStop curStop = stopMap.get(Integer.parseInt(curStopId));

                curStop.setActive(true);

                if(curStop!=null
                        &&routeStops.indexOf(curStopId)<routeStops.size()-1
                        &&curStop.getMinMetroDist()>MetroCriteria) // not metro & not last & not null (bad data may occur)
                {
                    BusStop nextStop;
                    int cc = routeStops.indexOf(curStopId);
                    double tripSimple=0;
                    double tripFull=0;
                    boolean reached=false;
                    while (!reached&&cc<routeStops.size()-1)
                    {
                        tripSimple+=(route.getHops()[cc]+StopDelay);
                        cc++;
                        nextStop = stopMap.get(Integer.parseInt(routeStops.get(cc)));
                        if(nextStop!=null&&nextStop.getMinMetroDist()<=MetroCriteria) // reached!
                        {
                            if(curStop.getTripSimple()==0||curStop.getTripSimple()>tripSimple)
                            {
                                curStop.setTripSimple(tripSimple);
                                curStop.setNearestMetro(routeStops.get(cc));
                            }

                            tripFull=tripSimple+interval+nextStop.getMinMetroDist()*PedestrianSpeed;

                            if(curStop.getTripFull()==0||curStop.getTripFull()>tripFull)
                                curStop.setTripFull(tripFull);

                            reached=true;
                        }
                    }
                }
            }
        }

        //process special cases
        for (BusStop busStop: stops)
        {
            if(busStop.getMinMetroDist()<=MetroCriteria)
            {
                busStop.setNearestMetro(String.valueOf(busStop.getId()));
                busStop.setTripFull(busStop.getMinMetroDist()*PedestrianSpeed);
            }
            else if(busStop.getTripSimple()==0)
            {
                busStop.setTripSimple(Double.POSITIVE_INFINITY);
                busStop.setTripFull(Double.POSITIVE_INFINITY);
            }




            updateBusStop(busStop);
        }

        System.out.printf("\n\n===== Updated %d bus stops in %d seconds ======\n\n"
                , stops.size(), (System.currentTimeMillis()-startTime)/1000);
    }
}
