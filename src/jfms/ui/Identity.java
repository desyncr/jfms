package jfms.ui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Identity {
	private final IntegerProperty id = new SimpleIntegerProperty();
	private final StringProperty name = new SimpleStringProperty();
	private final StringProperty ssk = new SimpleStringProperty();
	private final IntegerProperty localTrustListTrust = new SimpleIntegerProperty();
	private final IntegerProperty localMessageTrust = new SimpleIntegerProperty();
	private final IntegerProperty peerTrustListTrust = new SimpleIntegerProperty();
	private final IntegerProperty peerMessageTrust = new SimpleIntegerProperty();

	public final int getId() {
		return id.get();
	}

	public final void setId(int id) {
		this.id.set(id);
	}

	public final IntegerProperty idProperty() {
		return id;
	}

	public final String getName() {
		return name.get();
	}

	public final void setName(String name) {
		this.name.set(name);
	}

	public final StringProperty nameProperty() {
		return name;
	}

	public final String getSsk() {
		return ssk.get();
	}

	public final void setSsk(String ssk) {
		this.ssk.set(ssk);
	}

	public final StringProperty sskProperty() {
		return ssk;
	}

	public final int getLocalTrustListTrust() {
		return localTrustListTrust.get();
	}

	public final void setLocalTrustListTrust(int trustListTrust) {
		this.localTrustListTrust.set(trustListTrust);
	}

	public final IntegerProperty localTrustListTrustProperty() {
		return localTrustListTrust;
	}

	public final int getLocalMessageTrust() {
		return localMessageTrust.get();
	}

	public final void setLocalMessageTrust(int messageTrust) {
		this.localMessageTrust.set(messageTrust);
	}

	public final IntegerProperty localMessageTrustProperty() {
		return localMessageTrust;
	}

	public final int getPeerTrustListTrust() {
		return peerTrustListTrust.get();
	}

	public final void setPeerTrustListTrust(int trustListTrust) {
		this.peerTrustListTrust.set(trustListTrust);
	}

	public final IntegerProperty peerTrustListTrustProperty() {
		return peerTrustListTrust;
	}

	public final int getPeerMessageTrust() {
		return peerMessageTrust.get();
	}

	public final void setPeerMessageTrust(int messageTrust) {
		this.peerMessageTrust.set(messageTrust);
	}

	public final IntegerProperty peerMessageTrustProperty() {
		return peerMessageTrust;
	}
}
