package jfms.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import jfms.config.ChoiceValidator;
import jfms.config.Config;
import jfms.config.ConfigEntry;
import jfms.config.Constants;

public class SettingsDialog {
	private final Dialog<ButtonType> dialog;
	private final GridPane grid = new GridPane();
	private int[] entries;
	private Control[] valueNodes;

	public SettingsDialog(Window ownerWindow) {
		entries = new int[0];

		final Node tree = createSettingsTree();
		SplitPane sp = new SplitPane(tree, createSettingsPane());
		SplitPane.setResizableWithParent(tree, false);

		dialog = StyleFactory.getInstance().createDialog();
		dialog.initOwner(ownerWindow);
		dialog.setTitle("Settings");
		dialog.setResizable(true);
		dialog.getDialogPane().setContent(sp);

		List<ButtonType> buttons = dialog.getDialogPane().getButtonTypes();
		buttons.add(ButtonType.OK);
		buttons.add(ButtonType.APPLY);
		buttons.add(ButtonType.CANCEL);

		Button okButton = (Button)dialog.getDialogPane()
			.lookupButton(ButtonType.OK);
		okButton.addEventHandler(ActionEvent.ACTION, new OkButtonHandler(false));

		Button applyButton = (Button)dialog.getDialogPane()
			.lookupButton(ButtonType.APPLY);
		applyButton.addEventFilter(ActionEvent.ACTION, new OkButtonHandler(true));
	}

	private class OkButtonHandler implements EventHandler<ActionEvent> {
		private final boolean doConsume;

		public OkButtonHandler(boolean doConsume) {
			this.doConsume = doConsume;
		}

		@Override
		public void handle(ActionEvent e) {
			if (doConsume) {
				e.consume();
			}

			saveChanges();
		}
	}

	private class GroupSelectionChangeListener
		implements ChangeListener<TreeItem<String>> {

		@Override
		public void changed(
				ObservableValue<? extends TreeItem<String>> observable,
				TreeItem<String> oldValue, TreeItem<String> newValue) {

			handleChanges();

			String infoText = null;
			grid.getChildren().clear();

			if (newValue == null) {
				entries = new int[0];
				return;
			}

			switch (newValue.getValue()) {
			case "Freenet":
				entries = new int[] {
					Config.FCP_HOST,
					Config.FCP_PORT
				};
				break;
			case "Message Composer":
				entries = new int[] {
					Config.MIN_MESSAGE_DELAY,
					Config.MAX_MESSAGE_DELAY,
				};
				break;
			case "Requests":
				entries = new int[] {
					Config.MESSAGEBASE,
					Config.FAST_MESSAGE_CHECK,
					Config.MAX_FCP_REQUESTS,
					Config.DOWNLOAD_PRIORITY,
					Config.UPLOAD_PRIORITY,
					Config.MAX_IDENTITY_AGE,
					Config.MAX_MESSAGE_AGE,
					Config.INACTIVITY_TIMEOUT,
					Config.INACTIVITY_RETRY_INTERVAL,
				};
				break;
			case "Trust":
				entries = new int[] {
					Config.MIN_LOCAL_TRUSTLIST_TRUST,
					Config.MIN_LOCAL_MESSAGE_TRUST,
					Config.MIN_PEER_TRUSTLIST_TRUST,
					Config.MIN_PEER_MESSAGE_TRUST,
					Config.DL_MSGLISTS_WITHOUT_TRUST,
					Config.DL_MESSAGES_WITHOUT_TRUST,
					Config.EXCLUDE_NULL_TRUST,
					Config.INDIRECT_TRUST_WEIGHT
				};
				break;
			case "Logging":
				entries = new int[] {
					Config.LOG_LEVEL
				};
				break;
			case "Theme":
				entries = new int[] {
					Config.ICON_SET,
					Config.FORCE_HIDPI_ICONS,
					Config.FORCE_HIDPI_EMOTICONS
				};
				break;
			case "Messages":
				entries = new int[] {
					Config.WEBVIEW,
					Config.SHOW_AVATARS,
				};
				infoText = "WebView uses a built-in HTML browser to render "
					+ "messages (requires restart). Certain features (e.g., "
					+ "emoticons) are only available in WebView.\n"
					+ "\n"
					+ "Avatars will be saved in directory "
					+ Constants.AVATAR_DIR + " if enabled (requires restart).";
				break;
			case "Virtual Folders":
				entries = new int[] {
					Config.SHOW_SUBSCRIBED_ONLY,
				};
				break;
			default:
				entries = new int[0];
				return;
			}

			valueNodes = new Control[entries.length];

			final Config config = Config.getInstance();
			int row = 0;
			for (int id: entries) {
				ConfigEntry entry = config.getEntry(id);

				Label label = new Label(entry.getName());
				Control value;

				switch (entry.getType()) {
				case BOOLEAN:
					CheckBox cb = new CheckBox();
					cb.setSelected(config.getBooleanValue(id));
					value = cb;
					break;
				case CHOICE:
					ChoiceValidator validator =
						(ChoiceValidator)entry.getValidator();
					List<String> items = validator.getAllowedValues();
					ChoiceBox<String> choice = new ChoiceBox<>(FXCollections
							.observableArrayList(items));
					choice.setValue(config.getStringValue(id));
					value = choice;
					break;
				default:
					value = new TextField(config.getStringValue(id));
				}

				if (entry.getDescription() != null) {
					Tooltip.install(value, new Tooltip(entry.getDescription()));
				}
				valueNodes[row] = value;
				grid.addRow(row++, label, value);
			}

			if (infoText != null) {
				Label info = new Label(infoText);
				info.setWrapText(true);
				grid.add(info, 0, row, 2, 1);
			}
		}
	}

	private String getValue(ConfigEntry entry, Control control) {
		String value;
		switch (entry.getType()) {
		case BOOLEAN:
			CheckBox cb = (CheckBox)control;
			value = Boolean.toString(cb.isSelected());
			break;
		case CHOICE:
			@SuppressWarnings("rawtypes")
			ChoiceBox choice = (ChoiceBox)control;
			value = (String)choice.getValue();
			break;
		default:
			TextField text = (TextField)control;
			value = text.getText();
			break;
		}

		return value;
	}

	private void handleChanges() {
		boolean changed = false;
		final Config config = Config.getInstance();
		for (int i=0; i<entries.length; i++) {
			int id = entries[i];
			String value = getValue(config.getEntry(id), valueNodes[i]);
			String oldValue = config.getStringValue(id);

			if (!value.equals(oldValue)) {
				changed = true;
				break;
			}

		}

		if (changed) {
			Alert alert = StyleFactory.getInstance().createAlert(
				Alert.AlertType.CONFIRMATION,
				"Settings have changed.\nDo you want to save the changes?",
				ButtonType.YES,
				ButtonType.NO
			);
			alert.setHeaderText("Apply settings");

			Optional<ButtonType> result = alert.showAndWait();
			ButtonType buttonType = ButtonType.CANCEL;
			if (result.isPresent())  {
				buttonType =  result.get();
			}

			if (buttonType == ButtonType.YES) {
				saveChanges();
			}
		}
	}

	private void saveChanges() {
		final Config config = Config.getInstance();
		for (int i=0; i<entries.length; i++) {
			int id = entries[i];
			String value = getValue(config.getEntry(id), valueNodes[i]);

			config.setStringValue(id, value);
		}

		config.saveToFile(Constants.JFMS_CONFIG_PATH);
	}

	public void showAndWait() {
		dialog.showAndWait();
	}

	private Node createSettingsTree() {
		TreeItem<String> generalFolder = new TreeItem<>("General Settings");
		generalFolder.getChildren().addAll(Arrays.asList(
				new TreeItem<>("Freenet"),
				new TreeItem<>("Logging")
				));
		generalFolder.setExpanded(true);

		TreeItem<String> fmsFolder = new TreeItem<>("FMS");
		fmsFolder.getChildren().addAll(Arrays.asList(
				new TreeItem<>("Message Composer"),
				new TreeItem<>("Requests"),
				new TreeItem<>("Trust")
				));
		fmsFolder.setExpanded(true);


		TreeItem<String> appearanceFolder = new TreeItem<>("Appearance");
		appearanceFolder.getChildren().addAll(Arrays.asList(
				new TreeItem<>("Theme"),
				new TreeItem<>("Messages"),
				new TreeItem<>("Virtual Folders")
				));
		appearanceFolder.setExpanded(true);

		TreeItem<String> rootItem = new TreeItem<>();
		rootItem.getChildren().addAll(Arrays.asList(
					generalFolder, fmsFolder, appearanceFolder));
		TreeView<String> treeView = new TreeView<>(rootItem);
		treeView.setShowRoot(false);

		treeView.getSelectionModel().selectedItemProperty().addListener(
				new GroupSelectionChangeListener());

		return treeView;
	}

	private Node createSettingsPane() {
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));
		grid.setPrefWidth(300);

		return grid;
	}
}
