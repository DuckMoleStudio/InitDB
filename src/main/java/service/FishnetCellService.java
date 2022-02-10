package service;

import entity.FishnetCell;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class FishnetCellService {



    public static FishnetCell getFishnetCellById (String id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(FishnetCell.class, id);
    }

    public static List<FishnetCell> listFishnetCells()
    {
        return (List<FishnetCell>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From FishnetCell")
                .list();
    }

    public static void addFishnetCell (FishnetCell fishnetCell)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(fishnetCell);
        tx1.commit();
        session.close();
    }

    public static void updateFishnetCell (FishnetCell fishnetCell)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.update(fishnetCell);
        tx1.commit();
        session.close();
    }
}
