import entity.Route2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static service.Route2Service.addRoute2;

public class ImportTripsAll {
    public static void main(String[] args) {

        int count=0;
        long startTime = System.currentTimeMillis();

        try (BufferedReader br = new BufferedReader(new FileReader("c:/matrix/GTFS/GTFS_TRIPS.TXT"))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                if(values[3].equals("00")) {
                    Route2 route = new Route2();
                    route.setId(Integer.parseInt(values[0]));
                    route.setRid(values[1]);
                    route.setDir(values[4]);

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
