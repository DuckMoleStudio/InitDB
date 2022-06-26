import entity.Trip;

import java.util.List;

import static service.TripService.listTrips;

public class TerminalTest
{
    public static void main(String[] args)
    {
        int version=16;
        String stopMy="5558";
        List<Trip> trips = listTrips();
        for(Trip trip: trips)
        {
            String[] stops = trip.getStops();
            boolean contains = false;
            for(String stop: stops)
                if (stop.equals(stopMy))
                {
                    contains = true;
                    break;
                }
                if(contains)
                {
                    System.out.printf("\nRoute %s:%s",trip.getRid(),trip.getDir());
                    for (String stop : stops)
                        System.out.printf(" %s,",stop);
                }

        }
    }
}
