import entity.BusStop;
import entity.FishnetCell;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

import static org.locationtech.jts.algorithm.Centroid.getCentroid;
import static service.FishnetCellService.getFishnetCellById;

public class RequestTests {
    public static void main(String[] args) {
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);
        FishnetCell cell_mosf = getFishnetCellById("128118"); // home sweet home 128118, LO 189118
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Query query = session.createQuery("select p from BusStop p where within(p.geom, :cell) = true",
                BusStop.class);
        query.setParameter("cell", cell_mosf.getGeom());
        List<BusStop> result = query.getResultList();
        System.out.println("Stops within:");
        for(BusStop bs: result)
        {
            System.out.println(bs.getName());
        }
        query = session.createQuery("select p from BusStop p where dwithin(p.geom, :cell, 0.03) = true " +
                        "order by distance(transform(p.geom,98568), transform(:cell,98568))",
                BusStop.class);
        query.setParameter("cell", gf.createPoint(getCentroid(cell_mosf.getGeom())));
        query.setMaxResults(10);
        result = query.getResultList();

        /*
        query = session.createQuery("select distance(transform(p.geom,3857), transform(:cell,3857)) as d " +
                        "from BusStop p where dwithin(p.geom, :cell, 0.01) = true " +
                        "order by distance(p.geom, :cell)",
                Double.class);
        query.setParameter("cell", cell_mosf.getGeom());
        List<Double> distances = query.getResultList();

        query = session.createQuery(
                "select distance(transform(p.geom,3857), transform(:cell,3857))*cosd(55) as d " +
                        "from BusStop p where dwithin(p.geom, :cell, 0.01) = true " +
                        "order by distance(p.geom, :cell)",
                Double.class);
        query.setParameter("cell", cell_mosf.getGeom());
        List<Double> distances1 = query.getResultList();


         */
        query = session.createQuery(
                "select distance(transform(p.geom,98568), transform(:cell,98568)) as d from BusStop p " +
                        "order by distance(transform(p.geom,98568), transform(:cell,98568))",
                Double.class);
        query.setParameter("cell", gf.createPoint(getCentroid(cell_mosf.getGeom())));
        query.setMaxResults(10);
        List<Double> distances2 = query.getResultList();

        System.out.println("\nNearest stops:");
        for(BusStop bs: result)
        {
            System.out.printf("%s at (%f of 98568)\n",
                    bs.getName(),
                    distances2.get(result.indexOf(bs)));
        }

    }
}
