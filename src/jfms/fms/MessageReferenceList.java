package jfms.fms;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jfms.config.Config;
import jfms.store.Store;

public class MessageReferenceList {
	private static final Logger LOG = Logger.getLogger(MessageReferenceList.class.getName());

	private final Set<MessageReference> messageReferences = new HashSet<>();
	private final Set<MessageReference> seenReferences = new HashSet<>();

	public synchronized void addMessageToDownload(MessageReference message) {
		// Clear the list of boards to make sure we won't have duplicates
		// in messageReferences that differ only in boards
		final MessageReference strippedRef = message.strippedCopy();
		if (!seenReferences.add(strippedRef)) {
			return;
		}

		TrustManager trustManager = FmsManager.getInstance().getTrustManager();
		final Config config = Config.getInstance();

		// List of boards in the message list may be used to decided whether we
		// download a message. The message itself contains the authorative list
		// of boards.

		int peerMessageTrust = trustManager.getPeerMessageTrust(
			message.getIdentityId());

		boolean isTrusted;
		if (config.getDownloadMessagesWithoutTrust()) {
			isTrusted = peerMessageTrust == -1 ||
				peerMessageTrust >= config.getMinPeerMessageTrust();
		} else {
			isTrusted = peerMessageTrust != -1 &&
				peerMessageTrust >= config.getMinPeerMessageTrust();
		}

		if (isTrusted) {
			messageReferences.add(message);
		}
	}

	public void clear() {
		messageReferences.clear();
		seenReferences.clear();
	}

	public int size() {
		return messageReferences.size();
	}

	public boolean isEmpty() {
		return messageReferences.isEmpty();
	}

	public void cleanup() {
		Store store = FmsManager.getInstance().getStore();

		LocalDate oldestMessageDate = LocalDate.now(ZoneOffset.UTC)
			.minusDays(Config.getInstance().getMaxMessageAge());

		int oldCount = 0;
		int existsCount = 0;
		int totalCount = messageReferences.size();

		Iterator<MessageReference> iter = messageReferences.iterator();
		while (iter.hasNext()) {
			MessageReference msg = iter.next();

			boolean remove = false;
			if (msg.getDate().compareTo(oldestMessageDate) < 0) {
				oldCount++;
				remove = true;
			}

			if (!remove && store.messageExists(
						msg.getIdentityId(),
						msg.getDate(),
						msg.getIndex())) {
				existsCount++;
				remove = true;
			}

			if (remove) {
				iter.remove();
			}
		}

		LOG.log(Level.FINER, "Found {0} messages: "
				+ "{1} new, {2} too old, {3} already exist", new Object[]{
				totalCount, messageReferences.size(), oldCount, existsCount});
	}

	public MessageReference remove() {
		Iterator<MessageReference> iter = messageReferences.iterator();
		if (iter.hasNext()) {
			MessageReference msg = iter.next();
			iter.remove();
			return msg;
		} else {
			return null;
		}
	}
}
