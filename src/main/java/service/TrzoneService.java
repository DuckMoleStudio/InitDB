package service;

import entity.Trzone;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class TrzoneService {
    public static Trzone getTrzoneById (int id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(Trzone.class, id);
    }

    public static List<Trzone> listTrzones()
    {
        return (List<Trzone>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From Trzone")
                .list();
    }

    public static void addTrzone (Trzone trzone)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(trzone);
        tx1.commit();
        session.close();
    }

    public static void deleteTrzone (Trzone trzone)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.delete(trzone);
        tx1.commit();
        session.close();
    }

    public static void updateTrzone (Trzone trzone)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.saveOrUpdate(trzone);
        tx1.commit();
        session.close();
    }
}
