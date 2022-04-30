import entity.RouteName;
import entity.Trip;
import entity.Version;
import org.hibernate.Session;
import org.hibernate.query.Query;
import utils.HibernateSessionFactoryUtil;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static service.AdmzoneService.getAdmzoneById;
import static service.RouteNameService.listRouteNames;
import static service.RouteNameService.updateRouteName;
import static service.TripService.updateTrip;
import static service.VersionService.getVersionById;
import static service.VersionService.updateVersion;

public class GTFSbyArea
{
    public static void main(String[] args)
    {
        int baseVersion = 5;
        int areaId = 6;

        Version version = new Version();
        version.setDesc("GTFS: только ЗАО");
        version.setDate(Date.valueOf("2022-4-27"));
        updateVersion(version);

        List<RouteName> routes = listRouteNames();
        Map<Integer,RouteName> routeMap = new HashMap<>();
        for(RouteName rn: routes)
            routeMap.put(rn.getId(),rn);

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Query query = session.createQuery(
                "select p from Trip p " +
                        "where within(p.geomML, :area) = true ", Trip.class);
        query.setParameter("area", getAdmzoneById(areaId).getGeom());
        List<Trip> tripsAll = query.getResultList();

        List<Trip> trips = new ArrayList<>();

        for(Trip trip: tripsAll)
            if(trip.getVersions().containsKey(baseVersion))
                if(trip.getVersions().get(baseVersion))
                    trips.add(trip);

        for(Trip trip: trips)
        {
            trip.getVersions().put(version.getVersionId(),true);
            if(!routeMap.get(Integer.parseInt(trip.getRid())).getTrips().containsKey(version.getVersionId()))
                routeMap.get(Integer.parseInt(trip.getRid())).getTrips().put(version.getVersionId(),new ArrayList<>());
            routeMap.get(Integer.parseInt(trip.getRid())).getTrips().get(version.getVersionId()).add(trip.getId());
            updateTrip(trip);
        }

        for(RouteName rn: routes)
            updateRouteName(rn);
    }
}
