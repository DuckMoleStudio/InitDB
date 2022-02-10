package service;

import entity.FishnetCell;
import entity.FishnetCell2;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class FishnetCell2Service {



    public static FishnetCell2 getFishnetCellById (String id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(FishnetCell2.class, id);
    }

    public static List<FishnetCell2> listFishnetCells2()
    {
        return (List<FishnetCell2>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From FishnetCell2")
                .list();
    }

    public static void addFishnetCell2 (FishnetCell2 fishnetCell)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(fishnetCell);
        tx1.commit();
        session.close();
    }

    public static void updateFishnetCell2 (FishnetCell2 fishnetCell)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.update(fishnetCell);
        tx1.commit();
        session.close();
    }
}
