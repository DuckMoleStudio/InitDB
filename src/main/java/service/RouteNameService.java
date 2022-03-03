package service;

import entity.RouteName;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class RouteNameService {

    public static RouteName getRouteNameById (int id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(RouteName.class, id);
    }



    public static List<RouteName> listRouteNames()
    {
        return (List<RouteName>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From RouteName")
                .list();
    }

    public static void addRouteName (RouteName routeName)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(routeName);
        tx1.commit();
        session.close();
    }

    public static void deleteRouteName(RouteName routeName) {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.delete(routeName);
        tx1.commit();
        session.close();
    }

    public static void updateRouteName (RouteName routeName)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.saveOrUpdate(routeName);
        tx1.commit();
        session.close();
    }
}
