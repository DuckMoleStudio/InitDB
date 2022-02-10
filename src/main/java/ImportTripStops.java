import entity.Route2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static service.Route2Service.*;

public class ImportTripStops {
    public static void main(String[] args) {

        int count=0;
        long startTime = System.currentTimeMillis();

        List<Route2> routes = listRoutes2();
        Map<String,List<String>> stopMap = new HashMap<>();
        for(Route2 route: routes)
            stopMap.put(String.valueOf(route.getId()),new ArrayList<>());

        try (BufferedReader br = new BufferedReader(new FileReader("c:/matrix/GTFS/GTFS_TRIPS_STOPS.TXT"))) {
            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                // if on valid set
                if (stopMap.containsKey(values[4]))
                 stopMap.get(values[4]).add(values[10]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        for(Route2 route: routes)
        {
            route.setStops(stopMap.get(String.valueOf(route.getId())).toArray(new String[0]));
            updateRoute2(route);
            count++;
        }

        System.out.printf("\n\n===== Loaded stops data for %d routes in %d seconds ======\n\n"
                , count, (System.currentTimeMillis()-startTime)/1000);
    }
}
