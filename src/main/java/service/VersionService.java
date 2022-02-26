package service;

import entity.Route;
import entity.Version;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class VersionService {
    public static Version getVersionById (int id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(Version.class, id);
    }

    public static List<Version> listVersions()
    {
        return (List<Version>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From Version")
                .list();
    }

    public static void addVersion (Version version)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(version);
        tx1.commit();
        session.close();
    }

    public static void deleteVersion(Version version) {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        //Session session = HibernateSessionFactoryUtil.getSessionFactory().getCurrentSession();
        Transaction tx1 = session.beginTransaction();
        session.delete(version);
        tx1.commit();
        session.close();
    }

    public static void updateVersion (Version version)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.update(version);
        tx1.commit();
        session.close();
    }
}
