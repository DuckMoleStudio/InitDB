import entity.*;

import java.util.List;

import static service.BusStopVerService.listBusStopVers;
import static service.BusStopVerService.updateBusStopVer;
import static service.FishnetCellVerService.listFishnetCellVers;
import static service.FishnetCellVerService.updateFishnetCellVer;
import static service.RouteNameService.*;
import static service.TripService.*;
import static service.VersionService.deleteVersion;
import static service.VersionService.getVersionById;

public class DeleteVersion
{
    public static void main(String[] args)
    {

        int versionId = 3;

        Version version = getVersionById(versionId);
        deleteVersion(version);

        List<BusStopVer> stops = listBusStopVers();
        for(BusStopVer stop: stops)
        {
            stop.getActive().remove(versionId);
            stop.getTripSimple().remove(versionId);
            stop.getTripFull().remove(versionId);
            stop.getNearestMetro().remove(versionId);

            updateBusStopVer(stop);
        }

        List<FishnetCellVer> cells = listFishnetCellVers();
        for(FishnetCellVer cell: cells)
        {
            cell.getMinStopDist().remove(versionId);
            cell.getNearestMetro().remove(versionId);
            cell.getMetroSimple().remove(versionId);
            cell.getMetroFull().remove(versionId);
            cell.getNearestMetroCar().remove(versionId);
            cell.getMetroCar().remove(versionId);

            updateFishnetCellVer(cell);
        }

        List<RouteName> routeNames = listRouteNames();
        for(RouteName routeName: routeNames)
        {
            if(routeName.getTrips().containsKey(versionId))
            {
                routeName.getTrips().remove(versionId);
                if(routeName.getTrips().isEmpty())
                    deleteRouteName(routeName);
                else
                    updateRouteName(routeName);
            }
        }

        List<Trip> trips = listTrips();
        for(Trip trip: trips)
        {
            if(trip.getVersions().containsKey(versionId))
            {
                trip.getVersions().remove(versionId);
                if(trip.getVersions().isEmpty())
                    deleteTrip(trip);
                else
                    updateTrip(trip);
            }
        }

    }
}
