package service;

import entity.BusStop;
import entity.Route;
import entity.Route2;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class Route2Service {

    public static Route2 getRoute2ById (int id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(Route2.class, id);
    }

    public static Route2 getRoute2ByRId (String rid, String dir)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Query query = session.createQuery("select p from Route2 p where p.rid = :rid",
                Route2.class);
        query.setParameter("rid", rid);
        List<Route2> result = query.getResultList();

        if(result.size()<1) return null;
        if(result.size()==1) return result.get(0);

        if(Integer.parseInt(dir)%2==Integer.parseInt(result.get(0).getDir())%2)
            return result.get(0);
        else return result.get(1);
    }

    public static List<Route2> listRoutes2()
    {
        return (List<Route2>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From Route2")
                .list();
    }

    public static void addRoute2 (Route2 route2)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(route2);
        tx1.commit();
        session.close();
    }

    public static void deleteRoute2(Route2 route2) {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.delete(route2);
        tx1.commit();
        session.close();
    }

    public static void updateRoute2 (Route2 route2)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.update(route2);
        tx1.commit();
        session.close();
    }
}
