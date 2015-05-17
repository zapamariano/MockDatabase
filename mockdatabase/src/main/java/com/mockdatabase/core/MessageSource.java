package com.mockdatabase.core;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class MessageSource {

	private ResourceBundle bundle;

	private static MessageSource instance;

	private MessageSource() {
		Locale locale = new Locale("en", "US");
		bundle = ResourceBundle.getBundle("messages", locale);
	}

	public static MessageSource getInstance() {
		if (instance == null) {
			instance = new MessageSource();
		}
		return instance;
	}

	public String getMessage(String key) {
		return bundle.getString(key);
	}

	public String getMessage(String key, Object... arguments) {
		return MessageFormat.format(getMessage(key), arguments);
	}

}
