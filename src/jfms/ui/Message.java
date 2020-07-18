package jfms.ui;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Message {
	private static final Logger LOG = Logger.getLogger(Message.class.getName());

	public enum Status {
		UNREAD,
		READ,
		QUEUED,
		DRAFT,
		SENT
	}

	private int storeId = -1;
	private int identityId = -1;

	private final StringProperty subject = new SimpleStringProperty();
	private final StringProperty from = new SimpleStringProperty();
	private final StringProperty fromShort = new SimpleStringProperty();
	private final StringProperty date = new SimpleStringProperty();
	private final StringProperty messageId = new SimpleStringProperty();
	private final StringProperty parentMessageId = new SimpleStringProperty();
	private final IntegerProperty localTrustLevel = new SimpleIntegerProperty(-1);
	private final IntegerProperty peerTrustLevel = new SimpleIntegerProperty(-1);
	private final StringProperty indexDate = new SimpleStringProperty();
	private final IntegerProperty index = new SimpleIntegerProperty(-1);
	private final StringProperty replyBoard = new SimpleStringProperty();
	private final StringProperty boards = new SimpleStringProperty();
	private final StringProperty body = new SimpleStringProperty();
	private final BooleanProperty isNew = new SimpleBooleanProperty();
	private final BooleanProperty isStarred = new SimpleBooleanProperty();
	private final StringProperty lastReplyDate = new SimpleStringProperty();

	private List<String> boardList;

	public void clear() {
		storeId = -1;
		identityId = -1;

		subject.set("");
		from.set("");
		fromShort.set("");
		date.set("");
		messageId.set("");
		parentMessageId.set("");
		localTrustLevel.set(-1);
		peerTrustLevel.set(-1);
		indexDate.set("");
		index.set(-1);
		replyBoard.set("");
		boards.set("");
		body.set("");
		isNew.set(false);
		isStarred.set(false);
		lastReplyDate.set("");

		boardList = null;
	}

	public void setAll(Message m) {
		storeId = m.storeId;
		identityId = m.identityId;

		subject.set(m.subject.get());
		from.set(m.from.get());
		fromShort.set(m.fromShort.get());
		date.set(m.date.get());
		messageId.set(m.messageId.get());
		parentMessageId.set(m.parentMessageId.get());
		localTrustLevel.set(m.localTrustLevel.get());
		peerTrustLevel.set(m.peerTrustLevel.get());
		indexDate.set(m.indexDate.get());
		index.set(m.index.get());
		replyBoard.set(m.replyBoard.get());
		boards.set(m.boards.get());
		body.set(m.body.get());
		isNew.set(m.isNew.get());
		isStarred.set(m.isStarred.get());
		lastReplyDate.set(m.lastReplyDate.get());

		boardList = m.boardList;
	}

	public int getStoreId() {
		return storeId;
	}

	public void setStoreId(int dbId) {
		this.storeId = dbId;
	}

	public int getIdentityId() {
		return identityId;
	}

	public void setIdentityId(int identityId) {
		this.identityId = identityId;
	}

	public final String getSubject() {
		return subject.get();
	}

	public final void setSubject(String subject) {
		this.subject.set(subject);
	}

	public final StringProperty subjectProperty() {
		return subject;
	}

	public final String getFrom() {
		return from.get();
	}

	public final void setFrom(String from) {
		this.from.set(from);
		if (from != null) {
			int idx = from.lastIndexOf('@');
			if (idx != -1) {
				fromShort.set(from.substring(0, idx));
			} else {
				fromShort.set(from);
			}
		} else {
			fromShort.set(null);
		}
	}

	public final StringProperty fromProperty() {
		return from;
	}

	public final String getFromShort() {
		return fromShort.get();
	}

	public final StringProperty fromShortProperty() {
		return fromShort;
	}

	public final String getDate() {
		return date.get();
	}

	public final void setDate(String date) {
		this.date.set(date);
	}

	public final StringProperty dateProperty() {
		return date;
	}

	public final String getMessageId() {
		return messageId.get();
	}

	public final void setMessageId(String messageId) {
		this.messageId.set(messageId);
	}

	public final StringProperty messageIdProperty() {
		return messageId;
	}

	public final String getParentMessageId() {
		return parentMessageId.get();
	}

	public final void setParentMessageId(String parentMessageId) {
		this.parentMessageId.set(parentMessageId);
	}

	public final StringProperty parentMessageIdProperty() {
		return parentMessageId;
	}

	public final int getLocalTrustLevel() {
		return localTrustLevel.get();
	}

	public final void setLocalTrustLevel(int trustLevel) {
		this.localTrustLevel.set(trustLevel);
	}

	public final IntegerProperty localTrustLevelProperty() {
		return localTrustLevel;
	}

	public final int getPeerTrustLevel() {
		return peerTrustLevel.get();
	}

	public final void setPeerTrustLevel(int trustLevel) {
		this.peerTrustLevel.set(trustLevel);
	}

	public final IntegerProperty peerTrustLevelProperty() {
		return peerTrustLevel;
	}

	public final String getIndexDate() {
		return indexDate.get();
	}

	public final void setIndexDate(String indexDate) {
		this.indexDate.set(indexDate);
	}

	public final StringProperty indexDateProperty() {
		return indexDate;
	}

	public final int getIndex() {
		return index.get();
	}

	public final void setIndex(int index) {
		this.index.set(index);
	}

	public final IntegerProperty indexProperty() {
		return index;
	}

	public final String getReplyBoard() {
		return replyBoard.get();
	}

	public final void setReplyBoard(String replyBoard) {
		this.replyBoard.set(replyBoard);
	}

	public final StringProperty replyBoardProperty() {
		return replyBoard;
	}

	public List<String> getBoardList() {
		return boardList;
	}

	public final void setBoardList(List<String> boardList) {
		this.boardList = boardList;
	}

	public final String getBoards() {
		return boards.get();
	}

	public final void setBoards() {
		if (boardList == null) {
			LOG.log(Level.WARNING, "boardList not set");
			return;
		}

		// XXX copied from MessageWindow
		// TODO sort instead of random DB order
		StringBuilder str = new StringBuilder();
		str.append(getReplyBoard());
		for (String board : boardList) {
			if (!board.equals(getReplyBoard())) {
				str.append(", ");
				str.append(board);
			}
		}

		boards.set(str.toString());
	}

	public final StringProperty boardsProperty() {
		return boards;
	}

	public final String getBody() {
		return body.get();
	}

	public final void setBody(String body) {
		this.body.set(body);
	}

	public final StringProperty bodyProperty() {
		return body;
	}

	public final boolean getIsNew() {
		return isNew.get();
	}

	public final void setIsNew(boolean isNew) {
		this.isNew.set(isNew);
	}

	public final BooleanProperty isNewProperty() {
		return isNew;
	}

	public final boolean getIsStarred() {
		return isStarred.get();
	}

	public final void setIsStarred(boolean isStarred) {
		this.isStarred.set(isStarred);
	}

	public final BooleanProperty isStarredProperty() {
		return isStarred;
	}

	public final String getLastReplyDate() {
		return lastReplyDate.get();
	}

	public final void setLastReplyDate(String lastReplyDate) {
		this.lastReplyDate.set(lastReplyDate);
	}

	public final StringProperty lastReplyDateProperty() {
		return lastReplyDate;
	}
}
