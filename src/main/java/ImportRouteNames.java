import entity.Route;
import entity.Route2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static service.Route2Service.listRoutes2;
import static service.Route2Service.updateRoute2;
import static service.Route2Service.deleteRoute2;
import static service.RouteService.*;

public class ImportRouteNames {
    public static void main(String[] args) {

        int count=0;
        long startTime = System.currentTimeMillis();
        List<Route2> routes_all = listRoutes2();

        try (BufferedReader br = new BufferedReader(new FileReader("c:/matrix/GTFS/GTFS_ROUTES.TXT"))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                for(Route2 route: routes_all)
                {
                    if(values[0].equals(route.getRid()))
                    {
                        if(values[8].equals("Тм"))
                            route.setShortName("-del-");
                        else
                        route.setShortName(values[3]);

                        route.setLongName(values[4]);
                    }
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        for(Route2 route: routes_all)
        {
            if(route.getShortName().equals("-del-"))
                deleteRoute2(route);
            else
            {
                updateRoute2(route);
                count++;
            }
        }

        System.out.printf("\n\n===== Loaded %d route names in %d seconds ======\n\n"
                , count, (System.currentTimeMillis()-startTime)/1000);
    }
}
