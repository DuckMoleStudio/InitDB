package service;

import entity.*;
import org.hibernate.Session;
import org.hibernate.Transaction;
import utils.HibernateSessionFactoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static service.FishnetStaticService.listFishnetStatic;
import static service.VersionService.listVersions;

public class FishnetDataService {

    public static Optional<FishnetData> getFishnetDataByKey(FishnetStatic cell, Version version)
    {
        FishnetVersionKey key = new FishnetVersionKey(cell, version);
        return Optional.of(HibernateSessionFactoryUtil.getSessionFactory().openSession().get(FishnetData.class, key));
    }

    public static List<FishnetData> listFishnetData()
    {
        return (List<FishnetData>)  HibernateSessionFactoryUtil
                .getSessionFactory()
                .openSession()
                .createQuery("From FishnetData")
                .list();
    }

    public static List<FishnetData> listFishnetDataByCell(FishnetStatic cell)
    {
        List<Version> versions = listVersions();
        List<FishnetData> result = new ArrayList<>();
        for (Version version: versions)
            if(getFishnetDataByKey(cell, version).isPresent())
                result.add(getFishnetDataByKey(cell, version).get());
        return result;
    }

    public static List<FishnetData> listFishnetDataByVersion(Version version)
    {
        List<FishnetStatic> cells = listFishnetStatic();
        List<FishnetData> result = new ArrayList<>();
        for (FishnetStatic cell: cells)
            if(getFishnetDataByKey(cell, version).isPresent())
                result.add(getFishnetDataByKey(cell, version).get());
        return result;
    }

    public static void addFishnetData (FishnetData fishnetData)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.save(fishnetData);
        tx1.commit();
        session.close();
    }

    public static void updateFishnetData (FishnetData fishnetData)
    {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.update(fishnetData);
        tx1.commit();
        session.close();
    }

    public static void deleteFishnetData(FishnetData fishnetData) {
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Transaction tx1 = session.beginTransaction();
        session.delete(fishnetData);
        tx1.commit();
        session.close();
    }

}
