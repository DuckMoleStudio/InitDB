package service;

import entity.Trip;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class TripService {

    public static Trip getTripById (int id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(Trip.class, id);
    }



    public static List<Trip> listTrips()
    {
        return (List<Trip>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From Trip")
                .list();
    }

    public static void addTrip (Trip trip)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(trip);
        tx1.commit();
        session.close();
    }

    public static void deleteTrip(Trip trip) {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.delete(trip);
        tx1.commit();
        session.close();
    }

    public static void updateTrip (Trip trip)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.saveOrUpdate(trip);
        tx1.commit();
        session.close();
    }
}
