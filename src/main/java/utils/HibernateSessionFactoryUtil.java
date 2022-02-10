package utils;

import entity.*;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;



public class HibernateSessionFactoryUtil {
    private static SessionFactory sessionFactory;

    private HibernateSessionFactoryUtil() {}

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {

                Configuration configuration = new Configuration().configure();
                configuration.addAnnotatedClass(Route.class);
                configuration.addAnnotatedClass(BusStop.class);
                configuration.addAnnotatedClass(FishnetCell.class);
                configuration.addAnnotatedClass(FishnetCell2.class);
                configuration.addAnnotatedClass(FishnetCellMatrix.class);
                configuration.addAnnotatedClass(Metro.class);
                configuration.addAnnotatedClass(Route2.class);

                StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
                sessionFactory = configuration.buildSessionFactory(builder.build());

            } catch (Exception e) {
                System.out.println("Исключение!" + e);
            }
        }
        return sessionFactory;
    }
}
