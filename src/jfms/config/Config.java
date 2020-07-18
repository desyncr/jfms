package jfms.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Config {
	public static final int FCP_HOST    =  0;
	public static final int FCP_PORT    =  1;
	public static final int DEFAULT_ID  =  2;
	public static final int MESSAGEBASE =  3;
	public static final int WEBVIEW     =  4;
	public static final int ICON_SET    =  5;
	public static final int OFFLINE     =  6;
	public static final int DL_MSGLISTS_WITHOUT_TRUST = 7;
	public static final int DL_MESSAGES_WITHOUT_TRUST = 8;
	public static final int INACTIVITY_RETRY_INTERVAL = 10;
	public static final int INACTIVITY_TIMEOUT = 9;
	public static final int MAX_IDENTITY_AGE = 11;
	public static final int MAX_MESSAGE_AGE = 12;
	public static final int MAX_FCP_REQUESTS = 13;
	public static final int LOG_LEVEL =   14;
	public static final int MIN_LOCAL_TRUSTLIST_TRUST = 15;
	public static final int MIN_LOCAL_MESSAGE_TRUST = 16;
	public static final int MIN_PEER_TRUSTLIST_TRUST = 17;
	public static final int MIN_PEER_MESSAGE_TRUST= 18;
	public static final int SHOW_THREADS = 19;
	public static final int SORT_BY_MOST_RECENT_IN_THREAD = 20;
	public static final int MONOSPACE_FONT = 21;
	public static final int EMOTICONS = 22;
	public static final int MUTE_QUOTES = 23;
	public static final int SHOW_SIGNATURE = 24;
	public static final int DETECT_LINKS = 25;
	public static final int SHOW_ATTACHMENTS = 26;
	public static final int WINDOW_SIZE = 27;
	public static final int FORCE_HIDPI_ICONS = 28;
	public static final int FORCE_HIDPI_EMOTICONS = 29;
	public static final int DOWNLOAD_PRIORITY = 30;
	public static final int UPLOAD_PRIORITY = 31;
	public static final int FOLDER_PANE_WIDTH = 32;
	public static final int HEADER_PANE_HEIGHT = 33;
	public static final int FAST_MESSAGE_CHECK = 34;
	public static final int SHOW_SUBSCRIBED_ONLY = 35;
	public static final int COLLAPSE_READ_THREADS = 36;
	public static final int SHOW_AVATARS = 37;
	public static final int EXCLUDE_NULL_TRUST  = 38;
	public static final int INDIRECT_TRUST_WEIGHT = 39;
	public static final int MIN_MESSAGE_DELAY = 40;
	public static final int MAX_MESSAGE_DELAY = 41;
	public static final int ID_SIZE     = 42;

	public static final int PORT_MAX = 65535;

	private static final Config instance = new Config();
	private static final Logger LOG = Logger.getLogger(Config.class.getName());

	private boolean dirty = false;
	private final ConfigEntry[] entries;
	private final String[] values;
	private final boolean[] booleanValues;
	private final int[] intValues;
	private final Set<String> defaultIconSets;


	public static Config getInstance() {
		return instance;
	}

	private Config() {
		defaultIconSets = new TreeSet<>(Arrays.asList("oxygen", "breeze"));
		List<String> iconSets = new ArrayList<>(defaultIconSets);
		File iconsDir = new File("icons/");
		File[] iconDirs = iconsDir.listFiles();
		if (iconDirs != null) {
			for (File f : iconDirs) {
				final String name = f.getName();
				if (f.isDirectory() && !defaultIconSets.contains(name)) {
					iconSets.add(name);
				}
			}
		}

		values = new String[ID_SIZE];
		booleanValues = new boolean[ID_SIZE];
		intValues = new int[ID_SIZE];
		entries = new ConfigEntry[ID_SIZE];

		// Freenet Settings
		entries[FCP_HOST] = new ConfigEntry(
					"fcp.host",
					ConfigType.STRING,
					"FCP Host",
					Constants.DEFAULT_FCP_HOST,
					"IP Address of your Freenet Node");
		entries[FCP_PORT] = new ConfigEntry(
					"fcp.port",
					ConfigType.INT,
					"FCP Port",
					Constants.DEFAULT_FCP_PORT,
					new IntRangeValidator(0, PORT_MAX),
					"FCP Port of your Freenet Node");

		// FMS Settings
		entries[DEFAULT_ID] = new ConfigEntry(
				"fms.default_id",
				ConfigType.INT,
				"Default Identity",
				Constants.DEFAULT_DEFAULT_ID,
				null);
		entries[MESSAGEBASE] = new ConfigEntry(
				"fms.messagebase",
				ConfigType.STRING,
				"Message Base",
				Constants.DEFAULT_MESSAGEBASE,
				"FMS Message Base");
		entries[FAST_MESSAGE_CHECK] = new ConfigEntry(
				"fms.fast_message_check",
				ConfigType.BOOLEAN,
				"Enable fast message check",
				Constants.DEFAULT_TRUE,
				"Perform a check for new messages before downloading trust lists on startup");
		entries[INACTIVITY_RETRY_INTERVAL] = new ConfigEntry(
				"fms.inactivity_retry_interval",
				ConfigType.INT,
				"Inactivity retry interval",
				Constants.DEFAULT_INACTIVITY_RETRY_INTERVAL,
				new IntRangeValidator(1, 99),
				"Number of days between trying to download inactive "
				+ "identities");
		entries[INACTIVITY_TIMEOUT] = new ConfigEntry(
				"fms.inactivity_timeout",
				ConfigType.INT,
				"Inactivity timeout",
				Constants.DEFAULT_INACTIVITY_TIMEOUT,
				new IntRangeValidator(1, 99),
				"Number of days not seen after which an identity will be "
				+ "marked as inactive");
		entries[MAX_IDENTITY_AGE] = new ConfigEntry(
				"fms.max_identity_age",
				ConfigType.INT,
				"Maximum identity age",
				Constants.DEFAULT_MAX_IDENTITY_AGE,
				new IntRangeValidator(0, 99),
				"Maximum number of days backward that identities will be "
				+ "downloaded");
		entries[MAX_MESSAGE_AGE] = new ConfigEntry(
				"fms.max_message_age",
				ConfigType.INT,
				"Maximum message age",
				Constants.DEFAULT_MAX_MESSAGE_AGE,
				new IntRangeValidator(0, 9999),
				"Maximum number of days backward that messages will be "
				+ "downloaded");
		entries[MAX_FCP_REQUESTS] = new ConfigEntry(
				"fms.max_fcp_requests",
				ConfigType.INT,
				"Max FCP requests",
				Constants.DEFAULT_MAX_FCP_REQUESTS,
				new IntRangeValidator(1, 99),
				"Maximum number of concurrent FCP download requests");

		ConfigEntryValidator prioValidator =
			new ChoiceValidator(Arrays.asList("default",
					"0 (maximum priority)",
					"1", "2", "3", "4", "5", "6 (minimum priority)"));
		entries[DOWNLOAD_PRIORITY] = new ConfigEntry(
				"fms.download_priority",
				ConfigType.CHOICE,
				"Download priority",
				Constants.DEFAULT_DOWNLOAD_PRIORITY,
				prioValidator,
				"Priority of downloads");
		entries[UPLOAD_PRIORITY] = new ConfigEntry(
				"fms.upload_priority",
				ConfigType.CHOICE,
				"Upload priority",
				Constants.DEFAULT_UPLOAD_PRIORITY,
				prioValidator,
				"Priority of uploads");

		entries[MIN_MESSAGE_DELAY] = new ConfigEntry(
				"fms.min_message_delay",
				ConfigType.INT,
				"Minimum message delay",
				"0",
				new IntRangeValidator(0, Constants.MAX_MESSAGE_DELAY),
				"Minimum delay for sending messages in minutes");

		entries[MAX_MESSAGE_DELAY] = new ConfigEntry(
				"fms.max_message_delay",
				ConfigType.INT,
				"Maximum message delay",
				"0",
				new IntRangeValidator(0, Constants.MAX_MESSAGE_DELAY),
				"Maximum delay for sending messages in minutes");

		// Trust Settings
		entries[MIN_LOCAL_TRUSTLIST_TRUST] = new ConfigEntry(
				"fms.min_local_trustlist_trust",
				ConfigType.INT,
				"Minimum local trust list trust",
				Constants.DEFAULT_MIN_LOCAL_TRUSTLIST_TRUST,
				"Minimum local trust level of identity to be included in\n"
				+ "trust calculation");
		entries[MIN_LOCAL_MESSAGE_TRUST] = new ConfigEntry(
				"fms.min_local_message_trust",
				ConfigType.INT,
				"Minimum local message trust",
				Constants.DEFAULT_MIN_LOCAL_MESSAGE_TRUST,
				"Minimum local trust level of identity to be included in\n"
				+ "message download");
		entries[MIN_PEER_TRUSTLIST_TRUST] = new ConfigEntry(
				"fms.min_peer_trustlist_trust",
				ConfigType.INT,
				"Minimum peer trust list trust",
				Constants.DEFAULT_MIN_PEER_TRUSTLIST_TRUST,
				"Minimum peer trust level of identity to be included in\n"
				+ "trust calculation");
		entries[MIN_PEER_MESSAGE_TRUST] = new ConfigEntry(
				"fms.min_peer_message_trust",
				ConfigType.INT,
				"Minimum peer message trust",
				Constants.DEFAULT_MIN_PEER_MESSAGE_TRUST,
				"Minimum peer trust level of identity to be included in\n"
				+ "message download");
		entries[DL_MSGLISTS_WITHOUT_TRUST] = new ConfigEntry(
				"fms.dl_msglists_without_trust",
				ConfigType.BOOLEAN,
				"Download message lists without trust",
				Constants.DEFAULT_TRUE,
				"Download messages lists from identities that appear in trust\n"
				+ "lists but have neither peer nor local trust level assigned");
		entries[DL_MESSAGES_WITHOUT_TRUST] = new ConfigEntry(
				"fms.dl_messages_without_trust",
				ConfigType.BOOLEAN,
				"Download messages without trust",
				Constants.DEFAULT_TRUE,
				"Download messages from identities that appear in trust\n"
				+ "lists but have neither peer nor local trust level assigned");
		entries[EXCLUDE_NULL_TRUST] = new ConfigEntry(
				"fms.exclude_null_trust",
				ConfigType.BOOLEAN,
				"Exclude NULL trust from trustlists trust calculation",
				Constants.DEFAULT_TRUE,
				"If enabled NULL trust entries will be excluded,\n"
				+ "otherwise neutral value of 50 is assumed.");
		entries[INDIRECT_TRUST_WEIGHT] = new ConfigEntry(
				"fms.indirect_trust_weight",
				ConfigType.INT,
				"Weight of indirect trustlist trusts",
				Constants.DEFAULT_INDIRECT_TRUST_WEIGHT,
				"Weight of indirectly trusted identities:\n"
				+ "0 uses only locally trusted trustlists\n"
				+ "100 gives full weight to indirectly trusted trustlists");

		// UI Settings
		entries[ICON_SET] = new ConfigEntry(
				"ui.icon_set",
				ConfigType.CHOICE,
				"Icon Set",
				Constants.DEFAULT_ICON_SET,
				new ChoiceValidator(iconSets),
				"Icons used in toolbars, etc. (requires restart)");
		entries[FORCE_HIDPI_ICONS] = new ConfigEntry(
				"ui.force_hidpi_icons",
				ConfigType.BOOLEAN,
				"Always use high DPI icons",
				Constants.DEFAULT_FALSE,
				"Use high DPI icons even if no high DPI display is detected (requires restart)");
		entries[FORCE_HIDPI_EMOTICONS] = new ConfigEntry(
				"ui.force_hidpi_emoticons",
				ConfigType.BOOLEAN,
				"Always use high DPI emoticons",
				Constants.DEFAULT_FALSE,
				"Use high DPI emoticons even if no high DPI display is detected (requires restart)");
		entries[WEBVIEW] = new ConfigEntry(
				"ui.webview",
				ConfigType.BOOLEAN,
				"Enable WebView",
				Constants.DEFAULT_FALSE,
				"Use HTML Engine (for color/emoticons support) for message " +
				"content");
		entries[SHOW_AVATARS] = new ConfigEntry(
				"ui.show_avatars",
				ConfigType.BOOLEAN,
				"Show Avatars",
				Constants.DEFAULT_FALSE,
				"Show Avatars in expanded message headers");
		entries[SHOW_THREADS] = new ConfigEntry(
				"ui.show_threads",
				ConfigType.BOOLEAN,
				Constants.SHOW_THREADS_TEXT,
				Constants.DEFAULT_TRUE,
				"Show messages as threads in header pane");
		entries[SORT_BY_MOST_RECENT_IN_THREAD] = new ConfigEntry(
				"ui.sort_by_most_recent_in_thread",
				ConfigType.BOOLEAN,
				"Sort by most recent message in thread",
				Constants.DEFAULT_TRUE,
				"Sort messages by most recent message in thread when sorted " +
				"by date");
		entries[COLLAPSE_READ_THREADS] = new ConfigEntry(
				"ui.collapse_read_threads",
				ConfigType.BOOLEAN,
				"Collapse read threads",
				Constants.DEFAULT_TRUE,
				"Collapse threads that only contain read messages");
		entries[MONOSPACE_FONT] = new ConfigEntry(
				"ui.monospace_font",
				ConfigType.BOOLEAN,
				Constants.MONOSPACE_TEXT,
				Constants.DEFAULT_FALSE,
				"Render message content using monospace font");
		entries[EMOTICONS] = new ConfigEntry(
				"ui.emoticons",
				ConfigType.BOOLEAN,
				Constants.EMOTICON_TEXT,
				Constants.DEFAULT_FALSE,
				"Render emoticons as graphics in messages");
		entries[MUTE_QUOTES] = new ConfigEntry(
				"ui.mute_quotes",
				ConfigType.BOOLEAN,
				Constants.MUTE_QUOTES_TEXT,
				Constants.DEFAULT_FALSE,
				"Hide full quotes in messages");
		entries[SHOW_SIGNATURE] = new ConfigEntry(
				"ui.show_signature",
				ConfigType.BOOLEAN,
				Constants.SHOW_SIGNATURE_TEXT,
				Constants.DEFAULT_FALSE,
				"Show signature (if exists) at the end of each message");
		entries[DETECT_LINKS] = new ConfigEntry(
				"ui.detect_links",
				ConfigType.BOOLEAN,
				Constants.DETECT_LINKS_TEXT,
				Constants.DEFAULT_FALSE,
				"Detect links in messages");
		entries[SHOW_ATTACHMENTS] = new ConfigEntry(
				"ui.show_attachments",
				ConfigType.BOOLEAN,
				Constants.SHOW_ATTACHMENTS_TEXT,
				Constants.DEFAULT_FALSE,
				"Show attachments");
		entries[WINDOW_SIZE] = new ConfigEntry(
				"ui.window_size",
				ConfigType.STRING,
				"Size of main window",
				Constants.DEFAULT_WINDOW_SIZE,
				"Width x Height of main window on startup");
		entries[FOLDER_PANE_WIDTH] = new ConfigEntry(
				"ui.folder_pane_width",
				ConfigType.INT,
				"Folder pane width",
				Constants.DEFAULT_FOLDER_PANE_WIDTH,
				new IntRangeValidator(1, 99),
				"Relative width of folder pane (in percent)");
		entries[HEADER_PANE_HEIGHT] = new ConfigEntry(
				"ui.header_pane_height",
				ConfigType.INT,
				"Header pane height",
				Constants.DEFAULT_FOLDER_PANE_WIDTH,
				new IntRangeValidator(1, 99),
				"Relative heightof header pane (in percent)");

		// Virtual Folders
		entries[SHOW_SUBSCRIBED_ONLY] = new ConfigEntry(
				"ui.show_subscribed_only",
				ConfigType.BOOLEAN,
				"Show subscribed only",
				Constants.DEFAULT_FALSE,
				"Show only messages from subscribed boards in Recently Received folder");

		// Logging Settings
		entries[LOG_LEVEL] = new ConfigEntry(
				"log.level",
				ConfigType.CHOICE,
				"Log Level",
				Constants.DEFAULT_LOG_LEVEL,
				new ChoiceValidator(Arrays.asList("OFF", "WARNING", "INFO",
						"FINE", "FINER", "FINEST")),
				"Only log messages with specified level (or higher)");

		// configurable from Menu
		entries[OFFLINE] = new ConfigEntry(
				"fcp.offline",
				ConfigType.BOOLEAN,
				"Work offline",
				Constants.DEFAULT_FALSE,
				null);
	}

	public ConfigEntry getEntry(int id) {
		return entries[id];
	}

	public String getStringValue(int id) {
		return values[id];
	}

	public void setStringValue(int id, String value) {
		ConfigEntry entry = entries[id];
		String oldValue = values[id];

		if (!Objects.equals(value, oldValue)) {
			if (entry.validate(value)) {
				values[id] = value;
				update(id);
				dirty = true;
				LOG.log(Level.FINE,
						"Updated config value: {0} = {1}", new Object[]{
						entry.getName(), value});
			} else {
				LOG.log(Level.WARNING,
						"Invalid config value: {0} = {1}",
						new Object[]{entry.getName(), value});
			}
		}
	}

	public boolean getBooleanValue(int id) {
		return Boolean.parseBoolean(values[id]);
	}

	public int getIntValue(int id) {
		return Integer.parseInt(values[id]);
	}

	public InputStream getIconInputStream(String theme, String name,
			boolean allowHiDpi) {
		final String finalName;
		if (allowHiDpi && getForceHiDpiIcons()) {
			finalName = name.replaceFirst(".png$", "@2x.png");
		} else {
			finalName = name;
		}

		return getInputStream(theme, finalName);
	}

	public InputStream getIconInputStream(String theme, String name) {
		return getIconInputStream(theme, name, true);
	}

	public InputStream getEmoticonInputStream(String theme, String name) {
		final String finalName;
		if (getForceHiDpiEmoticons()) {
			finalName = name.replaceFirst(".png$", "@2x.png");
		} else {
			finalName = name;
		}

		return getInputStream(theme, finalName);
	}

	private InputStream getInputStream(String theme, String name) {
		final boolean isLocal = !defaultIconSets.contains(theme);
		final StringBuilder str = new StringBuilder();

		final char sep;
		if (isLocal) {
			sep = File.separatorChar;
		} else {
			sep = '/';
			str.append(sep);
		}

		str.append("icons");
		str.append(sep);
		str.append(theme);
		str.append(sep);
		str.append(name);

		final String path = str.toString();
		LOG.log(Level.FINEST, "Trying to load icon from {0}", path);

		InputStream is = null;
		if (isLocal) {
			try {
				is = new FileInputStream(path);
			} catch (IOException e) {
				LOG.log(Level.INFO, "Failed to load icon", e);
			}
		} else {
			is = Config.class.getResourceAsStream(path);
		}

		if (is == null) {
			LOG.log(Level.INFO, "Icon missing: {0}", path);
		}

		return is;
	}

	public String getFcpHost() {
		return values[FCP_HOST];
	}

	public int getFcpPort() {
		return intValues[FCP_PORT];
	}

	public String getMessageBase() {
		return values[MESSAGEBASE];
	}

	public int getDefaultId() {
		return intValues[DEFAULT_ID];
	}

	public boolean getFastMessageCheckEnabled() {
		return booleanValues[FAST_MESSAGE_CHECK];
	}

	public int getInactivityRetryInterval() {
		return intValues[INACTIVITY_RETRY_INTERVAL];
	}

	public int getInactivityTimeout() {
		return intValues[INACTIVITY_TIMEOUT];
	}

	public int getMaxIdentityAge() {
		return intValues[MAX_IDENTITY_AGE];
	}

	public int getMaxMessageAge() {
		return intValues[MAX_MESSAGE_AGE];
	}

	public int getMaxFcpRequests() {
		return intValues[MAX_FCP_REQUESTS];
	}

	public int getDownloadPriority() {
		char prio = values[DOWNLOAD_PRIORITY].charAt(0);
		return Character.digit(prio, 7);
	}

	public int getUploadPriority() {
		char prio = values[UPLOAD_PRIORITY].charAt(0);
		return Character.digit(prio, 7);
	}

	public int getMinMessageDelay() {
		return intValues[MIN_MESSAGE_DELAY];
	}

	public int getMaxMessageDelay() {
		return intValues[MAX_MESSAGE_DELAY];
	}

	public int getMinLocalTrustListTrust() {
		return intValues[MIN_LOCAL_TRUSTLIST_TRUST];
	}

	public int getMinLocalMessageTrust() {
		return intValues[MIN_LOCAL_MESSAGE_TRUST];
	}

	public int getMinPeerTrustListTrust() {
		return intValues[MIN_PEER_TRUSTLIST_TRUST];
	}

	public int getMinPeerMessageTrust() {
		return intValues[MIN_PEER_MESSAGE_TRUST];
	}

	public boolean getDownloadMsgListsWithoutTrust() {
		return booleanValues[DL_MSGLISTS_WITHOUT_TRUST];
	}

	public boolean getDownloadMessagesWithoutTrust() {
		return booleanValues[DL_MESSAGES_WITHOUT_TRUST];
	}

	public boolean getExcludeNullTrust() {
		return booleanValues[EXCLUDE_NULL_TRUST];
	}

	public int getIndirectTrustWeight() {
		return intValues[INDIRECT_TRUST_WEIGHT];
	}

	public String getIconSet() {
		return values[ICON_SET];
	}

	public boolean getWebViewEnabled() {
		return booleanValues[WEBVIEW];
	}

	public boolean getShowAvatars() {
		return booleanValues[SHOW_AVATARS];
	}

	public boolean getForceHiDpiIcons() {
		return booleanValues[FORCE_HIDPI_ICONS];
	}

	public boolean getForceHiDpiEmoticons() {
		return booleanValues[FORCE_HIDPI_EMOTICONS];
	}

	public boolean getShowThreads() {
		return booleanValues[SHOW_THREADS];
	}

	public boolean getSortByMostRecentInThread() {
		return booleanValues[SORT_BY_MOST_RECENT_IN_THREAD];
	}

	public boolean getCollapseReadThreads() {
		return booleanValues[COLLAPSE_READ_THREADS];
	}

	public boolean getUseMonospaceFont() {
		return booleanValues[MONOSPACE_FONT];
	}

	public boolean getShowEmoticons() {
		return booleanValues[EMOTICONS];
	}

	public boolean getMuteQuotes() {
		return booleanValues[MUTE_QUOTES];
	}

	public boolean getShowSignature() {
		return booleanValues[SHOW_SIGNATURE];
	}

	public boolean getDetectLinks() {
		return booleanValues[DETECT_LINKS];
	}

	public boolean getShowAttachments() {
		return booleanValues[SHOW_ATTACHMENTS];
	}

	public WindowInfo getWindowInfo() {
		return new WindowInfo(values[WINDOW_SIZE]);
	}

	public int getFolderPaneWidth() {
		return intValues[FOLDER_PANE_WIDTH];
	}

	public int getHeaderPaneHeight() {
		return intValues[HEADER_PANE_HEIGHT];
	}

	public boolean getShowSubscribedOnly() {
		return booleanValues[SHOW_SUBSCRIBED_ONLY];
	}

	public boolean getOffline() {
		return booleanValues[OFFLINE];
	}

	public boolean loadFromFile(String filename) {
		try (FileInputStream in = new FileInputStream(filename)) {
			Properties properties = new Properties();
			properties.load(in);

			for (int id=0; id<ID_SIZE; id++) {
				ConfigEntry entry = entries[id];
				String value = properties.getProperty(entry.getPropertyName());
				if (value != null) {
					values[id] = value;
				}
			}

			updateRootLogger();
			LOG.log(Level.FINEST, "loaded configuration from {0}", filename);
			sanitizeConfig();

			return true;
		} catch (FileNotFoundException e) {
			LOG.log(Level.INFO, "configuration file {0} not found", filename);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "failed to open configuration file", e);
		}

		return false;
	}

	public void saveToFile(String filename, boolean force) {
		if (!dirty && !force) {
			return;
		}

		sanitizeConfig();

		Properties properties = new Properties();
		for (int id=0; id<ID_SIZE; id++) {
			if (values[id] != null) {
				properties.put(entries[id].getPropertyName(), values[id]);
			}
		}

		try (FileOutputStream out = new FileOutputStream(filename)) {
			properties.store(out, "JFMS Configuration File");
			dirty = false;
			LOG.log(Level.FINEST, "wrote config to {0}", filename);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "failed to write config", e);
		}
	}

	public void saveToFile(String filename) {
		saveToFile(filename, false);
	}

	private void update(int id) {
		final ConfigType type = entries[id].getType();
		if (type.equals(ConfigType.BOOLEAN)) {
			booleanValues[id] = getBooleanValue(id);
		} else if (type.equals(ConfigType.INT)) {
			intValues[id] = getIntValue(id);
		}

		if (id == LOG_LEVEL) {
			updateRootLogger();
		}
	}

	private void sanitizeConfig() {
		for (int id=0; id<ID_SIZE; id++) {
			final ConfigEntry entry = entries[id];

			final String value = values[id];
			if (value != null) {
				if (!entry.validate(value)) {
					LOG.log(Level.FINE, "Invalid value for {0}: {1}, "
					+ "falling back to default", new Object[]{
					entry.getName(), value});
					values[id] = entry.getDefaultValue();
				}
			} else {
				LOG.log(Level.FINEST, "Adding default value for {0}",
						entry.getName());
				values[id] = entry.getDefaultValue();
			}

			update(id);
		}
	}

	private Level getLogLevel(String value) {
		if (value == null) {
			value = Constants.DEFAULT_LOG_LEVEL;
		}

		Level logLevel;
		switch (value) {
		case "OFF":
			logLevel = Level.OFF;
			break;
		case "WARNING":
			logLevel = Level.WARNING;
			break;
		case "INFO":
			logLevel = Level.INFO;
			break;
		case "FINE":
			logLevel = Level.FINE;
			break;
		case "FINER":
			logLevel = Level.FINER;
			break;
		case "FINEST":
			logLevel = Level.FINEST;
			break;
		default:
			logLevel = getLogLevel(null);
			break;
		}

		return logLevel;
	}

	private void updateRootLogger() {
		Level logLevel = getLogLevel(values[LOG_LEVEL]);
		final Logger rootLogger = Logger.getLogger("");
		if (!logLevel.equals(rootLogger.getLevel())) {
			rootLogger.setLevel(logLevel);
		}
	}
}
