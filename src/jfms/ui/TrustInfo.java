package jfms.ui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


public class TrustInfo {
	private final StringProperty name = new SimpleStringProperty();
	private final StringProperty ssk = new SimpleStringProperty();
	private final IntegerProperty weight = new SimpleIntegerProperty(-1);
	private final IntegerProperty trustListTrust = new SimpleIntegerProperty(-1);
	private final StringProperty trustListTrustComment = new SimpleStringProperty();
	private final IntegerProperty messageTrust = new SimpleIntegerProperty(-1);
	private final StringProperty messageTrustComment = new SimpleStringProperty();

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

	public final int getWeigth() {
		return weight.get();
	}

	public final void setWeight(int weight) {
		this.weight.set(weight);
	}

	public final IntegerProperty weightProperty() {
		return weight;
	}

	public final int getTrustListTrust() {
		return trustListTrust.get();
	}

	public final void setTrustListTrust(int level) {
		this.trustListTrust.set(level);
	}

	public final IntegerProperty trustListTrustProperty() {
		return trustListTrust;
	}

	public final String getTrustListTrustComment() {
		return trustListTrustComment.get();
	}

	public final void setTrustListTrustComment(String comment) {
		this.trustListTrustComment.set(comment);
	}

	public final StringProperty trustListTrustCommentProperty() {
		return trustListTrustComment;
	}

	public final int getMessageTrust() {
		return messageTrust.get();
	}

	public final void setMessageTrust(int level) {
		this.messageTrust.set(level);
	}

	public final IntegerProperty messageTrustProperty() {
		return messageTrust;
	}

	public final String getMessageTrustComment() {
		return messageTrustComment.get();
	}

	public final void setMessageTrustComment(String comment) {
		this.messageTrustComment.set(comment);
	}

	public final StringProperty messageTrustCommentProperty() {
		return messageTrustComment;
	}
}
