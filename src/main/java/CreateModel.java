import entity.Trip;
import entity.Version;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static service.TripService.listTrips;
import static loader.Calculation.*;
import static service.VersionService.*;

public class CreateModel {
    public static void main(String[] args) {

        // CONTROLS
        final String stopFile = "c:/matrix/GTFS/GTFS_STOPS.TXT";
        final String tripFile = "c:/matrix/GTFS/GTFS_TRIPS.TXT";
        final String nameFile = "c:/matrix/GTFS/GTFS_ROUTES.TXT";
        final String tripStopFile = "c:/matrix/GTFS/GTFS_TRIPS_STOPS.TXT";
        final String geomFile = "c:/matrix/GTFS/GTFS_TRIP_SHAPES.TXT";
        final String intervalFile = "c:/matrix/GTFS/GTFS_INTERVAL.TXT";

        final double speedRatio = 2.0;
        final String osmFile = "C:/matrix/RU-MOW.osm.pbf";
        final String dir = "local/graphhopper";
        final int MetroCriteria = 150; // meters, if stop is within, then it's a metro stop
        final int StopDelay = 30; // sec, time loss for stopping
        final int PedestrianSpeed = 1; // in m/s, 1 equals 3.6 km/h but we have air distances so ok
        final int IntervalDummy = 600; // in case not available
        final int Radius = 500; // meters, looking for stops in this radius
        final int RadiusMetro = 6000; // meters, looking for metro in this radius
        final int SnapDistance = 500; // no road radius

        Map<Integer,String> excludeRoutes = new HashMap<>();
        Map<Integer,String> includeTrips = new HashMap<>();

        excludeRoutes.put(8,"Тм");
        excludeRoutes.put(5,"межсубъектный");

        includeTrips.put(3,"00");

        /*
        includeTrips.put(3,"У1");
        includeTrips.put(3,"У2");
        includeTrips.put(3,"И1");
        includeTrips.put(3,"Д1");

         */

       /*
        double tT=0;
        double tD=0;
       List<Trip> trips = listTrips();
        for(Trip trip: trips)
        {
            tT+=trip.getTotalTime()/3600;
            tD+=trip.getTotalDistance()/1000;
        }
        System.out.println(tD/tT);
*/


/*
        Version version = getVersionById(6);
        //version.setDesc("Новый профиль для НГПТ в маршрутизации");
        version.setDate(Date.valueOf("2022-4-04"));
        updateVersion(version);

 */

        // LOADING

        //ImportBusStopsVer(stopFile,3);
        //ImportRoutesTripsVer(nameFile,tripFile,excludeRoutes,includeTrips, version.getVersionId());
        //ImportRoutesTripsVer(nameFile,tripFile,excludeRoutes,includeTrips, 5);
        //ImportTripStopsVer(tripStopFile);
        //ImportTripGeomVer(geomFile);
        //ImportTripIntervalsVer(intervalFile);

        // CALCULATING

        //CalcTripHopsVer(speedRatio, StopDelay, osmFile, dir);
        //CalcStopMinDistToMetroVer();
        CalcStopToMetroVer(MetroCriteria, StopDelay, PedestrianSpeed, IntervalDummy, 5);
        //CalcCellMinDistToStopHS(16, true, 6);
        //CalcCellMetroAllHS(Radius, RadiusMetro, PedestrianSpeed, osmFile, dir, speedRatio, SnapDistance, 16, true, 6);
    }
}
