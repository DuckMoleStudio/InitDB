package service;

import entity.FishnetCellHS;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class FishnetCellHSService {

    public static FishnetCellHS getFishnetCellById (int id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(FishnetCellHS.class, id);
    }

    public static List<FishnetCellHS> listFishnetCellHSs()
    {
        return (List<FishnetCellHS>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From FishnetCellHS")
                .list();
    }

    public static void addFishnetCellHS (FishnetCellHS fishnetCell)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(fishnetCell);
        tx1.commit();
        session.close();
    }

    public static void updateFishnetCellHS (FishnetCellHS fishnetCell)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.saveOrUpdate(fishnetCell);
        tx1.commit();
        session.close();
    }
}
