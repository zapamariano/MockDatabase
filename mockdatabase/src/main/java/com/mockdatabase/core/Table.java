package com.mockdatabase.core;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import org.hibernate.Session;

public class Table<T, PK extends Serializable> {

	private Class<T> entityClass;

	private File mappingXmlFile;

	public Table(Class<T> entityClass) {
		this.entityClass = entityClass;
	}

	public Table(Class<T> entityClass, File mappingXmlFile) {
		this.entityClass = entityClass;
		this.mappingXmlFile = mappingXmlFile;
	}

	public Table(Class<T> entityClass, String mappingXmlFileUrl) {
		this(entityClass, new File(mappingXmlFileUrl));
	}

	protected Class<T> getEntityClass() {
		return entityClass;
	}

	protected File getMappingXmlFile() {
		return mappingXmlFile;
	}

	@SuppressWarnings("unchecked")
	public PK save(T entity) {
		return (PK) Database.getSession().save(entity);
	}

	public void saveOrUpdate(T entity) {
		Database.getSession().saveOrUpdate(entity);
	}

	public void delete(T entity) {
		Database.getSession().delete(entity);
	}

	@SuppressWarnings("unchecked")
	public T getById(PK id) {
		return (T) Database.getSession().get(entityClass, id);
	}

	@SuppressWarnings("unchecked")
	public T findById(PK id) {
		return (T) Database.getSession().load(entityClass, id);
	}

	@SuppressWarnings("unchecked")
	public List<T> findAll() {
		return Database.getSession().createQuery("from " + entityClass.getSimpleName()).list();
	}

	@SuppressWarnings("unchecked")
	public List<T> filter(String whereClause) {
		Session session = Database.getSession();
		String fromClause = "from " + entityClass.getSimpleName();
		whereClause = whereClause.trim().toLowerCase();
		if (whereClause.startsWith("where")) {
			return session.createQuery(fromClause + " " + whereClause).list();
		} else {
			return session.createQuery(fromClause + " where " + whereClause).list();
		}
	}

	public long count() {
		String query = "select count(e) from " + entityClass.getSimpleName() + " as e";
		return (Long) Database.getSession().createQuery(query).uniqueResult();
	}

}
