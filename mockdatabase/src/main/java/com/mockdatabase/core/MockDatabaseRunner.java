package com.mockdatabase.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import com.mockdatabase.annotation.Configure;
import com.mockdatabase.annotation.Rollback;
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
			super.run(notifier);
		} catch (IOException e) {
			testNotifier.addFailure(new MockDatabaseException("An error ocurred while creating the database", e));
		} catch (Throwable e) {
			testNotifier.addFailure(new MockDatabaseException("An error ocurred while starting the test", e));
		} finally {
			deleteDatabaseFiles();
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
		databaseFile.delete();
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
		if (methods.size() > 1) {
			errors.add(new MockDatabaseException("There can not be more than one method annotated with @Configure"));
		} else {
			validatePublicVoidNoArgMethods(Configure.class, false, errors);
		}
	}

	private void validateRollbackMethods(List<Throwable> errors) {
		validatePublicVoidNoArgMethods(Rollback.class, false, errors);
	}

}
