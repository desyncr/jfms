package jfms.ui;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import jfms.config.Config;
import jfms.config.Constants;
import jfms.config.WindowInfo;
import jfms.fms.FmsManager;
import jfms.store.MessageSearchCriteria;
import jfms.store.Store;

public final class JfmsApplication extends Application {
	private static final Logger LOG = Logger.getLogger(JfmsApplication.class.getName());
	public static final String JFMS_VERSION_KEY = "jfms_version";
	public static final String JFMS_VERSION_PROPERTY= "jfms.version";

	private final Properties properties = new Properties();
	private FmsManager fmsManager;
	private Stage primaryStage;
	private NewsPane newsPane;
	private int width;
	private int height;
	private BoardWindow boardWindow;
	private IdentityWindow identityWindow;
	private LocalIdentityPane localIdentityPane;
	private MaintenanceWindow maintenanceWindow;

	@Override
	public void start(Stage stage) {
		this.primaryStage = stage;

		LOG.log(Level.FINEST, "Starting UI");

		loadProperties();
		final String version = properties.getProperty(JFMS_VERSION_PROPERTY);
		LOG.log(Level.INFO, "jfms version is {0}", version);

		try {
			final Config config = Config.getInstance();
			fmsManager = FmsManager.getInstance();
			boolean configExists =
				config.loadFromFile(Constants.JFMS_CONFIG_PATH);

			boolean dbExists = Store.databaseExists(Constants.DATABASE_FILE);

			// pre-initialize store to detect DB errors
			fmsManager.initializeStore();
			List<String> seedIdentities = null;

			if (!configExists || !dbExists) {
				LOG.log(Level.FINEST, "Running wizard...");
				// jfms.properties or DB do not exist
				// assume first run and start wizard
				ConfigWizard wizard = new ConfigWizard(!dbExists);
				wizard.run();
				if (wizard.getSettings().getFatalError()) {
					LOG.log(Level.WARNING, "Fatal error in wizard, exiting");
					return;
				}
				seedIdentities = wizard.getSettings().getSeedIdentities();

				config.saveToFile(Constants.JFMS_CONFIG_PATH, true);
			}

			final Store store  = fmsManager.getStore();
			store.initialize(seedIdentities);

			if (version != null) {
				String lastVersion = store.getValue(JFMS_VERSION_KEY);
				if (lastVersion == null) {
					lastVersion = "not set";
				}

				if (!version.equals(lastVersion)) {
					LOG.log(Level.INFO, "FMS version changed from {0} to {1}",
							new Object[]{lastVersion, version});
					store.saveValue(JFMS_VERSION_KEY, version);
				}
			}

			fmsManager.initialize();

			boardWindow = new BoardWindow();
			identityWindow = new IdentityWindow();
			localIdentityPane = new LocalIdentityPane();
			maintenanceWindow = new MaintenanceWindow();

			StatusBar statusBar = new StatusBar();

			BorderPane borderPane = new BorderPane();
			borderPane.setCenter(addCenterPane());
			borderPane.setTop(addMenuBar());
			borderPane.setBottom(statusBar.getNode());


			final WindowInfo winInfo = config.getWindowInfo();
			Scene scene = StyleFactory.getInstance().createScene(borderPane,
					winInfo.getWidth(), winInfo.getHeight());
			if (winInfo.isMaximized()) {
				stage.setMaximized(true);
			}

			scene.widthProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number  newValue) {
					if (!stage.isMaximized()) {
						width = newValue.intValue();
					}
				}
			});
			scene.heightProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number  newValue) {
					if (!stage.isMaximized()) {
						height = newValue.intValue();
					}
				}
			});

			stage.setTitle("jfms");
			stage.setScene(scene);
			stage.getIcons().addAll(Icons.getInstance().getAppIcons());

			stage.show();




			fmsManager.setFcpStatusListener(statusBar);
			fmsManager.setProgressListener(statusBar);

			fmsManager.run();
		} catch (SQLException e) {
			LOG.log(Level.SEVERE, "Initialization failed", e);

			Dialog<ButtonType> dialog = StyleFactory.getInstance()
					.createAlert(Alert.AlertType.ERROR);
			dialog.setTitle("jfms Initialization Failed");
			dialog.setHeaderText("Failed to inititalize SQLite Database");
			dialog.setContentText("Please check that you have a working SQLite "
					+ "JDBC driver\nin your classpath.\n"
					+ Constants.SEE_LOGS_TEXT);
			dialog.showAndWait();
		}
	}

	@Override
	public void stop() throws Exception {
		LOG.log(Level.FINEST, "stop called");
		super.stop();

		newsPane.saveConfig(new WindowInfo(width, height,
					primaryStage.isMaximized()));
		fmsManager.shutdown();
	}

	private Node addMenuBar() {
		//--------------------
		// File
		//--------------------
		CheckMenuItem workOffline = new CheckMenuItem("Work Offline");
		workOffline.setSelected(fmsManager.isOffline());
		workOffline.setOnAction((ActionEvent e) ->
				fmsManager.setOffline(workOffline.isSelected()));

		MenuItem quit = new MenuItem("Quit");
		quit.setOnAction((ActionEvent e) -> Platform.exit());

		Menu fileMenu = new Menu("File", null, workOffline, quit);


		//--------------------
		// View
		//--------------------
		CheckMenuItem threadedMenuItem =
			new CheckMenuItem(Constants.SHOW_THREADS_TEXT);
		threadedMenuItem.setOnAction((ActionEvent e) ->
				newsPane.setThreadedView(threadedMenuItem.isSelected(), true));

		CheckMenuItem threadSortMenuItem =
			new CheckMenuItem("Sort by most recent message in thread");
		threadSortMenuItem.setOnAction((ActionEvent e) ->
				newsPane.setThreadSort(threadSortMenuItem.isSelected()));

		CheckMenuItem collapseReadThreadsMenuItem =
			new CheckMenuItem("Collapse read threads");
		collapseReadThreadsMenuItem.setOnAction((ActionEvent e) ->
				newsPane.setCollapseReadThreads(
					collapseReadThreadsMenuItem.isSelected()));

		MenuItem expandAllThreadsMenuItem =
			new MenuItem("Expand all threads");
		expandAllThreadsMenuItem.setOnAction((ActionEvent e) ->
				newsPane.expandAllThreads(true));

		MenuItem collapseAllThreadsMenuItem =
			new MenuItem("Collapse all threads");
		collapseAllThreadsMenuItem.setOnAction((ActionEvent e) ->
				newsPane.expandAllThreads(false));

		CheckMenuItem monospaceMenuItem =
			new CheckMenuItem(Constants.MONOSPACE_TEXT);
		monospaceMenuItem.setOnAction((ActionEvent e) -> newsPane
				.setUseMonospaceFont(monospaceMenuItem.isSelected(), true));

		CheckMenuItem showEmoticonsMenuItem =
			new CheckMenuItem(Constants.EMOTICON_TEXT);
		showEmoticonsMenuItem.setOnAction((ActionEvent e) -> newsPane
				.setShowEmoticons(showEmoticonsMenuItem.isSelected(), true));

		CheckMenuItem muteQuotesMenuItem =
			new CheckMenuItem(Constants.MUTE_QUOTES_TEXT);
		muteQuotesMenuItem.setOnAction((ActionEvent e) -> newsPane
				.setMuteQuotes(muteQuotesMenuItem.isSelected(), true));

		CheckMenuItem showSignatureMenuItem =
			new CheckMenuItem(Constants.SHOW_SIGNATURE_TEXT);
		showSignatureMenuItem.setOnAction((ActionEvent e) -> newsPane
				.setShowSignature(showSignatureMenuItem.isSelected(), true));

		CheckMenuItem detectLinksMenuItem =
			new CheckMenuItem(Constants.DETECT_LINKS_TEXT);
		detectLinksMenuItem.setOnAction((ActionEvent e) -> newsPane
				.setDetectLinks(detectLinksMenuItem.isSelected(), true));

		CheckMenuItem showAttachmentsMenuItem =
			new CheckMenuItem(Constants.SHOW_ATTACHMENTS_TEXT);
		showAttachmentsMenuItem.setOnAction((ActionEvent e) -> newsPane
				.setShowAttachments(showAttachmentsMenuItem.isSelected(), true));

		Menu threadsMenu = new Menu("Threads", null,
				threadedMenuItem,
				threadSortMenuItem,
				collapseReadThreadsMenuItem,
				new SeparatorMenuItem(),
				expandAllThreadsMenuItem,
				collapseAllThreadsMenuItem);
		threadsMenu.setOnShowing(e -> {
			threadedMenuItem.setSelected(newsPane.getThreadedView());
			threadSortMenuItem.setSelected(newsPane.getThreadSort());
			collapseReadThreadsMenuItem.setSelected(newsPane.getCollapseReadThreads());
		});

		Menu viewMenu = new Menu("View", null,
				threadsMenu,
				new SeparatorMenuItem(),
				monospaceMenuItem, showEmoticonsMenuItem, muteQuotesMenuItem,
				showSignatureMenuItem, detectLinksMenuItem,
				showAttachmentsMenuItem);
		viewMenu.setOnShowing(e -> {
			monospaceMenuItem.setSelected(newsPane.getUseMonospaceFont());
			showEmoticonsMenuItem.setSelected(newsPane.getShowEmoticons());
			muteQuotesMenuItem.setSelected(newsPane.getMuteQuotes());
			showSignatureMenuItem.setSelected(newsPane.getShowSignature());
			detectLinksMenuItem.setSelected(newsPane.getDetectLinks());
			showAttachmentsMenuItem.setSelected(newsPane.getShowAttachments());
		});


		//--------------------
		// Go
		//--------------------
		MenuItem nextMessage = new MenuItem("Next Message");
		nextMessage.setOnAction((ActionEvent e) -> newsPane.gotoNextMessage());
		nextMessage.setAccelerator(KeyCombination.keyCombination("f"));

		MenuItem nextUnreadMessage = new MenuItem("Next Unread Message");
		nextUnreadMessage.setOnAction((ActionEvent e) -> newsPane.gotoNextUnreadMessage());
		nextUnreadMessage.setAccelerator(KeyCombination.keyCombination("n"));

		MenuItem previousMessage = new MenuItem("Previous Message");
		previousMessage.setOnAction((ActionEvent e) -> newsPane.gotoPreviousMessage());
		previousMessage.setAccelerator(KeyCombination.keyCombination("b"));

		MenuItem previousUnreadMessage = new MenuItem("Previous Unread Message");
		previousUnreadMessage.setOnAction((ActionEvent e) -> newsPane.gotoPreviousUnreadMessage());
		previousUnreadMessage.setAccelerator(KeyCombination.keyCombination("p"));

		Menu goMenu = new Menu("Go", null,
				nextMessage, nextUnreadMessage,
				previousMessage, previousUnreadMessage);


		//--------------------
		// Message
		//--------------------
		MenuItem reply = newsPane.createMessageReplyMenuItem();
		MenuItem delete = newsPane.createMessageDeleteMenuItem();
		MenuItem markUnread = newsPane.createMessageMarkUnreadMenuItem();
		MenuItem markThreadRead = newsPane.createMessageMarkThreadAsReadMenuItem();
		MenuItem idTrust = newsPane.createMessageTrustMenuItem();
		MenuItem idDetails = newsPane.createMessageIdentityDetailsMenuItem();
		MenuItem findUserMessages = newsPane.createFindUserMessagesMenuItem();
		MenuItem subscribe = newsPane.createSubscribeMenuItem();
		MenuItem copyURI = newsPane.createMessageCopyUriMenuItem();

		final Menu messageMenu = new Menu("Message", null,
				newsPane.createMessageNewMenuItem(),
				reply,
				delete,
				new SeparatorMenuItem(),
				markUnread,
				markThreadRead,
				new SeparatorMenuItem(),
				idTrust,
				idDetails,
				findUserMessages,
				new SeparatorMenuItem(),
				subscribe,
				copyURI
		);
		List<MenuItem> requireMessageItems = Arrays.asList(
			reply, delete, markUnread, markThreadRead,
			idTrust, idDetails, findUserMessages, copyURI
		);
		messageMenu.setOnShowing(e -> {
				for (MenuItem m : requireMessageItems) {
					m.setDisable(newsPane.isReplyDisabled());
				}
				if (delete.isDisable()) {
					delete.setDisable(!newsPane.isDeletable());
				}
				subscribe.setDisable(newsPane.isSubscribed());
		});


		//--------------------
		// Boards
		//--------------------
		final MenuItem removeBoard = newsPane.createRemoveBoardMenuItem();
		final MenuItem markRead = newsPane.createMarkBoardReadMenuItem();

		MenuItem findMessages = new MenuItem("Find Messages...");
		findMessages.setOnAction(e -> showFindDialog());

		MenuItem boardList = new MenuItem("Board List...");
		boardList.setOnAction(e -> boardWindow.show(primaryStage, newsPane));

		Menu boardsMenu = new Menu("Boards", null,
				markRead,
				newsPane.createMarkAllMessagesReadMenuItem(),
				newsPane.createAddBoardMenuItem(),
				removeBoard,
				new SeparatorMenuItem(),
				findMessages,
				boardList);
		boardsMenu.setOnShowing(e -> {
			boolean disabled = newsPane.getCurrentBoard() == null;
			markRead.setDisable(disabled);
			removeBoard.setDisable(disabled);
		});

		//--------------------
		// Settings
		//--------------------
		MenuItem manageLocalIds = new MenuItem("Manage Local Identities...");
		manageLocalIds.setOnAction(e -> localIdentityPane.show(primaryStage));

		MenuItem manageIds = new MenuItem("Manage Identities...");
		manageIds.setOnAction(e -> identityWindow.show(primaryStage));

		MenuItem maintenance = new MenuItem("Database Maintenance...");
		maintenance.setOnAction(e -> maintenanceWindow.show(primaryStage));

		MenuItem settings = new MenuItem("Settings...");
		settings.setOnAction((ActionEvent e) -> {
			SettingsDialog settingsDialog = new SettingsDialog(primaryStage);
			settingsDialog.showAndWait();
		});

		Menu identitiesMenu = new Menu("Options", null,
				manageLocalIds, manageIds, maintenance,
				new SeparatorMenuItem(),
				settings);

		//--------------------
		// Help
		//--------------------
		MenuItem about = new MenuItem("About jfms");
		about.setOnAction(e -> showAboutDialog());

		Menu helpMenu = new Menu("Help", null, about);


		return new MenuBar(fileMenu, viewMenu, goMenu,
				messageMenu, boardsMenu, identitiesMenu, helpMenu);
	}

	private Node addToolBar() {
		Button identitiesButton = new Button(null,
				new ImageView(Icons.getInstance().getIdentitiesIcon()));
		identitiesButton.setTooltip(new Tooltip("Manage Identities"));
		Utils.setToolBarButtonStyle(identitiesButton);
		identitiesButton.setOnAction(e -> identityWindow.show(primaryStage));

		Button boardsButton = new Button(null,
				new ImageView(Icons.getInstance().getBoardsIcon()));
		boardsButton.setTooltip(new Tooltip("Board List"));
		Utils.setToolBarButtonStyle(boardsButton);
		boardsButton.setOnAction(e -> boardWindow.show(primaryStage, newsPane));

		Button findButton = new Button(null,
				new ImageView(Icons.getInstance().getFindIcon()));
		findButton.setTooltip(new Tooltip("Find messages"));
		Utils.setToolBarButtonStyle(findButton);
		findButton.setOnAction(e -> showFindDialog());

		Button settingsButton = new Button(null,
				new ImageView(Icons.getInstance().getSettingsIcon()));
		settingsButton.setTooltip(new Tooltip("Settings"));
		Utils.setToolBarButtonStyle(settingsButton);
		settingsButton.setOnAction((ActionEvent e) -> {
			SettingsDialog settingsDialog = new SettingsDialog(primaryStage);
			settingsDialog.showAndWait();
		});

		return new ToolBar(identitiesButton, boardsButton,
				findButton, settingsButton);
	}

	private Node addCenterPane() {
		VBox vbox = new VBox();
		newsPane = new NewsPane(primaryStage);
		vbox.getChildren().addAll(addToolBar(), newsPane.getNode());
		VBox.setVgrow(newsPane.getNode(), Priority.ALWAYS);

		return vbox;
	}

	private void loadProperties() {
		try (InputStream is = JfmsApplication.class.getResourceAsStream(Constants.PROPERTIES_PATH)) {
			properties.load(is);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "failed to open properties file", e);
		}
	}

	private void showFindDialog() {
		FindDialog findDialog = new FindDialog(primaryStage);
		MessageSearchCriteria msc = findDialog.showAndWait();
		if (msc != null) {
			newsPane.createSearchResultsFolder(msc);
		}
	}

	private void showAboutDialog() {
		final Image pdIcon = new Image(getClass()
				.getResourceAsStream("/icons/pd-icon.png"));
		Dialog<ButtonType> aboutDialog = StyleFactory.getInstance()
				.createAlert(Alert.AlertType.INFORMATION);
		aboutDialog.setTitle("About jfms");
		aboutDialog.setGraphic(new ImageView(pdIcon));
		aboutDialog.setHeaderText("Version "
				+ properties.getProperty(JFMS_VERSION_PROPERTY, ""));

		StringBuilder str = new StringBuilder();
		str.append("This is free and unencumbered software released into the public domain.");
		str.append("\n\n");
		str.append("Full licenses are included in the JAR file.");
		str.append("\n\n");
		str.append("Java Version: ");
		str.append(System.getProperty("java.version"));
		str.append("\nJava VM Vendor: ");
		str.append(System.getProperty("java.vm.vendor"));
		str.append("\nJava VM Name: ");
		str.append(System.getProperty("java.vm.name"));
		str.append("\n\nJDBC driver: ");
		str.append(fmsManager.getStore().getInfo());
		str.append('\n');
		str.append(properties.getProperty("jdbc.license", ""));
		str.append("\n\n");
		str.append(properties.getProperty("icons.license", ""));

		TextArea info = new TextArea(str.toString());
		info.setEditable(false);
		aboutDialog.getDialogPane().setContent(info);
		aboutDialog.showAndWait();
	}
}
