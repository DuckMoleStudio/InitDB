import entity.BusStopVer;
import entity.Trip;
import org.hibernate.Session;
import org.hibernate.query.Query;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

import static service.AdmzoneService.getAdmzoneById;

public class CalcTripsForArea {
    public static void main(String[] args)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Query query = session.createQuery(
                "select p from Trip p " +
                        "where within(p.geomML, :area) = true ", Trip.class);
        query.setParameter("area", getAdmzoneById(6).getGeom());
        List<Trip> trips = query.getResultList();

        double totDist = 0;
        for(Trip trip: trips)
            totDist+=trip.getTotalDistance();

        System.out.printf("%d trips, %f total distance", trips.size(), totDist);
    }
}
