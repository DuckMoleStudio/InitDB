import entity.BusStop;
import entity.FishnetCell2;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

import static org.locationtech.jts.algorithm.Centroid.getCentroid;
import static service.FishnetCell2Service.listFishnetCells2;
import static service.FishnetCell2Service.updateFishnetCell2;

// DEPRECATED

public class CalcCellMetro {
    public static void main(String[] args) {

        //----- HARDCODE CONTROLS -------
        final int Radius = 500; // meters, looking for stops in this radius
        final int PedestrianSpeed = 1; // in m/s, equals 3.6 km/h but we have air distances so ok
        //----------------------

        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        List<FishnetCell2> cells = listFishnetCells2();
        for(FishnetCell2 cell: cells)
        {
            Query query = session.createQuery(
                    "select p.tripSimple, p.tripFull, p.nearestMetro," +
                            "distance(transform(p.geom,98568), transform(:cell,98568)) from BusStop p " +
                            "where dwithin(transform(p.geom,98568), transform(:cell,98568), :radius) = true ");
            query.setParameter("cell", gf.createPoint(getCentroid(cell.getGeom())));
            query.setParameter("radius", Radius);
            List<Object[]> stops = (List<Object[]>)query.list();

            if(!stops.isEmpty())
            {
                double minSimple = Double.POSITIVE_INFINITY;
                double minFull = Double.POSITIVE_INFINITY;
                String nearest = "no routes";
                for(Object[] stop: stops)
                {
                    if((Double)stop[0]<minSimple) minSimple=(Double)stop[0];
                    if((Double)stop[1]+(Double)stop[3]*PedestrianSpeed<minFull)
                    {
                        minFull=(Double)stop[1]+(Double)stop[3]*PedestrianSpeed;
                        nearest=(String) stop[2];
                    }
                }
                cell.setMetroSimple(minSimple);
                cell.setMetroFull(minFull);
                cell.setNearestMetro(nearest);
            }
            else
            {
                cell.setMetroSimple(Double.POSITIVE_INFINITY);
                cell.setMetroFull(Double.POSITIVE_INFINITY);
                cell.setNearestMetro("no stops");
            }

            updateFishnetCell2(cell);
        }

        System.out.printf("\n\n===== Calculated metro distances for %d cells in %d seconds ======\n\n"
                , cells.size(), (System.currentTimeMillis()-startTime)/1000);
    }
}
