package service;

import entity.BusStopVer;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class BusStopVerService {
    public static BusStopVer getBusStopVerById (int id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(BusStopVer.class, id);
    }

    public static List<BusStopVer> listBusStopVers()
    {
        return (List<BusStopVer>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From BusStopVer")
                .list();
    }

    public static void addBusStopVer (BusStopVer busStopVer)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(busStopVer);
        tx1.commit();
        session.close();
    }

    public static void deleteBusStopVer (BusStopVer busStopVer)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.delete(busStopVer);
        tx1.commit();
        session.close();
    }

    public static void updateBusStopVer (BusStopVer busStopVer)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.saveOrUpdate(busStopVer);
        tx1.commit();
        session.close();
    }
}
