package service;

import entity.FishnetStatic;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class FishnetStaticService {

    public static FishnetStatic getFishnetStaticById (int id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(FishnetStatic.class, id);
    }

    public static List<FishnetStatic> listFishnetStatic()
    {
        return (List<FishnetStatic>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From FishnetStatic")
                .list();
    }

    public static void addFishnetStatic (FishnetStatic fishnetStatic)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(fishnetStatic);
        tx1.commit();
        session.close();
    }

    public static void updateFishnetStatic (FishnetStatic fishnetStatic)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.update(fishnetStatic);
        tx1.commit();
        session.close();
    }
}
