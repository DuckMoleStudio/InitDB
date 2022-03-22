package service;

import entity.Admstrip;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class AdmstripService {
    public static Admstrip getAdmstripById (int id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(Admstrip.class, id);
    }

    public static List<Admstrip> listAdmstrips()
    {
        return (List<Admstrip>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From Admstrip")
                .list();
    }

    public static void addAdmstrip (Admstrip admstrip)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(admstrip);
        tx1.commit();
        session.close();
    }

    public static void deleteAdmstrip (Admstrip admstrip)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.delete(admstrip);
        tx1.commit();
        session.close();
    }

    public static void updateAdmstrip (Admstrip admstrip)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.saveOrUpdate(admstrip);
        tx1.commit();
        session.close();
    }
}
