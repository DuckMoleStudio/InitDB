package service;

import entity.Admzone;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class AdmzoneService {
    public static Admzone getAdmzoneById (int id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(Admzone.class, id);
    }

    public static List<Admzone> listAdmzones()
    {
        return (List<Admzone>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From Admzone")
                .list();
    }

    public static void addAdmzone (Admzone admzone)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(admzone);
        tx1.commit();
        session.close();
    }

    public static void deleteAdmzone (Admzone admzone)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.delete(admzone);
        tx1.commit();
        session.close();
    }

    public static void updateAdmzone (Admzone admzone)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.saveOrUpdate(admzone);
        tx1.commit();
        session.close();
    }
}
