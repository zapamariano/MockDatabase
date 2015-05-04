package com.mockdatabase.core;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

public class Database {

	private static List<Table<?, ? extends Serializable>> tables = new ArrayList<Table<?, ? extends Serializable>>();

	private static SessionFactory sessionFactory;

	protected static void build() {
		Configuration configuration = new Configuration();
		configuration.configure(Database.class
				.getResource("/hibernate.cfg.xml"));
		for (Table<?, ? extends Serializable> table : tables) {
			if (table.getMappingXmlFile() == null) {
				configuration.addAnnotatedClass(table.getEntityClass());
			} else {
				configuration.addFile(table.getMappingXmlFile());
			}
		}
		ServiceRegistry serviceRegistry = new ServiceRegistryBuilder()
				.applySettings(configuration.getProperties())
				.buildServiceRegistry();
		sessionFactory = configuration.buildSessionFactory(serviceRegistry);
	}

	protected static Session getSession() {
		return sessionFactory.getCurrentSession();
	}

	public static <T, PK extends Serializable> Table<T, PK> createTable(
			Class<T> entityClass, PK idType) {
		Table<T, PK> table = new Table<T, PK>(entityClass);
		tables.add(table);
		return table;
	}

	public static <T, PK extends Serializable> Table<T, PK> createTable(
			Class<T> entityClass, PK idType, File mappingXmlFile) {
		Table<T, PK> table = new Table<T, PK>(entityClass, mappingXmlFile);
		tables.add(table);
		return table;
	}

	public static <T, PK extends Serializable> Table<T, PK> createTable(
			Class<T> entityClass, PK idType, String mappingXmlFileUrl) {
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

}
