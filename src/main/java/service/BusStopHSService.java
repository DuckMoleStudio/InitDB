package service;

import entity.BusStopHS;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class BusStopHSService {
    public static BusStopHS getBusStopHSById (int id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(BusStopHS.class, id);
    }

    public static List<BusStopHS> listBusStopHSs()
    {
        return (List<BusStopHS>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From BusStopHS")
                .list();
    }

    public static void addBusStopHS (BusStopHS busStopHS)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(busStopHS);
        tx1.commit();
        session.close();
    }

    public static void deleteBusStopHS (BusStopHS busStopHS)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.delete(busStopHS);
        tx1.commit();
        session.close();
    }

    public static void updateBusStopHS (BusStopHS busStopHS)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.saveOrUpdate(busStopHS);
        tx1.commit();
        session.close();
    }
}
