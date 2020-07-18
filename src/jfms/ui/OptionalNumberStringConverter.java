package jfms.ui;

import javafx.util.converter.NumberStringConverter;

public class OptionalNumberStringConverter extends NumberStringConverter {
	public OptionalNumberStringConverter() {
		super();
	}

	@Override
	public String toString(Number value) {
		if (value.longValue() >= 0) {
			return super.toString(value);
		} else {
			return "";
		}
	}
}
