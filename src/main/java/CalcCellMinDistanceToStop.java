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

public class CalcCellMinDistanceToStop {
    public static void main(String[] args) {

        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        List<FishnetCell2> cells = listFishnetCells2();
        for(FishnetCell2 cell: cells)
        {
            Query query = session.createQuery(
                    "select distance(transform(p.geom,98568), transform(:cell,98568)) as d from BusStop p " +
                            "where dwithin(p.geom, :cell, 0.03) = true and p.active = true " +
                            "order by distance(transform(p.geom,98568), transform(:cell,98568))",
                    Double.class);
            query.setParameter("cell", gf.createPoint(getCentroid(cell.getGeom())));
            query.setMaxResults(1);
            List<Double> distances = query.getResultList();

            if(!distances.isEmpty())
            cell.setMinStopDist(distances.get(0));
            else cell.setMinStopDist(Double.POSITIVE_INFINITY);

            updateFishnetCell2(cell);
        }

        System.out.printf("\n\n===== Calculated distances for %d cells in %d seconds ======\n\n"
                , cells.size(), (System.currentTimeMillis()-startTime)/1000);
    }
}
