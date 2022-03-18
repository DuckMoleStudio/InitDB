package service;

import entity.FishnetCellVer;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class FishnetCellVerService {

    public static FishnetCellVer getFishnetCellById (int id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(FishnetCellVer.class, id);
    }

    public static List<FishnetCellVer> listFishnetCellVers()
    {
        return (List<FishnetCellVer>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From FishnetCellVer")
                .list();
    }

    public static void addFishnetCellVer (FishnetCellVer fishnetCell)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(fishnetCell);
        tx1.commit();
        session.close();
    }

    public static void updateFishnetCellVer (FishnetCellVer fishnetCell)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.saveOrUpdate(fishnetCell);
        tx1.commit();
        session.close();
    }
}
