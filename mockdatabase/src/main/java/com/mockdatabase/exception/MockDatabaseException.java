package com.mockdatabase.exception;

@SuppressWarnings("serial")
public class MockDatabaseException extends RuntimeException {

	public MockDatabaseException() {
		super();
	}

	public MockDatabaseException(String message) {
		super(message);
	}

	public MockDatabaseException(String message, Throwable cause) {
		super(message, cause);
	}

	public MockDatabaseException(Throwable cause) {
		super(cause);
	}

}
