import entity.BusStop;
import org.hibernate.Session;
import org.hibernate.query.Query;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

import static service.BusStopService.listBusStops;
import static service.BusStopService.updateBusStop;

public class CalcStopMinDistanceToMetro {
    public static void main(String[] args) {

        long startTime = System.currentTimeMillis();

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        List<BusStop> stops = listBusStops();
        for(BusStop stop: stops)
        {
            Query query = session.createQuery(
                    "select distance(transform(p.geom,98568), transform(:stop,98568)) as d from Metro p " +
                            "where dwithin(p.geom, :stop, 0.1) = true " +
                            "order by distance(transform(p.geom,98568), transform(:stop,98568))",
                    Double.class);
            query.setParameter("stop", stop.getGeom());
            query.setMaxResults(1);
            List<Double> distances = query.getResultList();

            if(!distances.isEmpty())
                stop.setMinMetroDist(distances.get(0));
            else stop.setMinMetroDist(Double.POSITIVE_INFINITY);

            updateBusStop(stop);
        }

        System.out.printf("\n\n===== Calculated distances for %d stops in %d seconds ======\n\n"
                , stops.size(), (System.currentTimeMillis()-startTime)/1000);
    }
}
