package jfms.ui;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import jfms.config.Config;
import jfms.config.Constants;
import jfms.config.WindowInfo;
import jfms.fms.Attachment;
import jfms.fms.FmsManager;
import jfms.fms.LocalIdentity;
import jfms.fms.MessageListener;
import jfms.fms.MessageReference;
import jfms.fms.Sanitizer;
import jfms.fms.TrustManager;
import jfms.store.InsertStatus;
import jfms.store.MessageSearchCriteria;
import jfms.store.Store;

public class NewsPane {
	private static final Logger LOG = Logger.getLogger(NewsPane.class.getName());

	private static final String FOLDERS_TEXT = "Local Folders";
	private static final String VIRTUAL_FOLDERS_TEXT = "Virtual Folders";
	private static final String BOARDS_TEXT = "Boards";
	private static final String OUTBOX_TEXT = "Outbox";
	private static final String DRAFT_TEXT = "Drafts";
	private static final String SENT_TEXT = "Sent";
	private static final String RECENT_TEXT = "Recently Received";
	private static final String STARRED_TEXT = "Starred Messages";
	private static final String NO_FOLDER_TEXT = "No folder selected";
	private static final String NO_MESSAGES_TEXT = "No messages found";
	private static final String BOLD_STYLE = "-fx-font-weight: bold;";

	private final Stage primaryStage;
	private Mode mode = Mode.NONE;
	private Mode previousMode = Mode.NONE;
	private final SplitPane splitPane = new SplitPane();
	private final SplitPane msgSplitPane = new SplitPane();
	private final Button replyButton = new Button();
	private final ToggleButton showThreadsButton = new ToggleButton();
	private final ToggleButton monospaceButton = new ToggleButton();
	private final ToggleButton showEmoticonsButton = new ToggleButton();
	private final ToggleButton muteQuotesButton = new ToggleButton();
	private final ToggleButton showSignatureButton = new ToggleButton();
	private final ToggleButton detectLinksButton = new ToggleButton();
	private final ToggleButton showAttachmentsButton = new ToggleButton();
	private boolean threadedView;
	private boolean threadSort;
	private boolean collapseReadThreads;

	private final FmsManager fmsManager = FmsManager.getInstance();

	private TreeTableView<Message> headerTable;
	private TreeItem<Message> headerRootItem;
	private final Label placeHolderLabel = new Label(NO_FOLDER_TEXT);

	private String currentFolder;
	private final TreeView<Board> folderTree;
	private TreeItem<Board> fmsFolder;
	private TreeItem<Board> draftFolder;
	private TreeItem<Board> outFolder;
	private TreeItem<Board> virtualFolders;

	private final Map<String, MessageSearchCriteria> searchFolders = new HashMap<>();

	private MessageBodyView messageBody;
	private final VBox attachmentsVBox = new VBox();
	private final Message currentMessage = new Message();
	private AvatarImage avatar;
	private final EnumMap<Mode, ColumnSelector> columnSelectors =
		new EnumMap<>(Mode.class);
	private boolean ignoreMessageRowSelection = false;


	public enum Mode {
		NONE,
		OUTBOX,
		DRAFT,
		SENT,
		RECENTLY_RECEIVED,
		STARRED,
		SEARCH_RESULTS,
		BOARD,
	}

	public NewsPane(Stage stage) {
		final Config config =  Config.getInstance();

		primaryStage = stage;
		threadedView = config.getShowThreads();
		threadSort = config.getSortByMostRecentInThread();
		collapseReadThreads = config.getCollapseReadThreads();

		folderTree = createFolderPane();
		splitPane.getItems().addAll(folderTree, createMessagePane());
		splitPane.setDividerPositions((float)config.getFolderPaneWidth()/100);

		fmsManager.getMessageManager().setListener(new NewMessageListener());

		columnSelectors.put(Mode.NONE,
				new ColumnSelector(Collections.emptyList()));
		columnSelectors.put(Mode.OUTBOX,
				getLocalFolderColumns(Message.Status.QUEUED));
		columnSelectors.put(Mode.DRAFT,
				getLocalFolderColumns(Message.Status.DRAFT));
		columnSelectors.put(Mode.SENT,
				getLocalFolderColumns(Message.Status.SENT));
		final ColumnSelector virtualColumns = getBoardColumns();
		virtualColumns.setParameters("Starred,Subject,Reply Board,From,Date", "-Date");
		columnSelectors.put(Mode.RECENTLY_RECEIVED, virtualColumns);
		columnSelectors.put(Mode.STARRED, virtualColumns);
		columnSelectors.put(Mode.SEARCH_RESULTS, virtualColumns);
		columnSelectors.put(Mode.BOARD, getBoardColumns());
	}

	private MenuItem createDeleteLocalMessageMenuItem() {
		final MenuItem deleteMenuItem = new MenuItem("Delete Message");
		deleteMenuItem.setOnAction(e -> showDeleteMessageDialog());

		return deleteMenuItem;
	}

	public boolean isDeletable() {
		if (currentMessage.getIdentityId() == -1) {
			return false;
		}

		return mode == Mode.DRAFT || mode == Mode.OUTBOX;
	}

	private class LocalMessageTableCellImpl extends TreeTableCell<Message, String> {
		private final boolean showIcon;
		private final Message.Status messageStatus;

		public LocalMessageTableCellImpl(Message.Status messageStatus,
				boolean showIcon) {

			this.messageStatus = messageStatus;
			this.showIcon = showIcon;

			if (messageStatus == Message.Status.QUEUED) {

				setContextMenu(new ContextMenu(createDeleteLocalMessageMenuItem()));
			} else if (messageStatus == Message.Status.DRAFT) {
				final MenuItem editMenuItem = new MenuItem("Edit");
				editMenuItem.setOnAction(e ->
						editLocalMessage(getTreeTableRow().getItem()));

				setContextMenu(new ContextMenu(
					editMenuItem,
					createDeleteLocalMessageMenuItem()));
			}
		}

		@Override
		protected void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			Message msg = getTreeTableRow().getItem();
			if (empty || msg == null) {
				setText("");
				setGraphic(null);
			} else {
				setText(item);

				if (msg.getIsNew()) {
					setStyle(BOLD_STYLE);
				} else {
					setStyle(null);
				}

				if (showIcon) {
					Image messageIcon = Icons.getInstance()
							.getMessageIcon(messageStatus);
					setGraphic(new ImageView(messageIcon));
				} else {
					setGraphic(null);
				}
			}
		}
	}

	private class LocalMessageTableIntCellImpl extends TreeTableCell<Message, Integer> {
		public LocalMessageTableIntCellImpl(Message.Status messageStatus) {

			if (messageStatus == Message.Status.QUEUED) {
				setContextMenu(new ContextMenu(
						createDeleteLocalMessageMenuItem()));
			}
		}

		@Override
		protected void updateItem(Integer item, boolean empty) {
			super.updateItem(item, empty);
			Message msg = getTreeTableRow().getItem();
			if (empty || msg == null) {
				setText("");
				setGraphic(null);
			} else {
				setText(Integer.toString(item));
				setGraphic(null);

				if (msg.getIsNew()) {
					setStyle(BOLD_STYLE);
				} else {
					setStyle(null);
				}

			}
		}
	}

	private class MessageTableCellImpl extends TreeTableCell<Message, String> {
		private final boolean showIcon;

		public MessageTableCellImpl(boolean showIcon) {
			this.showIcon = showIcon;

			setContextMenu(createMessageContextMenu());
		}

		@Override
		protected void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			Message msg = getTreeTableRow().getItem();
			if (empty || msg == null) {
				setText("");
				setGraphic(null);
			} else {
				setText(item);

				if (msg.getIsNew()) {
					setStyle(BOLD_STYLE);
				} else {
					setStyle(null);
				}

				if (showIcon) {
					Image messageIcon;
					if (msg.getIsNew()) {
						messageIcon = Icons.getInstance()
							.getMessageIcon(Message.Status.UNREAD);
					} else {
						messageIcon = Icons.getInstance()
							.getMessageIcon(Message.Status.READ);
					}

					setGraphic(new ImageView(messageIcon));
				} else {
					setGraphic(null);
				}
			}
		}
	}

	// XXX avoid copy&paste
	private class MessageTableIntCellImpl extends TreeTableCell<Message, Integer> {
		public MessageTableIntCellImpl() {
			setContextMenu(createMessageContextMenu());
		}

		@Override
		protected void updateItem(Integer item, boolean empty) {
			super.updateItem(item, empty);
			Message msg = getTreeTableRow().getItem();
			if (empty || msg == null) {
				setText("");
				setGraphic(null);
			} else {
				if (item != null && item >= 0) {
					setText(Integer.toString(item));
				} else {
					setText("");
				}
				setGraphic(null);

				if (msg.getIsNew()) {
					setStyle(BOLD_STYLE);
				} else {
					setStyle(null);
				}
			}
		}
	}

	private class BoardTreeCellImpl extends TreeCell<Board> {
		private final ContextMenu boardsMenu = new ContextMenu();
		private final ContextMenu boardMenu = new ContextMenu();
		private final ContextMenu searchMenu = new ContextMenu();
		private final ContextMenu virtualFolderMenu = new ContextMenu();

		public BoardTreeCellImpl() {
			boardsMenu.getItems().addAll(
					createAddBoardMenuItem(),
					createMarkAllMessagesReadMenuItem());

			boardMenu.getItems().addAll(
					createMarkBoardReadMenuItem(),
					createRemoveBoardMenuItem());

			searchMenu.getItems().add(createRemoveSearchMenuItem());
			virtualFolderMenu.getItems().add(createRemoveSearchesMenuItem());
		}

		@Override
		public void updateItem(Board item, boolean empty) {
			super.updateItem(item, empty);

			if (!empty && item != null) {
				final Board board = getItem();
				StringBuilder text = new StringBuilder(board.getName());
				if (!board.isFolder() && (board.getUnreadMessageCount() > 0)) {
					setStyle(BOLD_STYLE);
					text.append(" (");
					text.append(board.getUnreadMessageCount());
					text.append(")");
				} else {
					setStyle(null);
				}
				setText(text.toString());
				setGraphic(getTreeItem().getGraphic());
				setTooltip(null);

				final String name = board.getName();
				final int level = headerTable.getTreeItemLevel(getTreeItem());
				switch (level) {
				case 0:
					break;
				case 1:
					if (BOARDS_TEXT.equals(name)) {
						setContextMenu(boardsMenu);
					} else if (VIRTUAL_FOLDERS_TEXT.equals(name)) {
						setContextMenu(virtualFolderMenu);
					}
					break;
				case 2:
					String parentName =
						getTreeItem().getParent().getValue().getName();
					if (BOARDS_TEXT.equals(parentName)) {
						setContextMenu(boardMenu);
					} else if (VIRTUAL_FOLDERS_TEXT.equals(parentName)) {
						if (!RECENT_TEXT.equals(name) && !STARRED_TEXT.equals(name)) {
							MessageSearchCriteria msc = searchFolders.get(name);
							setTooltip(new Tooltip(msc.toString()));
							setContextMenu(searchMenu);
						}
					}
					break;
				default:
					LOG.log(Level.WARNING, "unexpected tree item level: {0}",
							level);
				}
			} else {
				setText(null);
				setGraphic(null);
			}
		}
	}

	private class MessageRowSelectionChangeListener implements ChangeListener<TreeItem<Message>> {
		@Override
		public void changed(ObservableValue<? extends TreeItem<Message>> observable, TreeItem<Message> oldValue, TreeItem<Message> newValue) {

			if (ignoreMessageRowSelection) {
				// ignore selection updates while new messages are inserted
				return;
			}

			boolean replyDisabled = true;
			if (newValue != null) {
				switch (mode) {
				case BOARD:
				case RECENTLY_RECEIVED:
				case STARRED:
				case SEARCH_RESULTS:
					handleMessageSelected(newValue.getValue());
					replyDisabled = false;
					break;
				case DRAFT:
				case SENT:
				case OUTBOX:
					handleLocalMessageSelected(newValue.getValue());
					break;
				}
			} else {
				currentMessage.clear();
				messageBody.setText("No message selected", null);
				attachmentsVBox.getChildren().clear();
				avatar.setIdentityId(-1);
			}

			replyButton.setDisable(replyDisabled);
		}
	}

	private class FolderSelectionChangeListener implements ChangeListener<TreeItem<Board>> {
		@Override
		public void changed(ObservableValue<? extends TreeItem<Board>> observable, TreeItem<Board> oldValue, TreeItem<Board> newValue) {

			columnSelectors.get(mode).setParameters(headerTable);
			mode = Mode.NONE;
			currentFolder = null;
			if (newValue != null) {
				TreeItem<Board> parentNode = newValue.getParent();
				if (parentNode != null) {
					String folderName = newValue.getValue().getName();
					switch (parentNode.getValue().getName()) {
					case BOARDS_TEXT:
						mode = Mode.BOARD;
						currentFolder = folderName;
						break;
					case FOLDERS_TEXT:
						if (OUTBOX_TEXT.equals(folderName)) {
							mode = Mode.OUTBOX;
						} else if (DRAFT_TEXT.equals(folderName)) {
							mode = Mode.DRAFT;
						} else if (SENT_TEXT.equals(folderName)) {
							mode = Mode.SENT;
						}
						break;
					case VIRTUAL_FOLDERS_TEXT:
						if (RECENT_TEXT.equals(folderName)) {
							mode = Mode.RECENTLY_RECEIVED;
						} else if (STARRED_TEXT.equals(folderName)) {
							mode = Mode.STARRED;
						} else {
							mode = Mode.SEARCH_RESULTS;
							currentFolder = folderName;
						}
						break;
					}
				}
			}

			updateMessagePane();
		}
	}

	private class NewMessageListener implements MessageListener {
		@Override
		public void newMessage(jfms.fms.Message message) {
			Platform.runLater(() -> insertNewMessage(message));
		}

		@Override
		public void newLocalMessage(MessageReference msgRef,
				InsertStatus status, InsertStatus previousStatus) {

			Platform.runLater(() ->
					insertNewLocalMessage(msgRef, status, previousStatus));
		}
	}

	public MenuItem createMarkBoardReadMenuItem() {
		MenuItem menuItem = new MenuItem("Mark Board Read");
		menuItem.setOnAction(e ->
			markBoardRead(folderTree.getSelectionModel().getSelectedItem()));

		return menuItem;
	}

	public MenuItem createMarkAllMessagesReadMenuItem() {
		MenuItem menuItem = new MenuItem("Mark All Messages Read");
		menuItem.setOnAction(e -> markAllMessagesRead());

		return menuItem;
	}

	public MenuItem createAddBoardMenuItem() {
		MenuItem menuItem = new MenuItem("Subscribe Board...");
		menuItem.setOnAction((ActionEvent e) -> {
			Dialog<String> dialog = StyleFactory.getInstance()
					.createTextInputDialog();
			dialog.setTitle("Subscribe Board");
			dialog.setGraphic(null);
			dialog.setHeaderText("Name: ");
			Optional<String> result = dialog.showAndWait();
			if (result.isPresent() && !result.get().isEmpty()) {
				subscribeBoard(result.get());
			}
		});

		return menuItem;
	}

	public MenuItem createRemoveBoardMenuItem() {
		MenuItem menuItem = new MenuItem("Unsubscribe");
		menuItem.setOnAction((ActionEvent e) -> {
				final String boardName = folderTree.getSelectionModel()
						.getSelectedItem().getValue().getName();
				unsubscribeBoard(boardName);
		});

		return menuItem;
	}

	public MenuItem createRemoveSearchMenuItem() {
		MenuItem menuItem = new MenuItem("Remove");
		menuItem.setOnAction(e -> virtualFolders.getChildren()
				.remove(folderTree.getSelectionModel().getSelectedItem()));

		return menuItem;
	}

	public MenuItem createRemoveSearchesMenuItem() {
		MenuItem menuItem = new MenuItem("Remove All Search Results");
		menuItem.setOnAction(e -> {
			Iterator<TreeItem<Board>> iter =
				virtualFolders.getChildren().iterator();
			while (iter.hasNext()) {
				if (iter.next().getValue().getName().startsWith("Search #")) {
					iter.remove();
				}
			}
		});

		return menuItem;
	}

	public Node getNode() {
		return splitPane;
	}

	public void saveConfig(WindowInfo winInfo) {
		Config config = Config.getInstance();
		config.setStringValue(Config.SHOW_THREADS,
				Boolean.toString(threadedView));
		config.setStringValue(Config.SORT_BY_MOST_RECENT_IN_THREAD,
				Boolean.toString(threadSort));
		config.setStringValue(Config.COLLAPSE_READ_THREADS,
				Boolean.toString(collapseReadThreads));
		config.setStringValue(Config.MONOSPACE_FONT,
				Boolean.toString(monospaceButton.isSelected()));
		config.setStringValue(Config.EMOTICONS,
				Boolean.toString(showEmoticonsButton.isSelected()));
		config.setStringValue(Config.MUTE_QUOTES,
				Boolean.toString(muteQuotesButton.isSelected()));
		config.setStringValue(Config.SHOW_SIGNATURE,
				Boolean.toString(showSignatureButton.isSelected()));
		config.setStringValue(Config.DETECT_LINKS,
				Boolean.toString(detectLinksButton.isSelected()));
		config.setStringValue(Config.SHOW_ATTACHMENTS,
				Boolean.toString(showAttachmentsButton.isSelected()));

		config.setStringValue(Config.WINDOW_SIZE, winInfo.toString());
		config.setStringValue(Config.FOLDER_PANE_WIDTH, Long.toString(
					Math.round(splitPane.getDividerPositions()[0] * 100)));
		config.setStringValue(Config.HEADER_PANE_HEIGHT, Long.toString(
					Math.round(msgSplitPane.getDividerPositions()[0] * 100)));


		config.saveToFile(Constants.JFMS_CONFIG_PATH);
	}

	public boolean isReplyDisabled() {
		return replyButton.isDisabled();
	}

	public String getCurrentBoard() {
		return currentFolder;
	}

	public boolean getThreadedView() {
		return threadedView;
	}

	public void setThreadedView(boolean threaded, boolean toggleButton) {
		threadedView = threaded;
		if (toggleButton) {
			showThreadsButton.setSelected(threadedView);
			Utils.refreshToolBarButtonStyle(showThreadsButton);
		}
		updateMessagePane();
	}

	public boolean getThreadSort() {
		return threadSort;
	}

	public void setThreadSort(boolean sortByMostRecentInThread) {
		threadSort = sortByMostRecentInThread;

		for (ColumnSelector cs : columnSelectors.values()) {
			TreeTableColumn<Message,?> dateColumn = cs.getColumn("Date");
			if (dateColumn == null) {
				continue;
			}
			if (threadSort) {
				dateColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("lastReplyDate"));
			} else {
				dateColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("date"));
			}
		}

		// force update
		previousMode = Mode.NONE;
		updateMessagePane();
	}

	public void expandAllThreads(boolean expand) {
		int index = -1;
		TreeItem<Message> item;

		do {
			item = headerTable.getTreeItem(++index);
			if (item != null) {
				item.setExpanded(expand);
			}
		} while (item != null);
	}

	public boolean getCollapseReadThreads() {
		return collapseReadThreads;
	}

	public void setCollapseReadThreads(boolean collapseReadThreads) {
		this.collapseReadThreads = collapseReadThreads;

		// force update
		previousMode = Mode.NONE;
		updateMessagePane();
	}

	public boolean getUseMonospaceFont() {
		return monospaceButton.isSelected();
	}

	public void setUseMonospaceFont(boolean enable, boolean updateButton) {
		if (updateButton) {
			monospaceButton.setSelected(enable);
			Utils.refreshToolBarButtonStyle(monospaceButton);
		}
		messageBody.setUseMonospaceFont(enable);
	}

	public boolean getShowEmoticons() {
		return showEmoticonsButton.isSelected();
	}

	public void setShowEmoticons(boolean enable, boolean updateButton) {
		if (updateButton) {
			showEmoticonsButton.setSelected(enable);
			Utils.refreshToolBarButtonStyle(showEmoticonsButton);
		}
		messageBody.setShowEmoticons(enable);
	}

	public boolean getMuteQuotes() {
		return muteQuotesButton.isSelected();
	}

	public void setMuteQuotes(boolean enable, boolean updateButton) {
		if (updateButton) {
			muteQuotesButton.setSelected(enable);
			Utils.refreshToolBarButtonStyle(muteQuotesButton);
		}
		messageBody.setMuteQuotes(enable);
	}

	public boolean getShowSignature() {
		return showSignatureButton.isSelected();
	}

	public void setShowSignature(boolean enable, boolean updateButton) {
		if (updateButton) {
			showSignatureButton.setSelected(enable);
			Utils.refreshToolBarButtonStyle(showSignatureButton);
		}
		messageBody.setShowSignature(enable);
	}

	public boolean getDetectLinks() {
		return detectLinksButton.isSelected();
	}

	public void setDetectLinks(boolean enable, boolean updateButton) {
		if (updateButton) {
			detectLinksButton.setSelected(enable);
			Utils.refreshToolBarButtonStyle(detectLinksButton);
		}
		messageBody.setDetectLinks(enable);
	}

	public boolean getShowAttachments() {
		return showAttachmentsButton.isSelected();
	}

	public void setShowAttachments(boolean enable, boolean updateButton) {
		if (updateButton) {
			showAttachmentsButton.setSelected(enable);
			Utils.refreshToolBarButtonStyle(showAttachmentsButton);
		}
		updateAttachments(enable);
	}

	public Message getSelectedMessage() {
		if (mode == Mode.BOARD || mode == Mode.RECENTLY_RECEIVED ||
				mode == Mode.STARRED || mode == Mode.SEARCH_RESULTS ||
				mode == Mode.DRAFT) {
			Message message = headerTable.getSelectionModel()
				.getSelectedItem().getValue();
			LOG.log(Level.FINEST, "selected message with ID={0}",
					message.getStoreId());
			return message;
		}

		return null;
	}

	public List<Message> getSelectedThreadMessages() {
		TreeItem<Message> topLevelItem = headerTable.getSelectionModel()
			.getSelectedItem();
		int level = headerTable.getTreeItemLevel(topLevelItem);
		while (level > 1) {
			topLevelItem = topLevelItem.getParent();
			level--;
		}

		List<Message> messages = new ArrayList<>();
		List<TreeItem<Message>> itemsToProcess = new ArrayList<>();
		itemsToProcess.add(topLevelItem);

		while (!itemsToProcess.isEmpty()) {
			List<TreeItem<Message>> nextItemsToProcess = new ArrayList<>();
			for (TreeItem<Message> item : itemsToProcess) {
				messages.add(item.getValue());
				nextItemsToProcess.addAll(item.getChildren());
			}

			itemsToProcess = nextItemsToProcess;
		}

		return messages;
	}

	public void redrawRow(Message message) {
		// workaround to force re-rendering of single row
		Message originalMessage = new Message();
		originalMessage.setAll(message);
		message.clear();
		message.setAll(originalMessage);
	}

	public boolean isSubscribed(String boardName) {
		for (TreeItem<Board> b : fmsFolder.getChildren()) {
			if (b.getValue().getName().equals(boardName)) {
				return true;
			}
		}

		return false;
	}

	public boolean isSubscribed() {
		final String board = currentMessage.getReplyBoard();
		if (board == null || board.isEmpty()) {
			return true;
		}

		return isSubscribed(board);
	}

	public void subscribeBoard(String boardName) {
		String board = Sanitizer.sanitizeBoard(boardName);
		if (board.isEmpty()) {
			LOG.log(Level.INFO, "Board Name invalid: {0} ",
					Sanitizer.escapeString(boardName));
			return;
		}

		if (isSubscribed(boardName)) {
			LOG.log(Level.FINE, "Board {0} already subscribed", board);
			return;
		}

		jfms.fms.BoardManager boardManager = fmsManager.getBoardManager();
		if (boardManager.getBoardId(board) == null) {
			Store store = FmsManager.getInstance().getStore();
			int boardId = store.saveBoard(board, true);
			if (boardId != -1) {
				boardManager.addBoard(boardId, board);
			}
		}
		boardManager.subscribe(board);
		int unread = boardManager.getUnreadMessageCount(board);

		TreeItem<Board> newBoard = new TreeItem<>(
				new Board(board, unread),
				new ImageView(Icons.getInstance().getBoardIcon()));

		ObservableList<TreeItem<Board>> boardList = fmsFolder.getChildren();
		int boardListSize = boardList.size();
		boolean inserted = false;
		for (int i=0; i<boardListSize; i++) {
			String nextBoardName = boardList.get(i).getValue().getName();
			if (board.compareTo(nextBoardName) < 0) {
				boardList.add(i, newBoard);
				inserted = true;
				break;
			}
		}

		if (!inserted) {
			boardList.add(newBoard);
		}
	}

	public void unsubscribeBoard(String boardName) {
		Iterator<TreeItem<Board>> iter = fmsFolder.getChildren().iterator();
		while (iter.hasNext()) {
			TreeItem<Board> board = iter.next();
			if (board.getValue().getName().equals(boardName)) {
				iter.remove();
				break;
			}
		}

		fmsManager.getBoardManager().unsubscribe(boardName);
	}

	public MenuItem createMessageNewMenuItem() {
		final MenuItem menuItem = new MenuItem("New Message");
		menuItem.setOnAction(e -> handleNewMessage());

		return menuItem;
	}

	public MenuItem createMessageReplyMenuItem() {
		final MenuItem menuItem = new MenuItem("Reply");
		menuItem.setOnAction(e -> handleReply());

		return menuItem;
	}

	public MenuItem createMessageCopyUriMenuItem() {
		final MenuItem menuItem = new MenuItem("Copy URI to Clipboard");
		menuItem.setOnAction(e -> copyURIToClipboard());

		return menuItem;
	}

	public MenuItem createSubscribeMenuItem() {
		final MenuItem menuItem = new MenuItem("Subscribe Board");
		menuItem.setOnAction(e -> subscribeBoard(currentMessage.getReplyBoard()));

		return menuItem;
	}

	public MenuItem createMessageMarkUnreadMenuItem() {
		final MenuItem menuItem = new MenuItem("Mark as unread");
		menuItem.setOnAction((ActionEvent e) ->
				setMessageReadStatus(getSelectedMessage(), false));

		return menuItem;
	}

	public MenuItem createMessageMarkThreadAsReadMenuItem() {
		final MenuItem menuItem = new MenuItem("Mark Thread as read");
		menuItem.setOnAction((ActionEvent e) -> {
			for (Message m : getSelectedThreadMessages()) {
				setMessageReadStatus(m, true);
			}
		});

		return menuItem;
	}

	public MenuItem createMessageDeleteMenuItem() {
		final MenuItem menuItem = new MenuItem("Delete message...");
		menuItem.setOnAction(e -> showDeleteMessageDialog());

		return menuItem;
	}

	public MenuItem createMessageTrustMenuItem() {
		final MenuItem menuItem = new MenuItem("Identity Trust...");
		menuItem.setOnAction(e -> showTrustDialog());

		return menuItem;
	}

	public MenuItem createMessageIdentityDetailsMenuItem() {
		final MenuItem menuItem = new MenuItem("Identity Details...");
		menuItem.setOnAction(e -> showIdentityDetailsDialog());

		return menuItem;
	}

	public MenuItem createFindUserMessagesMenuItem() {
		final MenuItem menuItem = new MenuItem("Find messages by user");
		menuItem.setOnAction(e -> {
			MessageSearchCriteria msc = new MessageSearchCriteria();
			msc.setIdentityId(currentMessage.getIdentityId());
			msc.setFrom(currentMessage.getFromShort());
			createSearchResultsFolder(msc);
		});

		return menuItem;
	}

	public void gotoNextMessage() {
		int index = headerTable.getSelectionModel().getSelectedIndex() + 1;
		if (headerTable.getTreeItem(index) != null) {
			gotoMessage(index);
		}
	}

	public void gotoPreviousMessage() {
		int index = headerTable.getSelectionModel().getSelectedIndex() - 1;
		if (headerTable.getTreeItem(index) != null) {
			gotoMessage(index);
		}
	}

	public void gotoNextUnreadMessage() {
		int index = headerTable.getSelectionModel().getSelectedIndex();
		TreeItem<Message> item = null;

		do {
			item = headerTable.getTreeItem(++index);
			if (item == null) {
				break;
			}
		} while (!item.getValue().getIsNew());

		if (item != null) {
			gotoMessage(index);
		}
	}

	public void gotoPreviousUnreadMessage() {
		int index = headerTable.getSelectionModel().getSelectedIndex();
		TreeItem<Message> item = null;

		do {
			item = headerTable.getTreeItem(--index);
			if (item == null) {
				break;
			}
		} while (!item.getValue().getIsNew());

		if (item != null) {
			gotoMessage(index);
		}
	}

	public void gotoMessage(int index) {
		if (index < 0) {
			return;
		}
		headerTable.getSelectionModel().select(index);
		headerTable.scrollTo(Math.max(0, index-2));
	}

	public void createSearchResultsFolder(MessageSearchCriteria msc) {
		StringBuilder str = new StringBuilder("Search #");
		str.append(searchFolders.size() + 1);
		String searchName = str.toString();

		TreeItem<Board> searchFolder = new TreeItem<>(new Board(searchName),
				new ImageView(Icons.getInstance().getBoardIcon()));
		virtualFolders.getChildren().add(searchFolder);
		searchFolders.put(searchName, msc);
	}

	private TreeView<Board> createFolderPane() {

		int draftCount = 0;
		int unsentMessageCount = 0;
		Map<Integer, LocalIdentity> identities = fmsManager.getStore().retrieveLocalIdentities();
		for (Map.Entry<Integer, LocalIdentity> e : identities.entrySet()) {
			int localIdentityId = e.getKey();

			List<MessageReference> msgRefs = fmsManager.getStore().getLocalMessageList(localIdentityId, InsertStatus.DRAFT, null, -1);
			draftCount += msgRefs.size();

			msgRefs = fmsManager.getStore().getLocalMessageList(localIdentityId, InsertStatus.NOT_INSERTED, null, -1);
			unsentMessageCount += msgRefs.size();
		}

		draftFolder = new TreeItem<>(new Board(DRAFT_TEXT, draftCount),
				new ImageView(Icons.getInstance().getDraftIcon()));

		outFolder = new TreeItem<>(new Board(OUTBOX_TEXT, unsentMessageCount),
				new ImageView(Icons.getInstance().getOutboxIcon()));

		TreeItem<Board> sentFolder = new TreeItem<>(new Board(SENT_TEXT),
				new ImageView(Icons.getInstance().getSentIcon()));

		TreeItem<Board> localFolders = new TreeItem<>(
				new Board(FOLDERS_TEXT),
				new ImageView(Icons.getInstance().getBoardIcon()));
		localFolders.getChildren().addAll(
				Arrays.asList(draftFolder, outFolder, sentFolder));
		localFolders.setExpanded(true);

		TreeItem<Board> recentFolder = new TreeItem<>(new Board(RECENT_TEXT),
				new ImageView(Icons.getInstance().getBoardIcon()));
		TreeItem<Board> starredFolder = new TreeItem<>(new Board(STARRED_TEXT),
				new ImageView(Icons.getInstance().getBoardIcon()));
		virtualFolders = new TreeItem<>(
				new Board(VIRTUAL_FOLDERS_TEXT),
				new ImageView(Icons.getInstance().getBoardIcon()));
		virtualFolders.getChildren().addAll(Arrays.asList(
					recentFolder, starredFolder));
		virtualFolders.setExpanded(true);

		fmsFolder = new TreeItem<>(new Board(BOARDS_TEXT),
				new ImageView(Icons.getInstance().getBoardFolderIcon()));

		jfms.fms.BoardManager boardManager = fmsManager.getBoardManager();
		final Image folderIcon = Icons.getInstance().getBoardIcon();
		for (String boardName : boardManager.getSubscribedBoardNames()) {
			final int unread = boardManager.getUnreadMessageCount(boardName);
			TreeItem<Board> publicBoard = new TreeItem<>(
					new Board(boardName, unread),
					new ImageView(folderIcon));
			fmsFolder.getChildren().add(publicBoard);
		}

		fmsFolder.setExpanded(true);

		TreeItem<Board> boardListRoot = new TreeItem<>(new Board("FMS"));
		boardListRoot.getChildren().addAll(
				Arrays.asList(localFolders, virtualFolders, fmsFolder));
		boardListRoot.setExpanded(true);

		TreeView<Board> treeView = new TreeView<>(boardListRoot);
		treeView.setCellFactory((TreeView<Board> p) -> new BoardTreeCellImpl());
		treeView.getSelectionModel().selectedItemProperty().addListener(
				new FolderSelectionChangeListener());
		treeView.setShowRoot(false);

		return treeView;
	}

	private Node createMessagePane() {
		if (Config.getInstance().getWebViewEnabled()) {
			messageBody = new MessageBodyWebView();
		} else {
			messageBody = new MessageBodyTextView();
		}
		messageBody.setText("No message to display", null);

		Button newMessageButton = new Button(null,
				new ImageView(Icons.getInstance().getNewMessageIcon()));
		newMessageButton.setTooltip(new Tooltip("New Message"));
		newMessageButton.setOnAction((ActionEvent e) -> handleNewMessage());
		Utils.setToolBarButtonStyle(newMessageButton);

		replyButton.setGraphic(
				new ImageView(Icons.getInstance().getReplyIcon()));
		replyButton.setTooltip(new Tooltip("Reply"));
		Utils.setToolBarButtonStyle(replyButton);
		replyButton.setOnAction((ActionEvent e) -> handleReply());
		replyButton.setDisable(true);

		final Config config = Config.getInstance();

		showThreadsButton.setGraphic(
			new ImageView(Icons.getInstance().getThreadIcon()));
		showThreadsButton.setOnAction((ActionEvent e) ->
			setThreadedView(showThreadsButton.isSelected(), false));
		showThreadsButton.setTooltip(new Tooltip(Constants.SHOW_THREADS_TEXT));
		showThreadsButton.setSelected(threadedView);
		Utils.setToolBarButtonStyle(showThreadsButton);

		showEmoticonsButton.setGraphic(
			new ImageView(Icons.getInstance().getEmoticonIcon()));
		showEmoticonsButton.setOnAction((ActionEvent e) ->
				setShowEmoticons(showEmoticonsButton.isSelected(), false));
		showEmoticonsButton.setTooltip(new Tooltip(Constants.EMOTICON_TEXT));
		showEmoticonsButton.setSelected(config.getShowEmoticons());
		Utils.setToolBarButtonStyle(showEmoticonsButton);
		messageBody.setShowEmoticons(config.getShowEmoticons());

		monospaceButton.setGraphic(
				new ImageView(Icons.getInstance().getFontIcon()));
		monospaceButton.setOnAction((ActionEvent e) ->
				setUseMonospaceFont(monospaceButton.isSelected(), false));
		monospaceButton.setTooltip(new Tooltip(Constants.MONOSPACE_TEXT));
		monospaceButton.setSelected(config.getUseMonospaceFont());
		Utils.setToolBarButtonStyle(monospaceButton);
		messageBody.setUseMonospaceFont(config.getUseMonospaceFont());

		muteQuotesButton.setGraphic(
			new ImageView(Icons.getInstance().getMuteIcon()));
		muteQuotesButton.setOnAction((ActionEvent e) ->
				setMuteQuotes(muteQuotesButton.isSelected(), false));
		muteQuotesButton.setTooltip(new Tooltip(Constants.MUTE_QUOTES_TEXT));
		muteQuotesButton.setSelected(config.getMuteQuotes());
		Utils.setToolBarButtonStyle(muteQuotesButton);
		messageBody.setMuteQuotes(config.getMuteQuotes());

		showSignatureButton.setGraphic(
			new ImageView(Icons.getInstance().getSignatureIcon()));
		showSignatureButton.setOnAction((ActionEvent e) ->
				setShowSignature(showSignatureButton.isSelected(), false));
		showSignatureButton.setTooltip(new Tooltip(Constants.SHOW_SIGNATURE_TEXT));
		showSignatureButton.setSelected(config.getShowSignature());
		Utils.setToolBarButtonStyle(showSignatureButton);
		messageBody.setShowSignature(config.getShowSignature());

		detectLinksButton.setGraphic(
			new ImageView(Icons.getInstance().getLinkIcon()));
		detectLinksButton.setOnAction((ActionEvent e) ->
				setDetectLinks(detectLinksButton.isSelected(), false));
		detectLinksButton.setTooltip(new Tooltip(Constants.DETECT_LINKS_TEXT));
		detectLinksButton.setSelected(config.getDetectLinks());
		Utils.setToolBarButtonStyle(detectLinksButton);
		messageBody.setDetectLinks(config.getDetectLinks());

		showAttachmentsButton.setGraphic(
			new ImageView(Icons.getInstance().getAttachmentIcon()));
		showAttachmentsButton.setOnAction((ActionEvent e) ->
				setShowAttachments(showAttachmentsButton.isSelected(), false));
		showAttachmentsButton.setTooltip(new Tooltip(Constants.SHOW_ATTACHMENTS_TEXT));
		showAttachmentsButton.setSelected(config.getShowAttachments());
		Utils.setToolBarButtonStyle(showAttachmentsButton);

		ToolBar toolBar = new ToolBar(newMessageButton, replyButton,
			new Separator(Orientation.VERTICAL),
			showThreadsButton, monospaceButton, showEmoticonsButton,
			muteQuotesButton, showSignatureButton, detectLinksButton,
			showAttachmentsButton);


		headerTable = createHeaderTable();
		VBox treeTableVbox = new VBox();
		treeTableVbox.getChildren().addAll(toolBar, headerTable);
		VBox.setVgrow(headerTable, Priority.ALWAYS);

		final Node body = messageBody.getNode();
		VBox msgVbox = new VBox(createHeaders(), body,
				attachmentsVBox);
		VBox.setVgrow(body, Priority.ALWAYS);

		msgSplitPane.getItems().setAll(treeTableVbox, msgVbox);
		msgSplitPane.setOrientation(Orientation.VERTICAL);
		msgSplitPane.setDividerPositions(
				(float)config.getHeaderPaneHeight()/100);

		return msgSplitPane;
	}

	private TreeTableView<Message> createHeaderTable() {
		TreeTableView<Message> treeTableView = new TreeTableView<>();
		treeTableView.getSelectionModel().selectedItemProperty()
			.addListener(new MessageRowSelectionChangeListener());

		headerRootItem = new TreeItem<>();
		treeTableView.setRoot(headerRootItem);
		treeTableView.setShowRoot(false);
		treeTableView.setPlaceholder(placeHolderLabel);
		treeTableView.setTableMenuButtonVisible(true);
		treeTableView.setMaxHeight(Double.MAX_VALUE);
		treeTableView.setOnMouseClicked(e -> {
			if (e.getClickCount() > 1 && mode == Mode.DRAFT) {
				TreeItem<Message> selectedItem = headerTable.getSelectionModel()
					.getSelectedItem();
				if (selectedItem != null) {
					Message message = selectedItem.getValue();
					editLocalMessage(message);
				}
			}
		});

		return treeTableView;
	}

	private ColumnSelector getLocalFolderColumns(
			Message.Status messageStatus) {

		TreeTableColumn<Message,String> fromColumn = new TreeTableColumn<>("From");
		fromColumn.setCellFactory((TreeTableColumn<Message, String> p) ->
				new LocalMessageTableCellImpl(messageStatus, true));
		fromColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("fromShort"));


		TreeTableColumn<Message,String> dateColumn = new TreeTableColumn<>("Date");
		dateColumn.setCellFactory((TreeTableColumn<Message, String> p) ->
				new LocalMessageTableCellImpl(messageStatus, false));
		dateColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("date"));
		dateColumn.setSortType(TreeTableColumn.SortType.DESCENDING);



		TreeTableColumn<Message,Integer> indexColumn = new TreeTableColumn<>("Index");
		indexColumn.setCellFactory((TreeTableColumn<Message, Integer> p) ->
				new LocalMessageTableIntCellImpl(messageStatus));
		indexColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("index"));

		ColumnSelector localFolderColumns = new ColumnSelector(Arrays.asList(
					fromColumn, dateColumn, indexColumn));
		localFolderColumns.setParameters("From,Date,Index", "-Date");

		return localFolderColumns;
	}

	private ColumnSelector getBoardColumns() {
		TreeTableColumn<Message,Boolean> starColumn = new TreeTableColumn<>("Starred");
		starColumn.setGraphic(new ImageView(Icons.getInstance().getStarredIcon()));
		starColumn.setCellFactory((TreeTableColumn<Message, Boolean > p) ->
			new StarCellImpl());
		starColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("isStarred"));

		TreeTableColumn<Message,String> subjectColumn = new TreeTableColumn<>("Subject");
		subjectColumn.setCellFactory((TreeTableColumn<Message, String> p) ->
				new MessageTableCellImpl(true));
		subjectColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("subject"));


		TreeTableColumn<Message,String> fromColumn = new TreeTableColumn<>("From");
		fromColumn.setCellFactory((TreeTableColumn<Message, String> p) ->
				new MessageTableCellImpl(false));
		fromColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("fromShort"));

		TreeTableColumn<Message,String> idColumn = new TreeTableColumn<>("Identity");
		idColumn.setCellFactory((TreeTableColumn<Message, String> p) ->
				new MessageTableCellImpl(false));
		idColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("from"));


		TreeTableColumn<Message,String> dateColumn = new TreeTableColumn<>("Date");
		dateColumn.setCellFactory((TreeTableColumn<Message, String> p) ->
				new MessageTableDateCellImpl(false));
		dateColumn.setSortType(TreeTableColumn.SortType.DESCENDING);

		if (threadSort) {
			dateColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("lastReplyDate"));
		} else {
			dateColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("date"));
		}

		TreeTableColumn<Message,String> insertDateColumn = new TreeTableColumn<>("Insert Date");
		insertDateColumn.setCellFactory((TreeTableColumn<Message, String> p) ->
				new MessageTableCellImpl(false));
		insertDateColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("indexDate"));

		TreeTableColumn<Message,Integer> indexColumn = new TreeTableColumn<>("Index");
		indexColumn.setCellFactory((TreeTableColumn<Message, Integer> p) ->
				new MessageTableIntCellImpl());
		indexColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("index"));

		TreeTableColumn<Message,String> boardColumn = new TreeTableColumn<>("Reply Board");
		boardColumn.setCellFactory((TreeTableColumn<Message, String> p) ->
				new MessageTableCellImpl(false));
		boardColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("replyBoard"));

		TreeTableColumn<Message, Integer> localTrustColumn = new TreeTableColumn<>("Local Trust");
		localTrustColumn.setCellFactory((TreeTableColumn<Message, Integer> p) ->
				new MessageTableIntCellImpl());
		localTrustColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("localTrustLevel"));

		TreeTableColumn<Message, Integer> peerTrustColumn = new TreeTableColumn<>("Peer Trust");
		peerTrustColumn.setCellFactory((TreeTableColumn<Message, Integer> p) ->
				new MessageTableIntCellImpl());
		peerTrustColumn.setCellValueFactory(new TreeItemPropertyValueFactory<>("peerTrustLevel"));


		ColumnSelector columnSelector = new ColumnSelector(Arrays.asList(
				starColumn,
				subjectColumn, fromColumn, idColumn, dateColumn,
				insertDateColumn, indexColumn, boardColumn,
				localTrustColumn, peerTrustColumn));
		columnSelector.setParameters("Starred,Subject,From,Date", "-Date");

		return columnSelector;
	}

	private class MessageTableDateCellImpl extends TreeTableCell<Message, String> {
		public MessageTableDateCellImpl(boolean showIcon) {
			setContextMenu(createMessageContextMenu());
		}

		@Override
		protected void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			Message msg = getTreeTableRow().getItem();
			if (empty || msg == null) {
				setText("");
				setGraphic(null);
			} else {
				setText(msg.getDate());
				setGraphic(null);

				if (msg.getIsNew()) {
					setStyle(BOLD_STYLE);
				} else {
					setStyle(null);
				}
			}
		}
	}

	private static class StarCellImpl extends TreeTableCell<Message, Boolean> {
		public StarCellImpl() {
			setOnMouseClicked(e -> {
				final Message msg = getTreeTableRow().getItem();
				boolean isStarred = !msg.getIsStarred();
				msg.setIsStarred(isStarred);

				final Store store = FmsManager.getInstance().getStore();
				store.setMessageFlags(msg.getStoreId(),
					isStarred ? Constants.MSG_FLAG_STARRED : 0);
			});
		}

		@Override
		protected void updateItem(Boolean item, boolean empty) {
			super.updateItem(item, empty);
			Message msg = getTreeTableRow().getItem();
			setText("");
			if (empty || msg == null || !msg.getIsStarred()) {
				setGraphic(null);
			} else {
				Image messageIcon = Icons.getInstance().getStarredIcon();
				setGraphic(new ImageView(messageIcon));
			}
		}
	}

	private Node createHeaders() {
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));

		Label fromLabel = new Label("From:");
		Label dateLabel = new Label("Date:");
		Label boardsLabel = new Label("Boards:");
		Label messageIdLabel = new Label("Message ID:");
		Label messageTrustLabel = new Label("Peer Message Trust:");

		fromLabel.setStyle(BOLD_STYLE);
		dateLabel.setStyle(BOLD_STYLE);
		boardsLabel.setStyle(BOLD_STYLE);
		messageIdLabel.setStyle(BOLD_STYLE);
		messageTrustLabel.setStyle(BOLD_STYLE);

		Label fromText = new Label();
		fromText.textProperty().bind(currentMessage.fromProperty());

		Label boardsText = new Label();
		boardsText.textProperty().bind(currentMessage.boardsProperty());

		Label dateText = new Label();
		dateText.textProperty().bind(currentMessage.dateProperty());

		Label messageIdText = new Label();
		messageIdText.textProperty().bind(currentMessage.messageIdProperty());

		Label messageTrustText = new Label();
		messageTrustText.textProperty().bindBidirectional(
				currentMessage.peerTrustLevelProperty(),
				new OptionalNumberStringConverter());


		avatar = new AvatarImage(true);
		final Config config = Config.getInstance();
		if (config.getShowAvatars()) {
			grid.add(avatar.getImageView(), 0, 0, 1, GridPane.REMAINING);
			ColumnConstraints avatarColumn =
				new ColumnConstraints(avatar.getSize());
			avatarColumn.setHalignment(HPos.CENTER);
			grid.getColumnConstraints().add(avatarColumn);
		}

		int row = -1;
		grid.addRow(++row, fromLabel, fromText);
		grid.addRow(++row, dateLabel, dateText);
		grid.addRow(++row, boardsLabel, boardsText);
		grid.addRow(++row, messageIdLabel, messageIdText);
		grid.addRow(++row, messageTrustLabel, messageTrustText);


		TitledPane titledPane = new TitledPane("", grid);
		titledPane.setAnimated(false);
		titledPane.setExpanded(false);

		Platform.runLater(() -> {
			Node n = titledPane.lookup(".title");
			if (n != null) {
				n.setStyle(BOLD_STYLE);
			}
		});

		titledPane.textProperty().bind(currentMessage.subjectProperty());

		return titledPane;
	}

	private ContextMenu createMessageContextMenu() {
		final MenuItem subscribeMenuItem = createSubscribeMenuItem();

		ContextMenu menu = new ContextMenu(
			createMessageReplyMenuItem(),
			createMessageDeleteMenuItem(),
			new SeparatorMenuItem(),
			createMessageMarkUnreadMenuItem(),
			createMessageMarkThreadAsReadMenuItem(),
			new SeparatorMenuItem(),
			createMessageTrustMenuItem(),
			createMessageIdentityDetailsMenuItem(),
			createFindUserMessagesMenuItem(),
			new SeparatorMenuItem(),
			subscribeMenuItem,
			createMessageCopyUriMenuItem());

		menu.setOnShowing(e -> subscribeMenuItem.setDisable(isSubscribed()));

		return menu;
	}

	private void handleNewMessage() {
		MessageWindow messageWindow = new MessageWindow(currentFolder);
		messageWindow.show(null);
	}

	private void handleReply() {
		Store store = FmsManager.getInstance().getStore();
		Message message = getSelectedMessage();
		jfms.fms.Message fmsMessage = store.getMessage(
				message.getStoreId());
		if (fmsMessage == null) {
			return;
		}
		MessageWindow messageWindow = new MessageWindow(fmsMessage, false);
		messageWindow.show(null);
	}

	private void copyURIToClipboard() {
		final String ssk = FmsManager.getInstance().getIdentityManager().
			getSsk(currentMessage.getIdentityId());
		if (ssk == null) {
			LOG.log(Level.WARNING, "failed to lookup ID {0}",
					currentMessage.getIdentityId());
			return;
		}

		final LocalDate date = LocalDate.parse(
				currentMessage.getIndexDate(),
				DateTimeFormatter.ISO_LOCAL_DATE);
		String uri = jfms.fms.Identity.getMessageKey(ssk,
				date, currentMessage.getIndex());

		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final ClipboardContent content = new ClipboardContent();
		content.putString(uri);
		clipboard.setContent(content);
	}


	private void showTrustDialog() {
		final int identityId = currentMessage.getIdentityId();
		jfms.fms.Identity identity = FmsManager.getInstance()
			.getIdentityManager()
			.getIdentity(identityId);
		if (identity == null) {
			LOG.log(Level.INFO, "identity {0} not found", identityId);
			return;
		}

		IdentityTrustPane trustPane = new IdentityTrustPane(false);
		trustPane.updateTrustCb();
		trustPane.updateTrustFields(identityId);

		Dialog<ButtonType> dialog =
			StyleFactory.getInstance().createDialog();
		dialog.initOwner(primaryStage);
		dialog.setTitle("Edit Trust of " + identity.getFullName());
		dialog.setResizable(true);
		dialog.getDialogPane().setContent(trustPane.getNode());
		dialog.getDialogPane().getButtonTypes().addAll(
				ButtonType.OK, ButtonType.CANCEL);

		Optional<ButtonType> result = dialog.showAndWait();
		if (result.isPresent() && result.get() == ButtonType.OK) {
			trustPane.applySettings();
		}
	}

	private void showIdentityDetailsDialog() {
		IdentityDetailsPane detailsPane = new IdentityDetailsPane();
		detailsPane.updateDetails(currentMessage.getIdentityId());

		Dialog<ButtonType> dialog =
			StyleFactory.getInstance().createDialog();
		dialog.initOwner(primaryStage);
		dialog.setTitle("Identity Details");
		dialog.setResizable(true);
		dialog.getDialogPane().setContent(detailsPane.getNode());
		dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

		dialog.showAndWait();
	}

	private void showDeleteMessageDialog() {
		final int messageId = currentMessage.getStoreId();
		if (messageId == -1) {
			LOG.log(Level.INFO, "tried to delete non-existing message");
			return;
		}

		final boolean isDraft = mode == Mode.DRAFT || mode == Mode.OUTBOX;

		String subject = currentMessage.getSubject();
		final int maxSubjectLength = 50;
		if (subject.length() > maxSubjectLength) {
			subject = subject.substring(0, maxSubjectLength-3) + "...";
		}

		Alert dialog = StyleFactory.getInstance().createAlert(
				Alert.AlertType.CONFIRMATION);
		dialog.setTitle("Delete message");
		dialog.setHeaderText("Are you sure you want to delete the selected "
				+ "message\n\"" + subject + "\"?");
		if (!isDraft) {
			dialog.setContentText("If the sender is trusted and the message "
				+ "was sent recently,\n"
				+ "it may be downloaded again in the future.");
		}

		Optional<ButtonType> result = dialog.showAndWait();
		if (!result.isPresent() || result.get() != ButtonType.OK) {
			return;
		}

		if (isDraft) {
			final Message message = headerTable.getSelectionModel()
				.getSelectedItem().getValue();
			deleteLocalMessage(message);
			return;
		}

		final Store store = FmsManager.getInstance().getStore();
		store.removeMessage(messageId);

		TreeItem<Message> item = headerTable.getSelectionModel()
			.getSelectedItem();
		List<TreeItem<Message>> siblings = item.getParent().getChildren();

		int index = siblings.indexOf(item);
		siblings.remove(index);
		for (TreeItem<Message> child : item.getChildren()) {
			siblings.add(index, child);
		}
	}

	private void updateMessagePane() {
		boolean modeChanged = mode != previousMode;
		messageBody.setHighlight(null);

		switch (mode) {
		case NONE:
			if (modeChanged) {
				placeHolderLabel.setText(NO_FOLDER_TEXT);
			}
			break;
		case OUTBOX:
			if (modeChanged) {
				placeHolderLabel.setText(NO_MESSAGES_TEXT);
			}
			createLocalMessageTree(InsertStatus.NOT_INSERTED);
			break;
		case DRAFT:
			createLocalMessageTree(InsertStatus.DRAFT);
			if (modeChanged) {
				placeHolderLabel.setText(NO_MESSAGES_TEXT);
			}
			break;
		case SENT:
			createLocalMessageTree(InsertStatus.INSERTED);
			if (modeChanged) {
				placeHolderLabel.setText(NO_MESSAGES_TEXT);
			}
			break;
		case BOARD:
			if (modeChanged) {
				placeHolderLabel.setText(NO_MESSAGES_TEXT);
			}
			createFmsMessageTree(currentFolder, threadedView);
			break;
		case RECENTLY_RECEIVED:
			if (modeChanged) {
				placeHolderLabel.setText(NO_MESSAGES_TEXT);
			}
			createRecentFmsMessageTree(threadedView);
			break;
		case STARRED:
			if (modeChanged) {
				placeHolderLabel.setText(NO_MESSAGES_TEXT);
			}
			createStarredFmsMessageTree(threadedView);
			break;
		case SEARCH_RESULTS:
			if (modeChanged) {
				placeHolderLabel.setText(NO_MESSAGES_TEXT);
			}
			createSearchResultsMessageTree(currentFolder, threadedView);
			break;
		}

		// we always apply settings to keep folders sorted
		columnSelectors.get(mode).apply(headerTable);
		previousMode = mode;

		headerTable.scrollTo(0);
		currentMessage.clear();
		avatar.setIdentityId(-1);
	}

	private Message createLocalMessage(MessageReference msgRef) {
		Message message = new Message();
		message.setDate(msgRef.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
		message.setIndexDate(msgRef.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
		message.setIndex(msgRef.getIndex());

		return message;
	}

	private Message createMessage(jfms.fms.Message fmsMessage) {
		String name = "not found";
		final int identityId = fmsMessage.getIdentityId();
		jfms.fms.Identity id = fmsManager
			.getIdentityManager()
			.getIdentity(identityId);
		if (id != null) {
			name = id.getFullName();
		}

		Message message = new Message();
		message.setStoreId(fmsMessage.getMessageId());
		message.setIndexDate(fmsMessage.getInsertDate()
				.format(DateTimeFormatter.ISO_LOCAL_DATE));
		message.setIndex(fmsMessage.getInsertIndex());
		message.setIdentityId(identityId);
		message.setSubject(fmsMessage.getSubject());
		message.setFrom(name);

		StringBuilder str = new StringBuilder();
		str.append(fmsMessage.getDate()
				.format(DateTimeFormatter.ISO_LOCAL_DATE));
		str.append(" ");
		str.append(fmsMessage.getTime()
				.format(DateTimeFormatter.ISO_LOCAL_TIME));
		message.setDate(str.toString());
		message.setLastReplyDate(str.toString());

		message.setMessageId(fmsMessage.getMessageUuid());
		message.setReplyBoard(fmsMessage.getReplyBoard());
		message.setBoardList(fmsMessage.getBoards());
		message.setBoards();
		message.setIsNew(!fmsMessage.getRead());
		message.setIsStarred((fmsMessage.getFlags() & Constants.MSG_FLAG_STARRED) != 0);
		message.setParentMessageId(fmsMessage.getParentId());
		if (id != null) {
			final TrustManager trustManager = fmsManager.getTrustManager();
			int localTrust = trustManager.getLocalMessageTrust(identityId);
			if (localTrust != -1) {
				message.setLocalTrustLevel(localTrust);
			}
			int peerTrust = trustManager.getPeerMessageTrust(identityId);
			if (peerTrust != -1) {
				message.setPeerTrustLevel(peerTrust);
			}
		}

		return message;
	}

	private void insertNewMessage(jfms.fms.Message message) {
		for (String boardName: message.getBoards()) {
			if (boardName.equals(currentFolder)) {
				TreeItem<Message> selectedItem = headerTable.getSelectionModel()
					.getSelectedItem();
				ignoreMessageRowSelection = true;
				insertMessageIntoTree(createMessage(message),
						threadedView, headerRootItem.getChildren());

				// force resorting (is there a simpler way?)
				ObservableList<TreeTableColumn<Message,?>> sortOrder =
					headerTable.getSortOrder();
				sortOrder.add(headerTable.getColumns().get(0));
				sortOrder.remove(sortOrder.size() - 1);

				// restore selected item
				// some item insertions/permutations break selection
				headerTable.getSelectionModel().select(selectedItem);
				ignoreMessageRowSelection = false;
			}

			changeReadCount(Mode.BOARD, boardName, 1);
		}
	}

	private void hideLocalMessage(MessageReference msgRef) {
		List<TreeItem<Message>> items = headerRootItem.getChildren();
		for (int i=0; i<items.size(); i++) {
			Message msg = items.get(i).getValue();
			String date = msgRef.getDate().format(
					DateTimeFormatter.ISO_LOCAL_DATE);
			if (msgRef.getIdentityId() == msg.getIdentityId() &&
					msgRef.getIndex() == msg.getIndex() &&
					date.equals(msg.getIndexDate())) {

				items.remove(i);
				break;
			}
		}
	}

	private void insertNewLocalMessage(MessageReference msgRef,
			InsertStatus status, InsertStatus previousStatus) {

		Map<Integer, LocalIdentity> identities = fmsManager.getStore().retrieveLocalIdentities();
		LocalIdentity id = identities.get(msgRef.getIdentityId());

		Message message = createLocalMessage(msgRef);
		message.setIdentityId(msgRef.getIdentityId());
		message.setFrom(id.getFullName());


		boolean doHide = false;
		boolean doInsert = false;
		int draftDelta = 0;
		int outBoxDelta = 0;
		switch (mode) {
		case DRAFT:
			if (status == InsertStatus.DRAFT) {
				if (previousStatus == InsertStatus.DRAFT) {
					doHide = true;
				}
				doInsert = true;
			} else  {
				doHide = true;
			}
			message.setIsNew(true);
			break;
		case OUTBOX:
			doHide = status == InsertStatus.INSERTED;
			doInsert = status == InsertStatus.NOT_INSERTED;
			message.setIsNew(true);
			break;
		case SENT:
			doInsert = status == InsertStatus.INSERTED;
			break;
		}

		switch (status) {
		case DRAFT:
			if (previousStatus != InsertStatus.DRAFT) {
				draftDelta++;
			}
			break;
		case NOT_INSERTED:
			if (previousStatus == InsertStatus.DRAFT) {
				draftDelta--;
			}
			outBoxDelta++;
			break;
		case INSERTED:
			// inserted is also sent when message is deleted
			if (previousStatus == InsertStatus.DRAFT) {
				draftDelta--;
			}
			if (previousStatus == InsertStatus.NOT_INSERTED) {
				outBoxDelta--;
			}
			break;
		}

		if (doHide) {
			hideLocalMessage(msgRef);
		}

		if (doInsert) {
			insertMessageIntoTree(message, threadedView,
					headerRootItem.getChildren());
		}

		changeReadCount(Mode.DRAFT, null, draftDelta);
		changeReadCount(Mode.OUTBOX, null, outBoxDelta);
	}

	private void createLocalMessageTree(InsertStatus insertStatus) {
		List<Message> boardMessages = new ArrayList<>();

		Map<Integer, LocalIdentity> identities = fmsManager.getStore().retrieveLocalIdentities();
		for (Map.Entry<Integer, LocalIdentity> e : identities.entrySet()) {
			int localIdentityId = e.getKey();
			LocalIdentity localIdentity = e.getValue();

			List<MessageReference> msgRefs = fmsManager.getStore().getLocalMessageList(localIdentityId, insertStatus, null, -1);
			for (MessageReference msgRef : msgRefs) {
				Message message = createLocalMessage(msgRef);
				message.setIdentityId(localIdentityId);
				message.setFrom(localIdentity.getFullName());

				message.setIsNew(insertStatus == InsertStatus.NOT_INSERTED ||
						insertStatus == InsertStatus.DRAFT);

				boardMessages.add(message);
			}
		}

		createMessageTree(boardMessages, false);
	}

	private void createFmsMessageTree(String boardName, boolean threaded) {
		List<Message> boardMessages = new ArrayList<>();

		for (jfms.fms.Message m : fmsManager.getStore().getMessagesForBoard(boardName)) {
			boardMessages.add(createMessage(m));
		}

		createMessageTree(boardMessages, threaded);
	}

	private void createRecentFmsMessageTree(boolean threaded) {
		List<Message> boardMessages = new ArrayList<>();

		for (jfms.fms.Message m : fmsManager.getStore().getRecentMessages(
				Config.getInstance().getShowSubscribedOnly())) {

			boardMessages.add(createMessage(m));
		}

		createMessageTree(boardMessages, threaded);
	}

	private void createStarredFmsMessageTree(boolean threaded) {
		List<Message> boardMessages = new ArrayList<>();

		for (jfms.fms.Message m : fmsManager.getStore().getStarredMessages()) {
			boardMessages.add(createMessage(m));
		}

		createMessageTree(boardMessages, threaded);
	}

	private void createSearchResultsMessageTree(String searchName,
			boolean threaded) {

		MessageSearchCriteria msc = searchFolders.get(searchName);

		List<Message> boardMessages = new ArrayList<>();

		for (jfms.fms.Message m : fmsManager.getStore().findMessages(msc)) {
			boardMessages.add(createMessage(m));
		}

		createMessageTree(boardMessages, threaded);
		messageBody.setHighlight(msc.getBody());
	}

	private void insertMessageIntoTree(final Message m, boolean threaded,
			final List<TreeItem<Message>> rootNodes) {
		TreeItem<Message> newNode = new TreeItem<>(m);
		if (!threaded) {
			rootNodes.add(newNode);
			return;
		}

		boolean isRootNode = true;

		final String newMessageId = m.getMessageId();

		// check if we are parent of any existing nodes
		Iterator<TreeItem<Message>> iter = rootNodes.iterator();
		while (iter.hasNext()) {
			TreeItem<Message> rootNode = iter.next();
			final String rootNodeParentId = rootNode.getValue().getParentMessageId();

			if (rootNodeParentId != null && rootNodeParentId.equals(newMessageId)) {
				iter.remove();
				newNode.getChildren().add(rootNode);
				newNode.setExpanded(true);
			}
		}

		// check if any existing node is our parent
		final String newParentId = m.getParentMessageId();
		if (newParentId != null) {
			for (TreeItem<Message> rootNode : rootNodes) {
				TreeItem<Message> parent = findMessageNode(rootNode,
						newParentId);
				if (parent != null) {
					if (m.getDate().compareTo(parent.getValue().getDate()) > 0) {
						parent.getValue().setLastReplyDate(m.getDate());
					}
					parent.getChildren().add(newNode);
					isRootNode = false;

					// expand thread so the new message is not hidden
					while (parent != null) {
						parent.setExpanded(true);
						parent = parent.getParent();
					}

					break;
				}
			}
		}

		if (isRootNode) {
			rootNodes.add(newNode);
		}
	}

	private List<TreeItem<Message>> createMessageTreeNodes(
			List<Message> messages, boolean threaded) {

		final List<TreeItem<Message>> rootNodes = new ArrayList<>();
		if (!threaded) {
			for (Message m : messages) {
				rootNodes.add(new TreeItem<>(m));
			}

			return rootNodes;
		}

		final Map<String, TreeItem<Message>> nodesMap = new HashMap<>();
		final Map<String, Message> messageMap = new HashMap<>();

		for (Message m : messages) {
			messageMap.put(m.getMessageId(), m);
		}

		for (Message m : messages) {
			final String id = m.getMessageId();
			TreeItem<Message> node = nodesMap.computeIfAbsent(id,
					k -> new TreeItem<>(m));

			String parentId = m.getParentMessageId();
			if (parentId == null || !messageMap.containsKey(parentId)) {
				// message without parent -> create node at top level
				rootNodes.add(node);
				continue;
			}

			// if absent: parent not yet allocated -> create new node for parent
			TreeItem<Message> parentNode = nodesMap.computeIfAbsent(parentId,
					k -> new TreeItem<>(messageMap.get(parentId)));

			// avoid being-your-own-parent paradox
			boolean isWellFormed = true;
			TreeItem<Message> ancestor = parentNode;
			String newestDate = m.getDate();
			m.setLastReplyDate(newestDate);

			while (ancestor != null) {
				if (ancestor.getValue().getMessageId().equals(id)) {
					isWellFormed = false;
					break;
				}
				ancestor = ancestor.getParent();
			}

			// create new node for message and add as child
			if (isWellFormed) {
				parentNode.getChildren().add(node);
			} else {
				rootNodes.add(node);
			}
		}

		for (TreeItem<Message> node : nodesMap.values()) {

			final Message message = node.getValue();
			String newestDate = message.getDate();
			TreeItem<Message> ancestor = node.getParent();
			final boolean expand = collapseReadThreads && message.getIsNew();

			// threads below top level are always expanded
			if (!collapseReadThreads || ancestor != null) {
				node.setExpanded(true);
			}

			while (ancestor != null) {
				if (expand) {
					ancestor.setExpanded(true);
				}
				if (newestDate.compareTo(ancestor.getValue().getLastReplyDate()) > 0) {
					ancestor.getValue().setLastReplyDate(newestDate);
				}
				ancestor = ancestor.getParent();
			}
		}

		return rootNodes;
	}

	private void markBoardRead(TreeItem<Board> selectedBoard) {
		final String boardName = selectedBoard.getValue().getName();
		jfms.fms.BoardManager boardManager = fmsManager.getBoardManager();

		boardManager.setBoardMessagesRead(boardName, true);
		selectedBoard.setValue(new Board(boardName, 0));

		int row = 0;
		TreeItem<Message> msgItem;
		while ((msgItem = headerTable.getTreeItem(row++)) != null) {
			final Message msg = msgItem.getValue();
			if (!msg.getIsNew()) {
				continue;
			}

			msg.setIsNew(false);
			redrawRow(msg);

			final int boardCount = msg.getBoardList().size();
			if (boardCount > 1) {
				for (int i=1; i<boardCount; i++) {
					changeReadCount(Mode.BOARD, msg.getBoardList().get(i), -1);
				}
			}
		}
	}

	private void markAllMessagesRead() {
		Store store = FmsManager.getInstance().getStore();
		store.setAllMessagesRead();

		for (TreeItem<Board> b : fmsFolder.getChildren()) {
			b.setValue(new Board(b.getValue().getName(), 0));
		}

		switch (mode) {
		case RECENTLY_RECEIVED:
		case STARRED:
		case SEARCH_RESULTS:
		case BOARD:
			break;
		default:
			return;
		}

		int row = 0;
		TreeItem<Message> msgItem;
		while ((msgItem = headerTable.getTreeItem(row++)) != null) {
			final Message msg = msgItem.getValue();
			if (!msg.getIsNew()) {
				continue;
			}

			msg.setIsNew(false);
			redrawRow(msg);
		}
	}

	private void handleMessageSelected(Message message) {
		final int identityId = message.getIdentityId();
		final jfms.fms.Identity identity = FmsManager.getInstance()
			.getIdentityManager()
			.getIdentity(identityId);
		final String signature;
		if (identity != null) {
			signature = identity.getSignature();
		} else {
			LOG.log(Level.INFO, "identity {0} not found", identityId);
			signature = null;
		}

		final Store store = FmsManager.getInstance().getStore();
		final int id = message.getStoreId();
		final String body = store.getMessageBody(id);

		currentMessage.setAll(message);
		messageBody.setText(body, signature);
		updateAttachments(getShowAttachments());
		setMessageReadStatus(message, true);
		avatar.setIdentityId(identityId);
	}

	private void updateAttachments(boolean showAttachments) {
		attachmentsVBox.getChildren().clear();
		if (!showAttachments) {
			return;
		}

		final int messageId = currentMessage.getStoreId();
		if (messageId == -1) {
			return;
		}

		final Store store = FmsManager.getInstance().getStore();
		List<Attachment> attachments = store.getAttachments(messageId);

		for (Attachment a : attachments) {
			final String key = a.getKey();
			String name = "";
			final int slashIndex = key.indexOf('/');
			if (slashIndex != -1 && slashIndex < key.length()) {
				name = key.substring(slashIndex+1);
			}
			if (name.isEmpty()) {
				name = "no name";
			}

			final Hyperlink link = new Hyperlink(name);
			link.setTooltip(new Tooltip("Size: " + a.getSize() + " bytes"));

			final MenuItem copyMenuItem = new MenuItem("Copy Link to Clipboard");
			copyMenuItem.setOnAction(e -> {
					Clipboard clipboard = Clipboard.getSystemClipboard();
					ClipboardContent content = new ClipboardContent();
					content.putString(key);
					clipboard.setContent(content);
			});

			ContextMenu contextMenu = new ContextMenu(copyMenuItem);
			link.setOnMouseClicked(e ->
					contextMenu.show(link, e.getScreenX(), e.getScreenY()));

			attachmentsVBox.getChildren().add(link);
		}
	}

	private jfms.fms.Message getLocalMessage(Message message) {
		Store store = FmsManager.getInstance().getStore();
		final LocalDate indexDate = LocalDate.parse(message.getIndexDate(),
					DateTimeFormatter.ISO_LOCAL_DATE);
		String xmlMessage = store.getLocalMessage(message.getIdentityId(),
				indexDate, message.getIndex());

		jfms.fms.xml.MessageParser parser = new jfms.fms.xml.MessageParser();
		jfms.fms.Message parsedMessage = parser.parse(new ByteArrayInputStream(
					xmlMessage.getBytes(StandardCharsets.UTF_8)));
		if (parsedMessage != null) {
			parsedMessage.setInsertDate(indexDate);
			parsedMessage.setInsertIndex(message.getIndex());
		}

		return parsedMessage;
	}

	private void handleLocalMessageSelected(Message message) {
		jfms.fms.Message parsedMessage = getLocalMessage(message);
		if (parsedMessage == null) {
			currentMessage.clear();
			messageBody.setText("failed to parse message", null);
			attachmentsVBox.getChildren().clear();
			avatar.setIdentityId(-1);
			return;
		}

		Message localMessage = createMessage(parsedMessage);
		localMessage.setFrom(message.getFrom());

		currentMessage.setAll(localMessage);
		messageBody.setText(parsedMessage.getBody(), null);
		avatar.setIdentityId(-1);
	}

	private TreeItem<Message> findMessageNode(TreeItem<Message> node, String messageId) {
		Message msg = node.getValue();
		if (msg != null && msg.getMessageId().equals(messageId)) {
			return node;
		}

		for (TreeItem<Message> child : node.getChildren()) {
			TreeItem<Message> requestedNode = findMessageNode(child, messageId);
			if (requestedNode != null) {
				return requestedNode;
			}
		}

		return null;
	}

	private void createMessageTree(List<Message> boardMessages, boolean threaded) {
		List<TreeItem<Message>> rootNodes =
			createMessageTreeNodes(boardMessages, threaded);

		LOG.log(Level.FINEST, "found {0} messages in {1} threads", new Object[]{
				boardMessages.size(), rootNodes.size()});

		TreeItem<Message> newMessagesRoot = new TreeItem<>();
		newMessagesRoot.getChildren().setAll(rootNodes);

		headerTable.setRoot(newMessagesRoot);
		headerRootItem = newMessagesRoot;
	}

	private void setMessageReadStatus(Message message, boolean markRead) {
		if (message.getIsNew() == markRead) {
			message.setIsNew(!markRead);
			redrawRow(message);

			final Store store = FmsManager.getInstance().getStore();
			store.setMessageRead(message.getStoreId(), markRead);

			for (String boardName : message.getBoardList()) {
				changeReadCount(Mode.BOARD, boardName, markRead ? -1 : 1);
			}
		}
	}

	private void changeReadCount(Mode mode, String name, int delta) {
		if (delta == 0) {
			return;
		}

		TreeItem<Board> boardItem = null;

		if (mode == Mode.DRAFT) {
			boardItem = draftFolder;
		} else if (mode == Mode.OUTBOX ) {
			boardItem = outFolder;
		} else if (mode == Mode.BOARD) {
			// TODO is there an easy way to avoid iterating over
			// all shown boards?
			for (TreeItem<Board> b : fmsFolder.getChildren()) {
				if (b.getValue().getName().equals(name)) {
					boardItem = b;
				}
			}
		}

		if (boardItem == null) {
			// cross-posted board not subscribed, skip
			return;
		}

		int count = boardItem.getValue().getUnreadMessageCount() + delta;
		if (count < 0) {
			LOG.log(Level.WARNING, "tried to set read count < 0");
			count = 0;
		}
		boardItem.setValue(new Board(boardItem.getValue().getName(), count));
	}

	private void editLocalMessage(Message message) {
		jfms.fms.Message parsedMessage = getLocalMessage(message);
		if (parsedMessage == null) {
			LOG.log(Level.WARNING, "failed to retrieve local message");
			return;
		}
		parsedMessage.setIdentityId(message.getIdentityId());

		MessageWindow messageWindow = new MessageWindow(parsedMessage, true);
		messageWindow.show(null);
	}


	private void deleteLocalMessage(Message message) {
		LocalDate date = LocalDate.parse(message.getIndexDate(),
				DateTimeFormatter.ISO_LOCAL_DATE);

		FmsManager.getInstance().getMessageManager().deleteQueuedMessage(
				message.getIdentityId(), date, message.getIndex());
		MessageReference msgRef = new MessageReference();
		msgRef.setIdentityId(message.getIdentityId());
		msgRef.setDate(LocalDate.parse(message.getIndexDate(),
					DateTimeFormatter.ISO_LOCAL_DATE));
		msgRef.setIndex(message.getIndex());

		final InsertStatus previousStatus;
		switch (mode) {
		case DRAFT:
			previousStatus = InsertStatus.DRAFT;
			break;
		case OUTBOX:
			previousStatus = InsertStatus.NOT_INSERTED;
			break;
		default:
			previousStatus = InsertStatus.IGNORE;
		}

		// INSERTED also handles deleted messages (UI behavior is the same)
		insertNewLocalMessage(msgRef, InsertStatus.INSERTED, previousStatus);
	}
}
