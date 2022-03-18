import entity.Trip;
import entity.Version;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static loader.ImportGTFS.*;
import static service.TripService.deleteTrip;
import static service.TripService.listTrips;
import static service.VersionService.getVersionById;
import static service.VersionService.updateVersion;

public class LoadGTFS {
    public static void main(String[] args) {

        // CONTROLS
        final String stopFile = "c:/matrix/GTFS/GTFS_STOPS.TXT";
        final String tripFile = "c:/matrix/GTFS/GTFS_TRIPS.TXT";
        final String nameFile = "c:/matrix/GTFS/GTFS_ROUTES.TXT";
        final String tripStopFile = "c:/matrix/GTFS/GTFS_TRIPS_STOPS.TXT";
        final String geomFile = "c:/matrix/GTFS/GTFS_TRIP_SHAPES.TXT";
        final String intervalFile = "c:/matrix/GTFS/GTFS_INTERVAL.TXT";

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

        double tT=0;
        double tD=0;
       List<Trip> trips = listTrips();
        for(Trip trip: trips)
        {
            tT+=trip.getTotalTime()/3600;
            tD+=trip.getTotalDistance()/1000;
        }
        System.out.println(tD/tT);



/*
        Version version = getVersionById(5);
        version.setDesc("Корректирующий коэффициент скорости увеличен с 1.7 до 2.0 ");
        version.setDate(Date.valueOf("2022-3-11"));
        updateVersion(version);
*/


        // load stops
        //ImportBusStopsVer(stopFile,3);
        //ImportRoutesTripsVer(nameFile,tripFile,excludeRoutes,includeTrips, version.getVersionId());
        //ImportRoutesTripsVer(nameFile,tripFile,excludeRoutes,includeTrips, 5);
        // load trips
        //ImportTrips(tripFile);
        // load names
        //ImportRouteNames(nameFile);
        // load trip stops
        //ImportTripStops(tripStopFile);
        //ImportTripStopsVer(tripStopFile);

        // delete tram
        //DeleteTram();
        // load geom
        //ImportTripGeom(geomFile);
        //ImportTripGeomVer(geomFile);

        // load intervals
        //ImportTripIntervals(intervalFile);
        //ImportTripIntervalsVer(intervalFile);
    }
}
