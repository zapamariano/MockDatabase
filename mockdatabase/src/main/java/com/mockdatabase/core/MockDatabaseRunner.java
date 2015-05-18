package com.mockdatabase.core;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.junit.Ignore;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import com.mockdatabase.annotation.Configure;
import com.mockdatabase.annotation.Rollback;
import com.mockdatabase.annotation.Transaction;
import com.mockdatabase.exception.MockDatabaseException;

public class MockDatabaseRunner extends BlockJUnit4ClassRunner {

	private final String DATABASE_FILE_NAME;

	private final String DATABASE_LOCK_FILE_NAME;

	private final String USER_DIRECTORY;

	public MockDatabaseRunner(Class<?> klass) throws InitializationError {
		super(klass);
		validate();
		DATABASE_FILE_NAME = "test.h2.db";
		DATABASE_LOCK_FILE_NAME = "test.lock.db";
		USER_DIRECTORY = System.getProperty("user.home");
	}

	@Override
	public void run(final RunNotifier notifier) {
		EachTestNotifier testNotifier = new EachTestNotifier(notifier, getDescription());
		try {
			createDatabaseFile();
			buildDatabase();
			super.run(notifier);
		} catch (MockDatabaseException e) {
			testNotifier.addFailure(e);
		} catch (IOException e) {
			MessageSource messageSource = MessageSource.getInstance();
			testNotifier.addFailure(new MockDatabaseException(messageSource.getMessage("com.mock.database.creation.error"), e));
		} catch (Throwable e) {
			MessageSource messageSource = MessageSource.getInstance();
			testNotifier.addFailure(new MockDatabaseException(messageSource.getMessage("com.mock.database.run.error"), e));
		} finally {
			deleteDatabaseFiles();
		}
	}

	@Override
	protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
		Description description = describeChild(method);
		if (method.getAnnotation(Ignore.class) != null) {
			notifier.fireTestIgnored(description);
			return;
		}
		EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
		try {
			eachNotifier.fireTestStarted();
			Transaction transaction = method.getAnnotation(Transaction.class);
			Database.configureSession(transaction);
			Statement statement = methodBlock(method);
			statement.evaluate();
			Database.closeSession(transaction, method.getAnnotation(Rollback.class));
		} catch (AssumptionViolatedException e) {
			eachNotifier.addFailedAssumption(e);
			Database.destroySession();
		} catch (Throwable e) {
			if (e instanceof MockDatabaseException) {
				eachNotifier.addFailure(e);
			} else {
				MessageSource messageSource = MessageSource.getInstance();
				eachNotifier.addFailure(new MockDatabaseException(messageSource.getMessage("com.mock.database.test.method.error"), e));
			}
			Database.destroySession();
		} finally {
			eachNotifier.fireTestFinished();
		}
	}

	private void createDatabaseFile() throws IOException {
		String url = FilenameUtils.concat(USER_DIRECTORY, DATABASE_FILE_NAME);
		File databaseFile = new File(url);
		databaseFile.createNewFile();
	}

	private void deleteDatabaseFiles() {
		String lockUrl = FilenameUtils.concat(USER_DIRECTORY, DATABASE_LOCK_FILE_NAME);
		File databaseLockFile = new File(lockUrl);
		if (databaseLockFile.exists()) {
			databaseLockFile.delete();
		}
		String url = FilenameUtils.concat(USER_DIRECTORY, DATABASE_FILE_NAME);
		File databaseFile = new File(url);
		if (databaseFile.exists()) {
			databaseFile.delete();
		}
	}

	private void validate() throws InitializationError {
		List<Throwable> errors = new ArrayList<Throwable>();
		validateConfigureMethod(errors);
		validateRollbackMethods(errors);
		if (!errors.isEmpty()) {
			throw new InitializationError(errors);
		}
	}

	private void validateConfigureMethod(List<Throwable> errors) {
		List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(Configure.class);
		switch (methods.size()) {
		case 0:
			// No @Configure method is allowed.
			break;
		case 1:
			validatePublicVoidNoArgMethods(Configure.class, false, errors);
			validateConfigureMethodAnnotations(methods.get(0), errors);
			break;
		default:
			MessageSource messageSource = MessageSource.getInstance();
			errors.add(new MockDatabaseException(messageSource.getMessage("com.mock.database.configure.error")));
			break;
		}
	}

	private void validateRollbackMethods(List<Throwable> errors) {
		validatePublicVoidNoArgMethods(Rollback.class, false, errors);
	}

	private void validateConfigureMethodAnnotations(FrameworkMethod method, List<Throwable> errors) {
		Annotation[] annotations = method.getAnnotations();
		if (annotations.length > 1) {
			MessageSource messageSource = MessageSource.getInstance();
			errors.add(new MockDatabaseException(messageSource.getMessage("com.mock.database.configure.annotations.error")));
		}
	}

	private void buildDatabase() throws Throwable {
		List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(Configure.class);
		if (methods.isEmpty()) {
			return;
		}
		Object instance = getTestClass().getJavaClass().newInstance();
		methods.get(0).invokeExplosively(instance);
		Database.build();
	}

}
