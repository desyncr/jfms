package jfms.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.image.Image;

import jfms.config.Config;

public class Icons {
	private static final Logger LOG = Logger.getLogger(Icons.class.getName());
	private static Icons instance;

	private final String theme;
	private final boolean highDpi;

	// main toolbar
	private final Image identitiesIcon;
	private final Image boardsIcon;
	private final Image findIcon;
	private final Image settingsIcon;

	// notifications
	private final Image infoIcon;
	private final Image warnIcon;
	private final Image closeIcon;

	// message pane
	private final Image boardFolderIcon;
	private final Image boardIcon;
	private final Image outboxIcon;
	private final Image draftIcon;
	private final Image sentIcon;
	private final Image messageReadIcon;
	private final Image messageUnreadIcon;
	private final Image messageQueuedIcon;
	private final Image messageSentIcon;
	private final Image starredIcon;

	// new message window
	private final Image sendMessageIcon;
	private final Image saveDraftIcon;

	// message toolbar
	private final Image newMessageIcon;
	private final Image replyIcon;
	private final Image threadIcon;
	private final Image muteIcon;
	private final Image signatureIcon;
	private final Image linkIcon;
	private final Image attachmentIcon;
	private final Image emoticonIcon;
	private final Image fontIcon;

	// status bar
	private final Image onlineIcon;
	private final Image offlineIcon;

	// avatar
	private final Image genericAvatar;

	// application icons
	private final List<Image> appIcons = new ArrayList<>();


	public enum NetworkStatus {
		UNKNOWN,
		ONLINE,
		OFFLINE
	}

	public enum NotificationType {
		NONE,
		WARNING,
		INFORMATION
	}

	public static synchronized Icons getInstance() {
		if (instance == null) {
			instance = new Icons();
		}

		return instance;
	}

	private Icons() {
		theme = Config.getInstance().getIconSet();

		identitiesIcon = getImage("user-properties.png");
		highDpi = identitiesIcon != null && identitiesIcon.getWidth() > 30;

		boardsIcon = getImage("applications-internet_22x22.png");
		findIcon = getImage("edit-find.png");
		settingsIcon = getImage("configure.png");

		infoIcon = getImage("dialog-information.png");
		warnIcon = getImage("dialog-warning.png");
		closeIcon = getImage("dialog-close.png");

		boardFolderIcon = getImage("folder-remote.png");
		boardIcon = getImage("folder.png");
		outboxIcon = getImage("mail-folder-outbox.png");
		draftIcon = getImage("folder-text.png");
		sentIcon = getImage("mail-folder-sent.png");
		messageReadIcon = getImage("mail-read.png");
		messageUnreadIcon = getImage("mail-unread.png");
		messageQueuedIcon = getImage("mail-queued.png");
		messageSentIcon = getImage("mail-sent.png");
		starredIcon = getImage("rating.png");

		sendMessageIcon = getImage("mail-send.png");
		saveDraftIcon = getImage("document-save.png");

		newMessageIcon = getImage("mail-message-new.png");
		replyIcon = getImage("mail-reply-sender.png");
		threadIcon = getImage("view-list-tree.png");
		muteIcon = getImage("audio-volume-muted.png");
		signatureIcon = getImage("document-sign.png");
		linkIcon = getImage("applications-internet.png");
		attachmentIcon = getImage("mail-attachment.png");
		emoticonIcon = getImage("face-smile.png");
		fontIcon = getImage("gtk-select-font.png");

		onlineIcon = getImage("network-connect.png");
		offlineIcon = getImage("network-disconnect.png");

		int genericAvatarSize = 75;
		if (highDpi) {
			genericAvatarSize *= 2;
		}
		genericAvatar = getImage("user-identity.png",
				genericAvatarSize, genericAvatarSize, false);

		String[] appIconNames = new String[]{
			"jfms_16.png", "jfms_32.png", "jfms_64.png"};
		for (String s : appIconNames) {
			Image img = getImage(s, -1, -1, false);
			if (img != null) {
				appIcons.add(img);
			}
		}
	}

	public boolean isHighDpi() {
		return highDpi;
	}

	public Image getIdentitiesIcon() {
		return identitiesIcon;
	}

	public Image getBoardsIcon() {
		return boardsIcon;
	}

	public Image getFindIcon() {
		return findIcon;
	}

	public Image getSettingsIcon() {
		return settingsIcon;
	}

	public Image getNotificationIcon(NotificationType type) {
		Image image = null;
		switch (type) {
		case WARNING:
			image = warnIcon;
			break;
		case INFORMATION:
			image = infoIcon;
			break;
		}

		return image;
	}

	public Image getCloseIcon() {
		return closeIcon;
	}

	public Image getBoardFolderIcon() {
		return boardFolderIcon;
	}

	public Image getBoardIcon() {
		return boardIcon;
	}

	public Image getOutboxIcon() {
		return outboxIcon;
	}

	public Image getDraftIcon() {
		return draftIcon;
	}

	public Image getSentIcon() {
		return sentIcon;
	}

	public Image getStarredIcon() {
		return starredIcon;
	}


	public Image getMessageIcon(Message.Status status) {
		Image image = null;
		switch (status) {
		case READ:
			image = messageReadIcon;
			break;
		case UNREAD:
			image = messageUnreadIcon;
			break;
		case QUEUED:
			image = messageQueuedIcon;
			break;
		case SENT:
			image = messageSentIcon;
			break;
		}

		return image;
	}

	public Image getSendMessageIcon() {
		return sendMessageIcon;
	}

	public Image getSaveDraftIcon() {
		return saveDraftIcon;
	}

	public Image getNewMessageIcon() {
		return newMessageIcon;
	}

	public Image getReplyIcon() {
		return replyIcon;
	}

	public Image getThreadIcon() {
		return threadIcon;
	}

	public Image getMuteIcon() {
		return muteIcon;
	}

	public Image getEmoticonIcon() {
		return emoticonIcon;
	}

	public Image getFontIcon() {
		return fontIcon;
	}

	public Image getSignatureIcon() {
		return signatureIcon;
	}

	public Image getLinkIcon() {
		return linkIcon;
	}

	public Image getAttachmentIcon() {
		return attachmentIcon;
	}

	public Image getNetworkIcon(NetworkStatus status) {
		Image image = null;
		switch (status) {
		case ONLINE:
			image = onlineIcon;
			break;
		case OFFLINE:
			image = offlineIcon;
			break;
		}

		return image;
	}

	public Image getGenericAvatar() {
		return genericAvatar;
	}

	public List<Image> getAppIcons() {
		return appIcons;
	}

	private Image getImage(String name,
			double requestedWidth, double requestedHeight, boolean allowHiDpi) {

		final Config config = Config.getInstance();
		Image image = null;
		try (InputStream is =
				config.getIconInputStream(theme, name, allowHiDpi)) {

			if (is != null) {
				if (requestedWidth > 0 && requestedHeight > 0) {
					image = new Image(is, requestedWidth, requestedHeight,
							true, true);
				} else {
					image = new Image(is);
				}
			}
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Failed to load " + name + " icon ", e);
		}

		return image;
	}

	private Image getImage(String name) {
		return getImage(name, -1, -1, true);
	}
}
