package jfms.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class LocalIdentity extends Identity {
	private final BooleanProperty isActive = new SimpleBooleanProperty();

	public final boolean getIsActive() {
		return isActive.get();
	}

	public final void setIsActive(boolean isActive) {
		this.isActive.set(isActive);
	}

	public final BooleanProperty isActiveProperty() {
		return isActive;
	}
}
