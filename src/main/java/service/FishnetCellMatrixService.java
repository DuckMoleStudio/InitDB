package service;


import entity.FishnetCellMatrix;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.List;

public class FishnetCellMatrixService {



    public static FishnetCellMatrix getCellById (String id)
    {
        return HibernateSessionFactoryUtil.getSessionFactory().openSession().get(FishnetCellMatrix.class, id);
    }

    public static List<FishnetCellMatrix> listCells()
    {
        return (List<FishnetCellMatrix>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From FishnetCellMatrix")
                .list();
    }

    public static void addCell (FishnetCellMatrix fishnetCell)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(fishnetCell);
        tx1.commit();
        session.close();
    }

    public static void updateCell (FishnetCellMatrix fishnetCell)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.update(fishnetCell);
        tx1.commit();
        session.close();
    }
}

