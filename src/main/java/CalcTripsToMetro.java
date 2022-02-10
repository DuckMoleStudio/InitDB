import entity.BusStop;
import entity.Route;
import supplimentary.StopSpecial;

import java.util.*;

import static service.BusStopService.listBusStops;
import static service.BusStopService.updateBusStop;
import static service.RouteService.listRoutes;

// DEPRECATED

public class CalcTripsToMetro
{
    public static void main(String[] args)
    {
        //----- HARDCODE CONTROLS -------
        final int MetroCriteria = 150; // meters, if stop is within, then it's a metro stop
        final int HopValue = 120; // seconds, average between stops
        final int WaitEstimate = 7*60; // half-interval
        final int PedestrianSpeed = 1; // in m/s, equals 3.6 km/h but we have air distances so ok
        //----------------------

        long startTime = System.currentTimeMillis();

        List<BusStop> stops = listBusStops();
        List<Route> routes = listRoutes();
        Map<Integer, StopSpecial> stopSpecialMap = new HashMap<>();

        for(BusStop stop: stops)
        {
            StopSpecial ss = new StopSpecial();
            ss.setMetro(stop.getMinMetroDist() < MetroCriteria);
            ss.setTrips(new ArrayList<>());
            ss.setDistMetro(stop.getMinMetroDist());
            stopSpecialMap.put(stop.getId(), ss);
        }

        for(Route route: routes)
        {
            List<String> routeStops = new ArrayList<>();
            if(route.getStops()!=null) // some routes may be crippled
            routeStops = Arrays.asList(route.getStops());

            for (String stop_id: routeStops)
            {
                StopSpecial stopSpecial = stopSpecialMap.get(Integer.parseInt(stop_id));

                if(stopSpecial!=null
                        &&routeStops.indexOf(stop_id)<routeStops.size()-1
                        &&!stopSpecial.isMetro()) // not metro & not last & not null (bad data may oocur)
                {
                    StopSpecial ssCur;
                    int cc = routeStops.indexOf(stop_id);
                    int cs = 0;
                    boolean reached=false;
                    while (!reached&&cc<routeStops.size()-1)
                    {
                        cc++;
                        cs++;
                        ssCur = stopSpecialMap.get(Integer.parseInt(routeStops.get(cc)));
                        if(ssCur!=null&&ssCur.isMetro()) // reached!
                        {
                          if(stopSpecial.getMinStops()==0||stopSpecial.getMinStops()>cs)
                          {
                              stopSpecial.setMinStops(cs);
                              stopSpecial.setNearestMetro(routeStops.get(cc));
                          }
                          stopSpecial.getTrips().add(cs * HopValue +
                                  ssCur.getDistMetro()*PedestrianSpeed);
                          reached=true;
                        }
                    }
                }
            }
        }
        //now map back
        for (BusStop busStop: listBusStops())
        {
            StopSpecial curSS = stopSpecialMap.get(busStop.getId());

            if(curSS.isMetro()) busStop.setNearestMetro(String.valueOf(busStop.getId()));
            else
            busStop.setNearestMetro(curSS.getNearestMetro());

            if(curSS.isMetro())
                busStop.setTripSimple(0);
            else if(curSS.getMinStops()!=0)
            busStop.setTripSimple(curSS.getMinStops()*HopValue);
            else busStop.setTripSimple(Double.POSITIVE_INFINITY);

            if(curSS.getTrips().isEmpty())
            {
                if(curSS.isMetro())
                    busStop.setTripFull(curSS.getDistMetro()*PedestrianSpeed);
                else
                busStop.setTripFull(Double.POSITIVE_INFINITY);
            }
            else if (curSS.getTrips().size()==1)
                busStop.setTripFull(curSS.getTrips().get(0)+WaitEstimate);
            else {
                List<Double> trips = new ArrayList<>(curSS.getTrips());
                Collections.sort(trips);
                double minTrip = trips.stream().mapToDouble(a -> a).average().getAsDouble() + WaitEstimate / trips.size();
                boolean proceed = true;
                while (proceed) {
                    if(trips.size()>1) {
                        List<Double> testList = new ArrayList<>(trips);
                        testList.remove(testList.size() - 1);
                        if (testList.stream().mapToDouble(a -> a).average().getAsDouble() + WaitEstimate / testList.size() <
                                minTrip) {
                            minTrip = testList.stream().mapToDouble(a -> a).average().getAsDouble() + WaitEstimate / testList.size();
                            trips = testList;
                        } else proceed = false;
                    } else proceed = false;
                }
                busStop.setTripFull(minTrip);
            }

            updateBusStop(busStop);
        }

        System.out.printf("\n\n===== Updated %d bus stops in %d seconds ======\n\n"
                , listBusStops().size(), (System.currentTimeMillis()-startTime)/1000);
    }
}
