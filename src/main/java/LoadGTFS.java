import java.util.HashMap;
import java.util.Map;

import static loader.ImportGTFS.*;

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

        // load stops
        ImportBusStopsVer(stopFile,3);
        //ImportRoutesTripsVer(nameFile,tripFile,excludeRoutes,includeTrips,4);
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
