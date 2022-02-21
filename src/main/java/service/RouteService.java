package service;

import entity.Route;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class RouteService {

    public static Route getRouteById (int id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(Route.class, id);
    }

    public static Route getRouteByRId (String rid, String dir)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Query query = session.createQuery("select p from Route p where p.rid = :rid",
                Route.class);
        query.setParameter("rid", rid);
        List<Route> result = query.getResultList();

        if(result.size()<1) return null;
        if(result.size()==1) return result.get(0);

        if(Integer.parseInt(dir)%2==Integer.parseInt(result.get(0).getDir())%2)
            return result.get(0);
        else return result.get(1);
    }

    public static List<Route> listRoutes()
    {
        return (List<Route>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From Route")
                .list();
    }

    public static void addRoute (Route route)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(route);
        tx1.commit();
        session.close();
    }

    public static void deleteRoute(Route route) {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.delete(route);
        tx1.commit();
        session.close();
    }

    public static void updateRoute (Route route)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.update(route);
        tx1.commit();
        session.close();
    }
}
