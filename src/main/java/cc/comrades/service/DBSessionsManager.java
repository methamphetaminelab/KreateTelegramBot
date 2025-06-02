package cc.comrades.service;

import cc.comrades.util.EnvLoader;
import jakarta.persistence.Entity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.reflections.Reflections;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class DBSessionsManager {

    // thread-safe singleton
    private static class Holder {
        public static final SessionFactory FACTORY = buildSessionFactory();
    }

    private static SessionFactory getFactory() {
        return Holder.FACTORY;
    }

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration configuration = DBSessionsManager.createConfiguration();
            ServiceRegistry registry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties())
                    .build();
            return configuration.buildSessionFactory(registry);
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static Configuration createConfiguration() {
        String driver = EnvLoader.get("DB_DRIVER");
        String host = EnvLoader.get("DB_HOST");
        String port = EnvLoader.get("DB_PORT");
        String name = EnvLoader.get("DB_NAME");
        String url = "jdbc:" + driver + "://" + host + ":" + port + "/" + name;

        String user = EnvLoader.get("DB_USER");
        String password = EnvLoader.get("DB_PASSWORD");

        Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.connection.provider_class",
                "com.zaxxer.hikari.hibernate.HikariConnectionProvider");
        configuration.setProperty("hibernate.hikari.jdbcUrl", url);
        configuration.setProperty("hibernate.hikari.username", user);
        configuration.setProperty("hibernate.hikari.password", password);
        configuration.setProperty("hibernate.hikari.minimumIdle", "1");
        configuration.setProperty("hibernate.hikari.maximumPoolSize", "20");
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "update");

        Reflections reflections = new Reflections("cc.comrades.model.entity");
        Set<Class<?>> entities = reflections.getTypesAnnotatedWith(Entity.class);
        for (Class<?> clazz : entities) {
            configuration.addAnnotatedClass(clazz);
        }

        return configuration;
    }

    public static <T> T getObject(Class<T> clazz, Long id) {
        try (Session session = getFactory().openSession()) {
            return session.get(clazz, id);
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving " + clazz.getSimpleName() + " with id=" + id, e);
        }
    }

    public static <T> T saveObject(T object) {
        return executeTransaction((session) -> session.merge(object));
    }

    public static <T> void deleteObject(Class<T> clazz, Long id) {
        executeTransaction(session -> {
            T managed = session.get(clazz, id);
            if (managed != null) {
                session.remove(managed);
            } else {
                throw new IllegalArgumentException("Entity not found: " + clazz.getSimpleName() + " id=" + id);
            }
            return null;
        });
    }

    public static <T> List<T> findByField(Class<T> clazz, String fieldName, Object value) {
        try (Session session = getFactory().openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<T> cq = cb.createQuery(clazz);
            Root<T> root = cq.from(clazz);
            cq.select(root)
                    .where(cb.equal(root.get(fieldName), value));
            return session.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error retrieving " + clazz.getSimpleName()
                            + " entries where " + fieldName + " = " + value, e);
        }
    }

    public static <T> T findFirstByField(Class<T> clazz, String fieldName, Object value) {
        List<T> results = findByField(clazz, fieldName, value);
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    public static <T> T getOrCreate(Class<T> clazz, String fieldName, Object value, Supplier<T> supplier) {
        T entity = findFirstByField(clazz, fieldName, value);
        if (entity != null) {
            return entity;
        }
        return saveObject(supplier.get());
    }

    public static <T> List<T> getAllObjects(Class<T> clazz) {
        try (Session session = getFactory().openSession()) {
            String name = session.getMetamodel().entity(clazz).getName();
            return session.createQuery("FROM " + name, clazz).list();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving objects", e);
        }
    }

    public static <T> T executeTransaction(Function<Session, T> work) {
        return executeTransaction(work, (exception) -> {
            throw new RuntimeException(exception);
        });
    }

    public static <T> T executeTransaction(Function<Session, T> work, Consumer<Exception> onError) {
        try (Session session = getFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                T object = work.apply(session);
                transaction.commit();
                return object;
            } catch (Exception e) {
                transaction.rollback();
                onError.accept(e);
            }
        }

        return null;
    }

    public static void close() {
        SessionFactory factory = getFactory();
        if (factory != null && !factory.isClosed()) {
            factory.close();
        }
    }
}
