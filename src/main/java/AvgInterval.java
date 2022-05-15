import entity.Trip;

import java.util.List;

import static service.TripService.listTrips;

public class AvgInterval {
    public static void main(String[] args)
    {
        double gross=0;
        int count=0;
        int version=16;
        List<Trip> trips = listTrips();
        for(Trip trip: trips)
        {
            if(trip.getVersions().containsKey(version)&&!Double.isNaN(trip.getInterval()))
            {
                gross+=trip.getInterval();
                count++;
                System.out.println(count+" - "+trip.getInterval());
            }
        }
        System.out.println("Mean interval: "+gross/count);
    }
}
