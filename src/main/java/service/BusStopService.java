package service;

import entity.BusStop;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class BusStopService {


    public static BusStop getBusStopById (int id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(BusStop.class, id);
    }

    public static List<BusStop> listBusStops()
    {
        return (List<BusStop>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From BusStop")
                .list();
    }

    public static void addBusStop (BusStop busStop)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(busStop);
        tx1.commit();
        session.close();
    }

    public static void updateBusStop (BusStop busStop)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.update(busStop);
        tx1.commit();
        session.close();
    }
}
