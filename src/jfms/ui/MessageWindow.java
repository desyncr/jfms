package jfms.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import jfms.config.Config;
import jfms.config.Constants;
import jfms.fms.FmsManager;
import jfms.fms.IdentityManager;
import jfms.fms.InReplyTo;
import jfms.fms.LocalIdentity;
import jfms.fms.MessageReference;
import jfms.store.InsertStatus;
import jfms.store.Store;
import jfms.util.UUID;

public class MessageWindow {
	private static final Logger LOG = Logger.getLogger(MessageWindow.class.getName());
	private static final String BOLD_STYLE = "-fx-font-weight: bold;";

	private final Stage stage = new Stage();
	private final ComboBox<String> fromCb = new ComboBox<>();
	private final ToggleButton monospaceButton = new ToggleButton();
	private final TextField subjectTextField = new TextField();
	private final TextField boardsTextField = new TextField();
	private final TextArea messageContent;
	private final InReplyTo inReplyTo;
	private Map<Integer, LocalIdentity> localIds;
	private final MessageReference draftRef;
	private final Random random = new Random();

	public MessageWindow(String boardName) {
		boardsTextField.setText(boardName);
		setFromList();
		messageContent = createMessageContent(null);
		inReplyTo = null;
		draftRef = null;
	}

	public MessageWindow(jfms.fms.Message parentMessage, boolean isDraft) {
		setFromList();
		String subject = parentMessage.getSubject();

		if (isDraft) {
			draftRef = new MessageReference();
			draftRef.setIdentityId(parentMessage.getIdentityId());
			draftRef.setDate(parentMessage.getInsertDate());
			draftRef.setIndex(parentMessage.getInsertIndex());
		} else {
			draftRef = null;
		}

		if (isDraft) {
			int identityId = parentMessage.getIdentityId();
			final LocalIdentity id = localIds.get(identityId);
			if (id != null) {
				fromCb.setValue(id.getFullName());
			}
		} else if (!subject.startsWith("Re: ")) {
			subject = "Re: " + subject;
		}
		subjectTextField.setText(subject);

		StringBuilder boards = new StringBuilder();
		boards.append(parentMessage.getReplyBoard());
		for (String board : parentMessage.getBoards()) {
			if (!board.equals(parentMessage.getReplyBoard())) {
				boards.append(", ");
				boards.append(board);
			}
		}
		boardsTextField.setText(boards.toString());

		if (isDraft) {
			messageContent = createMessageContent(null);
			messageContent.setText(parentMessage.getBody());
			inReplyTo = parentMessage.getInReplyTo();
		} else {
			messageContent = createMessageContent(parentMessage);
			inReplyTo = parentMessage.getInReplyTo().increment();
			inReplyTo.add(0, parentMessage.getMessageUuid());
		}
	}

	public void show(Window ownerWindow) {
		VBox topVBox = new VBox();
		topVBox.getChildren().addAll(createMenuBar(), createToolbar(),
				createHeaders());


		BorderPane borderPane = new BorderPane();
		borderPane.setTop(topVBox);
		borderPane.setCenter(messageContent);
		Scene scene = StyleFactory.getInstance().createScene(borderPane);

		stage.initOwner(ownerWindow);
		stage.setTitle("Create message");
		stage.setScene(scene);
		stage.setOnCloseRequest(e -> {
			if (!confirmCloseRequest()) {
				e.consume();
			}
		});
		stage.show();

		messageContent.requestFocus();
	}

	private Node createMenuBar() {
		MenuItem sendMenuItem = new MenuItem("Send");
		sendMenuItem.setOnAction(e -> sendMessage(InsertStatus.NOT_INSERTED));

		MenuItem saveDraftItem = new MenuItem("Save Draft");
		saveDraftItem.setOnAction(e -> sendMessage(InsertStatus.DRAFT));

		MenuItem closeMenuItem = new MenuItem("Close");
		closeMenuItem.setOnAction(e -> {
			if (confirmCloseRequest()) {
				stage.hide();
			}
		});

		Menu messageMenu = new Menu("Message", null,
				sendMenuItem, saveDraftItem,
				new SeparatorMenuItem(),
				closeMenuItem);

		CheckMenuItem monospaceMenuItem =
			new CheckMenuItem(Constants.MONOSPACE_TEXT);
		Menu viewMenu = new Menu("View", null,
				monospaceMenuItem);
		viewMenu.setOnAction(e ->
				setUseMonospaceFont(monospaceMenuItem.isSelected(), true));
		viewMenu.setOnShowing(e ->
				monospaceMenuItem.setSelected(monospaceButton.isSelected()));

		return new MenuBar(messageMenu, viewMenu);
	}

	private void setUseMonospaceFont(boolean enable, boolean updateButton) {
		if (updateButton) {
			monospaceButton.setSelected(enable);
			Utils.refreshToolBarButtonStyle(monospaceButton);
		}

		if (enable) {
			messageContent.setStyle("-fx-font-family: Monospace");
		} else {
			messageContent.setStyle(null);
		}
	}

	private boolean confirmCloseRequest() {
		Alert alert = StyleFactory.getInstance().createAlert(
				Alert.AlertType.CONFIRMATION,
				"Message has not been sent. Discard message?");
		alert.setHeaderText("Discard message?");

		Optional<ButtonType> result = alert.showAndWait();
		return result.isPresent() && result.get() == ButtonType.OK;
	}

	private Node createToolbar() {
		Button sendMessageButton = new Button(null,
				new ImageView(Icons.getInstance().getSendMessageIcon()));
		sendMessageButton.setTooltip(new Tooltip("Send Message"));
		Utils.setToolBarButtonStyle(sendMessageButton);
		sendMessageButton.setOnAction(e -> sendMessage(InsertStatus.NOT_INSERTED));

		Button saveDraftButton = new Button(null,
				new ImageView(Icons.getInstance().getSaveDraftIcon()));
		saveDraftButton.setTooltip(new Tooltip("Save Draft"));
		Utils.setToolBarButtonStyle(saveDraftButton);
		saveDraftButton.setOnAction(e -> sendMessage(InsertStatus.DRAFT));

		monospaceButton.setGraphic(
				new ImageView(Icons.getInstance().getFontIcon()));
		monospaceButton.setOnAction(e ->
				setUseMonospaceFont(monospaceButton.isSelected(), false));
		monospaceButton.setTooltip(new Tooltip(Constants.MONOSPACE_TEXT));
		Utils.setToolBarButtonStyle(monospaceButton);

		return new ToolBar(sendMessageButton,
				saveDraftButton,
				new Separator(Orientation.VERTICAL),
				monospaceButton);
	}

	private Node createHeaders() {
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));

		Label fromLabel = new Label("From:");
		Label subjectLabel = new Label("Subject:");
		Label boardsLabel = new Label("Boards:");

		fromLabel.setStyle(BOLD_STYLE);
		subjectLabel.setStyle(BOLD_STYLE);
		boardsLabel.setStyle(BOLD_STYLE);

		grid.add(fromLabel, 0, 0);
		grid.add(fromCb, 1, 0);
		grid.add(subjectLabel, 0, 1);
		grid.add(subjectTextField, 1, 1);
		grid.add(boardsLabel, 0, 2);
		grid.add(boardsTextField, 1, 2);

		// second column gets any extra width
		ColumnConstraints col1 = new ColumnConstraints();
		ColumnConstraints col2 = new ColumnConstraints();
		col2.setHgrow(Priority.ALWAYS);
		grid.getColumnConstraints().addAll(col1, col2);
		boardsTextField.setTooltip(new Tooltip(
				"First board is the reply board\n" +
				"Additonal boards must be separated by comma"));

		return grid;
	}

	private void showSendMessageFailedDialog(String errorText) {
		Alert alert = StyleFactory.getInstance().createAlert(
				Alert.AlertType.WARNING, errorText);
		alert.setHeaderText("Failed to send Message");
		alert.showAndWait();
	}

	private LocalDateTime calculateMessageTime() {
		LocalDateTime dateTime = LocalDateTime.now(ZoneOffset.UTC)
			.truncatedTo(ChronoUnit.SECONDS);

		final Config config = Config.getInstance();
		int minMessageDelay = config.getMinMessageDelay();
		int maxMessageDelay = config.getMaxMessageDelay();
		if (minMessageDelay > maxMessageDelay) {
			// currently not validated via config
			LOG.log(Level.WARNING, "minMessageDelay > maxMessageDelay");
			minMessageDelay = maxMessageDelay;
		}

		if (maxMessageDelay > 0) {
			int range = maxMessageDelay - minMessageDelay + 1;
			int delay = minMessageDelay + random.nextInt(range);
			dateTime = dateTime.plusMinutes(delay);
		}

		return dateTime;
	}

	private void sendMessage(InsertStatus insertStatus) {
		final String subjectText = subjectTextField.getText();
		if (subjectText == null || subjectText.isEmpty()) {
			showSendMessageFailedDialog("subject missing");
			return;
		}

		final String boardsText = boardsTextField.getText();
		if (boardsText == null || boardsText.isEmpty()) {
			showSendMessageFailedDialog("boards missing");
			return;
		}

		final List<String> boards = getBoards();
		if (boards.isEmpty()) {
			showSendMessageFailedDialog("boards invalid");
			return;
		}



		int localIdentityId = 0;
		String ssk = null;
		for (Map.Entry<Integer, LocalIdentity> e : localIds.entrySet()) {
			final LocalIdentity id = e.getValue();
			if (id.getFullName().equals(fromCb.getValue())) {
				localIdentityId = e.getKey();
				ssk = id.getSsk();
			}
		}

		if (ssk == null) {
			showSendMessageFailedDialog("No known identity selected");
			LOG.log(Level.WARNING, "Failed to get ID for {0}",
					fromCb.getValue());
			return;
		}

		final String uuid = UUID.randomUUID(ssk);
		final LocalDateTime dateTime = calculateMessageTime();
		final LocalDate date = dateTime.toLocalDate();

		jfms.fms.Message msg = new jfms.fms.Message();
		msg.setDate(date);
		msg.setTime(dateTime.toLocalTime());
		msg.setSubject(subjectText);
		msg.setMessageUuid(uuid);
		msg.setReplyBoard(boards.get(0));
		msg.setBody(messageContent.getText());

		msg.setBoards(boards);
		if (inReplyTo != null) {
			msg.setInReplyTo(inReplyTo);
		}

		jfms.fms.xml.MessageWriter writer =
			new jfms.fms.xml.MessageWriter();
		final String messageXml = writer.writeXml(msg);
		if (messageXml == null) {
			LOG.log(Level.WARNING, "Failed to create XML from message");
			showSendMessageFailedDialog(Constants.SEE_LOGS_TEXT);
			return;
		}


		Store store = FmsManager.getInstance().getStore();
		int index = store.saveLocalMessage(localIdentityId, messageXml, date,
				draftRef, insertStatus);
		if (index == -1) {
			LOG.log(Level.WARNING, "Failed to store message");
			showSendMessageFailedDialog(Constants.SEE_LOGS_TEXT);
			return;
		}

		MessageReference msgRef = new MessageReference();
		msgRef.setIdentityId(localIdentityId);
		msgRef.setDate(date);
		msgRef.setIndex(index);

		final InsertStatus previousStatus;
		if (draftRef == null) {
			previousStatus = InsertStatus.IGNORE;
		} else {
			previousStatus = InsertStatus.DRAFT;
		}

		FmsManager.getInstance().getMessageManager()
			.addLocalMessage(msgRef, insertStatus, previousStatus);

		stage.hide();
	}

	private TextArea createMessageContent(jfms.fms.Message parentMessage) {
		TextArea textArea = new TextArea();
		textArea.setPrefColumnCount(80);
		textArea.setPrefRowCount(25);
		textArea.setWrapText(true);

		if (parentMessage != null) {
			IdentityManager identityManager = FmsManager.getInstance().getIdentityManager();
			jfms.fms.Identity id = identityManager.getIdentity(parentMessage.getIdentityId());
			String name = id.getFullName();

			try (BufferedReader reader = new BufferedReader(
						new StringReader(parentMessage.getBody()))) {

				StringBuilder newMessage = new StringBuilder(name);
				newMessage.append(" wrote:\n");
				String line;
				while ((line = reader.readLine()) != null) {
					if (!line.isEmpty() && line.charAt(0) == '>') {
						newMessage.append('>');
					} else {
						newMessage.append("> ");
					}
					newMessage.append(line);
					newMessage.append('\n');
				}
				textArea.setText(newMessage.toString());
				textArea.end();
			} catch (IOException e) {
				LOG.log(Level.WARNING, "failed to create message", e);
			}
		}


		return textArea;
	}

	private void setFromList() {
		final Store store = FmsManager.getInstance().getStore();
		final int defaultId = Config.getInstance().getDefaultId();
		localIds = store.retrieveLocalIdentities();

		ObservableList<String> fromList = FXCollections.observableArrayList();
		for (Map.Entry<Integer, LocalIdentity> e : localIds.entrySet()) {
			final LocalIdentity id = e.getValue();
			if (defaultId == e.getKey()) {
				fromCb.setValue(id.getFullName());
			}
			fromList.add(id.getFullName());
		}

		fromCb.setItems(fromList);
	}

	private List<String> getBoards() {
		String[] boards = boardsTextField.getText().split(",");
		List<String> checkedBoards = new ArrayList<>();

		for (String board : boards) {
			String trimmed = board.trim();
			if (!trimmed.isEmpty()) {
				checkedBoards.add(trimmed);
			}
		}

		return checkedBoards;
	}
}
