package com.mockdatabase.core;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import com.google.common.io.Files;
import com.mockdatabase.annotation.Rollback;
import com.mockdatabase.exception.MockDatabaseException;

public class Database {

	private static List<Table<?, ? extends Serializable>> tables = new ArrayList<Table<?, ? extends Serializable>>();

	private static SessionFactory sessionFactory;

	protected static void build() {
		Configuration configuration = new Configuration();
		configuration.configure(Database.class.getResource("/hibernate.cfg.xml"));
		for (Table<?, ? extends Serializable> table : tables) {
			if (table.getMappingXmlFile() == null) {
				configuration.addAnnotatedClass(table.getEntityClass());
			} else {
				configuration.addFile(table.getMappingXmlFile());
			}
		}
		ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();
		sessionFactory = configuration.buildSessionFactory(serviceRegistry);
	}

	protected static Session getSession() {
		if (sessionFactory == null) {
			MessageSource messageSource = MessageSource.getInstance();
			throw new MockDatabaseException(messageSource.getMessage("com.mock.database.session.error"));
		}
		return sessionFactory.getCurrentSession();
	}

	protected static void configureSession(com.mockdatabase.annotation.Transaction transactionAnnotation) {
		Session session = sessionFactory.openSession();
		session.setFlushMode(FlushMode.MANUAL);
		Transaction transaction = session.beginTransaction();
		if (transactionAnnotation != null && transactionAnnotation.timeout() >= 0) {
			transaction.setTimeout(transactionAnnotation.timeout());
		}
	}

	protected static void closeSession(com.mockdatabase.annotation.Transaction transactionAnnotation, Rollback rollbackAnnotation) {
		if (rollbackAnnotation != null) {
			destroySession();
			return;
		}
		if (transactionAnnotation != null && transactionAnnotation.readOnly()) {
			destroySession();
			return;
		}
		Session session = getSession();
		session.flush();
		session.getTransaction().commit();
		session.close();
	}

	protected static void destroySession() {
		Session session = getSession();
		session.getTransaction().rollback();
		session.close();
	}

	public static <T, PK extends Serializable> Table<T, PK> createTable(Class<T> entityClass, Class<PK> idType) {
		Table<T, PK> table = new Table<T, PK>(entityClass);
		tables.add(table);
		return table;
	}

	public static <T, PK extends Serializable> Table<T, PK> createTable(Class<T> entityClass, Class<PK> idType, File mappingXmlFile) {
		Table<T, PK> table = new Table<T, PK>(entityClass, mappingXmlFile);
		tables.add(table);
		return table;
	}

	public static <T, PK extends Serializable> Table<T, PK> createTable(Class<T> entityClass, Class<PK> idType, String mappingXmlFileUrl) {
		Table<T, PK> table = new Table<T, PK>(entityClass, mappingXmlFileUrl);
		tables.add(table);
		return table;
	}
	
	@SuppressWarnings("unchecked")
	public static <E> List<E> list(DetachedCriteria detachedCriteria, Class<E> returnClass) {
		return detachedCriteria.getExecutableCriteria(getSession()).list();
	}
	
	@SuppressWarnings("unchecked")
	public static <E> List<E> listHQL(String HQLQuery, Class<E> returnClass) {
		return getSession().createQuery(HQLQuery).list();
	}

	@SuppressWarnings("unchecked")
	public static <E> List<E> listSQL(String SQLQuery, Class<E> returnClass) {
		return getSession().createSQLQuery(SQLQuery).list();
	}

	@SuppressWarnings("unchecked")
	public static <E> E uniqueResult(DetachedCriteria detachedCriteria, Class<E> returnClass) {
		return (E) detachedCriteria.getExecutableCriteria(getSession()).uniqueResult();
	}
	
	@SuppressWarnings("unchecked")
	public static <E> E uniqueResultHQL(String HQLQuery, Class<E> returnClass) {
		return (E) getSession().createQuery(HQLQuery).uniqueResult();
	}

	@SuppressWarnings("unchecked")
	public static <E> E uniqueResultSQL(String SQLQuery, Class<E> returnClass) {
		return (E) getSession().createSQLQuery(SQLQuery).uniqueResult();
	}

	public static int executeUpdateHQL(String HQLQuery) {
		return getSession().createQuery(HQLQuery).executeUpdate();
	}

	public static int executeUpdateSQL(String SQLQuery) {
		return getSession().createSQLQuery(SQLQuery).executeUpdate();
	}

	public static int runScriptHQL(File file, Charset encoding) {
		try {
			String script= readFile(file, encoding);
			return executeUpdateHQL(script);
		} catch (IOException e) {
			MessageSource messageSource = MessageSource.getInstance();
			String message = messageSource.getMessage("com.mock.database.script.hql.error", file.getName());
			throw new MockDatabaseException(message, e);
		}
	}

	public static int runScriptHQL(String path, Charset encoding) {
		return runScriptHQL(new File(path), encoding);
	}

	public static int runScriptSQL(File file, Charset encoding) {
		try {
			String script = readFile(file, encoding);
			return executeUpdateSQL(script);
		} catch (IOException e) {
			MessageSource messageSource = MessageSource.getInstance();
			String message = messageSource.getMessage("com.mock.database.script.sql.error", file.getName());
			throw new MockDatabaseException(message, e);
		}
	}

	public static int runScriptSQL(String path, Charset encoding) {
		return runScriptSQL(new File(path), encoding);
	}

	private static String readFile(File file, Charset encoding) throws IOException {
		String data = Files.toString(file, encoding);
		data = data.trim();
		if (!data.startsWith("begin") && !data.startsWith("BEGIN")) {
			data = "BEGIN " + data + " END;";
		}
		return data;
	}

}
