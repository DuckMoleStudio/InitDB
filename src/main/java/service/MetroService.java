package service;

import entity.Metro;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class MetroService {

    public static Metro getMetroId (int id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(Metro.class, id);
    }

    public static List<Metro> listMetros()
    {
        return (List<Metro>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From Metro")
                .list();
    }

    public static void addMetro (Metro metro)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(metro);
        tx1.commit();
        session.close();
    }
}
