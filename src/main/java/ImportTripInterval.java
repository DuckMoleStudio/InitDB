import entity.Route2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static service.Route2Service.listRoutes2;
import static service.Route2Service.updateRoute2;

public class ImportTripInterval {
    public static void main(String[] args) {

        int count=0;
        long startTime = System.currentTimeMillis();

        List<Route2> routes = listRoutes2();
        Map<String,List<Integer>> intervalMap = new HashMap<>();
        for(Route2 route: routes)
            intervalMap.put(route.getRid(),new ArrayList<>());

        try (BufferedReader br = new BufferedReader(new FileReader("c:/matrix/GTFS/GTFS_INTERVAL.TXT"))) {
            br.readLine();
            String line;


            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                List<Integer> curIntervals = intervalMap.get(values[7]);
                if(curIntervals!=null) {
                    curIntervals.add(Integer.parseInt(values[4]) * 60);
                    curIntervals.add(Integer.parseInt(values[5]) * 60);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        for(Route2 route: routes)
        {
            List<Integer> curIntervals = intervalMap.get(route.getRid());
            route.setInterval(curIntervals.stream().mapToInt(Integer::intValue).average().orElse(Double.NaN));
            updateRoute2(route);
            count++;
        }



        System.out.printf("\n\n===== Loaded intervals data for %d routes in %d seconds ======\n\n"
                , count, (System.currentTimeMillis()-startTime)/1000);
    }
}
