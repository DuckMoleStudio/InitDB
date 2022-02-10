import entity.Route;
import entity.Route2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static service.RouteService.addRoute;
import static service.Route2Service.addRoute2;

// DEPRECATED

public class ImportTrips {
    public static void main(String[] args) {

        int count=0;
        long startTime = System.currentTimeMillis();
        List<String> routes_0 = new ArrayList<>();
        List<String> routes_1 = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("c:/matrix/GTFS/GTFS_TRIPS.TXT"))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                if((Integer.parseInt(values[4])==0 && !routes_0.contains(values[1]))||
                        (Integer.parseInt(values[4])==1 && !routes_1.contains(values[1])))
                {
                    Route2 route = new Route2();
                    route.setId(Integer.parseInt(values[0]));
                    route.setRid(values[1]);
                    route.setDir(values[4]);
                    if(Integer.parseInt(values[4])==0) routes_0.add(values[1]);
                    else routes_1.add(values[1]);

                    addRoute2(route);

                    count++;
                }


            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.printf("\n\n===== Loaded %d trips in %d seconds ======\n\n"
                , count, (System.currentTimeMillis()-startTime)/1000);
    }
}
