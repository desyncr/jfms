package jfms.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;

import jfms.config.Config;
import jfms.config.Constants;
import jfms.fms.FmsManager;
import jfms.fms.xml.IdentityExportParser;
import jfms.fms.xml.IdentityExportWriter;
import jfms.store.Store;

public class LocalIdentityPane {
	private static final Logger LOG = Logger.getLogger(LocalIdentityPane.class.getName());
	private static final String BOLD_STYLE = "-fx-font-weight: bold;";

	private final TableView<LocalIdentity> table = new TableView<>();
	private Stage stage;
	private final Button defaultButton = new Button();
	private final Button announceButton = new Button();
	private final Button editButton = new Button();
	private final Button deleteButton = new Button();

	private static class IdChangeListener implements ListChangeListener<LocalIdentity> {
		private final ObservableList<LocalIdentity> list;

		public IdChangeListener(ObservableList<LocalIdentity> list) {
			this.list = list;
		}

		@Override
		public void onChanged(ListChangeListener.Change<? extends LocalIdentity> c) {
			final Store store = FmsManager.getInstance().getStore();

			while (c.next()) {
				if (c.wasUpdated()) {
					for (int i=c.getFrom(); i<c.getTo(); i++) {
						LocalIdentity id = list.get(i);
						store.setLocalIdentityActive(id.getId(),
								id.getIsActive());
					}
				}
			}
		}
	}

	private class NameCellImpl extends TableCell<LocalIdentity, String> {
		@Override
		public void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null) {
				setText(null);
				setGraphic(null);
			} else {
				LocalIdentity id = table.getItems().get(getIndex());
				if (isDefaultId(id.getId())) {
					setText(item + " (default)");
					setStyle(BOLD_STYLE);
				} else {
					setText(item);
					setStyle(null);
				}
				setGraphic(null);
			}
		}
	}

	private class IdSelectionChangeListener implements ChangeListener<LocalIdentity> {
		@Override
		public void changed(ObservableValue<? extends LocalIdentity> observable, LocalIdentity oldValue, LocalIdentity newValue) {
			if (newValue == null) {
				defaultButton.setDisable(true);
				announceButton.setDisable(true);
				editButton.setDisable(true);
				deleteButton.setDisable(true);
			} else {
				final int selectedId = newValue.getId();
				final boolean isDefault =
					selectedId == Config.getInstance().getDefaultId();
				defaultButton.setDisable(isDefault);
				announceButton.setDisable(false);
				editButton.setDisable(false);
				deleteButton.setDisable(false);
			}
		}
	}

	public void show(Window ownerWindow) {
		if (stage == null) {
			stage = new Stage();

			Scene scene = StyleFactory.getInstance().createScene(createContent());
			stage.initOwner(ownerWindow);
			stage.setTitle("Local Identities");
			stage.setScene(scene);
		}

		updateListItems();
		stage.show();
	}

	private VBox createContent() {
		Button addButton = new Button("Add...");
		addButton.setMaxWidth(120);
		addButton.setOnAction(e -> showAddIdentityDialog());

		announceButton.setText("Announce...");
		announceButton.setDisable(true);
		announceButton.setMaxWidth(120);
		announceButton.setOnAction(e -> showAnnounceIdentityDialog());

		Button importButton = new Button("Import...");
		importButton.setMaxWidth(120);
		importButton.setOnAction(e -> showImportDialog());

		Button exportButton = new Button("Export...");
		exportButton.setMaxWidth(120);
		exportButton.setOnAction(e -> showExportDialog());

		editButton.setText("Edit...");
		editButton.setDisable(true);
		editButton.setMaxWidth(120);
		editButton.setOnAction((ActionEvent e) -> {
			final LocalIdentity id = table.getSelectionModel().getSelectedItem();
			final EditLocalIdentityDialog dialog =
					new EditLocalIdentityDialog(id.getId(), null);
			if (dialog.showAndWait()) {
				updateListItems();
			}
		});

		defaultButton.setText("Set Default");
		defaultButton.setDisable(true);
		defaultButton.setMaxWidth(120);
		defaultButton.setOnAction((ActionEvent e) -> {
			final LocalIdentity id = table.getSelectionModel().getSelectedItem();
			if (id != null) {
				int storeId = id.getId();
				Config.getInstance().setStringValue(Config.DEFAULT_ID, Integer.toString(storeId));
				Config.getInstance().saveToFile(Constants.JFMS_CONFIG_PATH);
			}

			updateListItems();
		});

		deleteButton.setText("Delete");
		deleteButton.setMaxWidth(120);
		deleteButton.setOnAction((ActionEvent e) -> deleteSelectedIdentity());

		Label info = new Label("WARNING:\n- It is easy to detect if you are "
			+ "using jfms instead of the original client.\n- You should not "
			+ "use separate identities for security reasons.\n- It may be easy "
			+ "to guess if two identities are linked, especially if you do "
			+ "not run jfms 24/7.");
		info.setWrapText(true);


		VBox vbox = new VBox();
		vbox.setSpacing(10);
		vbox.setPadding(new Insets(0, 10, 0, 10));
		vbox.setSpacing(5.0);
		vbox.getChildren().addAll(addButton, announceButton,
				importButton, exportButton,
				editButton, defaultButton, deleteButton);


		TableColumn<LocalIdentity,String> nameColumn = new TableColumn<>("Name");
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		nameColumn.setCellFactory(t -> new NameCellImpl());
		nameColumn.setSortType(TableColumn.SortType.ASCENDING);
		nameColumn.prefWidthProperty().bind(table.widthProperty().multiply(0.8));

		TableColumn<LocalIdentity,Boolean> activeColumn = new TableColumn<>("active");
		activeColumn.setCellValueFactory(new PropertyValueFactory<>("isActive"));
		activeColumn.setCellFactory(CheckBoxTableCell.forTableColumn(activeColumn));
		activeColumn.setEditable(true);
		activeColumn.prefWidthProperty().bind(table.widthProperty().multiply(0.19));

		table.getColumns().addAll(Arrays.asList(nameColumn, activeColumn));

		table.getSelectionModel().selectedItemProperty()
			.addListener(new IdSelectionChangeListener());
		table.setPrefHeight(200);
		table.setEditable(true);



		HBox hbox = new HBox();
		hbox.getChildren().addAll(table, vbox);
		hbox.setPadding(new Insets(10, 10, 10, 10));
		HBox.setHgrow(table, Priority.ALWAYS);

		VBox mainVbox = new VBox(hbox, info);
		mainVbox.setPadding(new Insets(10, 10, 10, 10));

		return mainVbox;
	}

	private void showAddIdentityDialog() {
		if (FmsManager.getInstance().isOffline()) {
			Alert alert = StyleFactory.getInstance().createAlert(
					Alert.AlertType.INFORMATION);
			alert.setTitle("Error");
			alert.setHeaderText("Add identity failed");
			alert.setContentText("Cannot create identity when offline");
			alert.showAndWait();
			return;
		}

		final EditLocalIdentityDialog dialog = new EditLocalIdentityDialog(
				-1, b -> Platform.runLater(() -> handleKeyGenerated(b)));
		dialog.showAndWait();
	}

	private void showAnnounceIdentityDialog() {
		if (FmsManager.getInstance().isOffline()) {
			Alert alert = StyleFactory.getInstance().createAlert(
					Alert.AlertType.INFORMATION);
			alert.setTitle("Error");
			alert.setHeaderText("Announcement failed");
			alert.setContentText("Cannot announce identity when offline");
			alert.showAndWait();
			return;
		}
		final LocalIdentity id = table.getSelectionModel().getSelectedItem();
		final int storeId = id.getId();
		final PuzzleDialog dialog = new PuzzleDialog(storeId);
		if (dialog.showAndWait()) {
			updateListItems();
		}
	}

	private void showImportDialog() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open IdentityExport XML");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("XML Files", "*.xml"),
				new ExtensionFilter("All Files", "*.*"));

		File file = fileChooser.showOpenDialog(stage);
		if (file == null) {
			return;
		}

		LOG.log(Level.FINEST, "Importing local identities from {0}", file);
		try (FileInputStream fis = new FileInputStream(file)) {
			IdentityExportParser parser = new IdentityExportParser();
			List<jfms.fms.LocalIdentity> localIdentities = parser.parse(fis);
			if (localIdentities == null) {
				return ;
			}
			final Store store = FmsManager.getInstance().getStore();
			Set<String> existingSSKs = store.retrieveLocalIdentities()
				.entrySet().stream()
				.map(i -> i.getValue().getSsk())
				.collect(Collectors.toSet());

			List<jfms.fms.LocalIdentity> idsToImport = showDialog(
					"Import Identities",
					"Which identities do you want to import?",
					localIdentities, existingSSKs);

			FmsManager.getInstance().getIdentityManager()
				.importLocalIdentities(idsToImport);
			updateListItems();
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Failed to open {0}", file);
		}
	}

	private void showExportDialog() {
		final Store store = FmsManager.getInstance().getStore();
		final List<jfms.fms.LocalIdentity> localIds =
			store.retrieveLocalIdentities().entrySet().stream()
			.map(Map.Entry::getValue)
			.collect(Collectors.toList());

		List<jfms.fms.LocalIdentity> selectedIds = showDialog(
				"Export Identities",
				"Which identities do you want to export?",
				localIds,
				Collections.emptySet());
		if (selectedIds.isEmpty()) {
			return;
		}

		IdentityExportWriter writer = new IdentityExportWriter();
		byte[] data = writer.writeXml(selectedIds);

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save IdentityExport XML");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("XML Files", "*.xml"),
				new ExtensionFilter("All Files", "*.*"));

		File file = fileChooser.showSaveDialog(stage);
		if (file == null) {
			return;
		}

		LOG.log(Level.FINEST, "Saving local identities to {0}", file);
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(data);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Failed to write {0}", file);
		}
	}

	private List<jfms.fms.LocalIdentity> showDialog(String title,
			String header,
			List<jfms.fms.LocalIdentity> localIdentities,
			Set<String> disabledSSKs) {

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));

		final CheckBox[] cbs = new CheckBox[localIdentities.size()];
		int row = 0;
		for (jfms.fms.LocalIdentity id : localIdentities) {
			CheckBox cb = new CheckBox();
			if (disabledSSKs.contains(id.getSsk())) {
				cb.setDisable(true);
			} else {
				cb.setSelected(true);
			}
			cbs[row] = cb;
			grid.addRow(row++, cb, new Label(id.getFullName()));
		}

		Dialog<ButtonType> dialog =
			StyleFactory.getInstance().createAlert(Alert.AlertType.CONFIRMATION);
		dialog.initOwner(stage);
		dialog.setTitle(title);
		dialog.setHeaderText(header);
		dialog.setResizable(true);

		dialog.getDialogPane().setContent(grid);

		Optional<ButtonType> result = dialog.showAndWait();
		if (!result.isPresent() || result.get() != ButtonType.OK) {
			return Collections.emptyList();
		}

		List<jfms.fms.LocalIdentity> selectedIds = new ArrayList<>();

		row = 0;
		for (jfms.fms.LocalIdentity id : localIdentities) {
			if (cbs[row++].isSelected()) {
				selectedIds.add(id);
			}
		}

		return selectedIds;
	}

	private void deleteSelectedIdentity() {
		final LocalIdentity id = table.getSelectionModel().getSelectedItem();
		if (id == null) {
			return;
		}

		Alert dialog = StyleFactory.getInstance().createAlert(
				Alert.AlertType.CONFIRMATION);
		dialog.setTitle("Deleting local identity");
		dialog.setHeaderText("Are you sure you want to delete the "
				+ "local identity\n"
				+ id.getName());
		dialog.setContentText("The local identity and all associated data will "
			+ "be permanently deleted.");
		Optional<ButtonType> result = dialog.showAndWait();
		if (!result.isPresent() || result.get() != ButtonType.OK) {
			return;
		}

		final Store store = FmsManager.getInstance().getStore();
		int numericId = id.getId();
		store.deleteLocalIdentity(numericId);

		if (isDefaultId(numericId)) {
			final Config config = Config.getInstance();
			config.setStringValue(Config.DEFAULT_ID,
					Constants.DEFAULT_DEFAULT_ID);
			config.saveToFile(Constants.JFMS_CONFIG_PATH);
		}
		updateListItems();
	}

	private void updateListItems() {
		final Store store = FmsManager.getInstance().getStore();
		final Map<Integer, jfms.fms.LocalIdentity> storeIds =
			store.retrieveLocalIdentities();

		ObservableList<LocalIdentity> items = FXCollections.observableArrayList(
				(LocalIdentity i) -> new Observable[]{i.isActiveProperty()});
		for (Map.Entry<Integer, jfms.fms.LocalIdentity> e : storeIds.entrySet()) {
			final jfms.fms.LocalIdentity storeId = e.getValue();
			LocalIdentity id = new LocalIdentity();
			id.setId(e.getKey());
			id.setName(storeId.getFullName());
			id.setIsActive(storeId.getIsActive());

			items.add(id);
		}
		items.addListener(new IdChangeListener(items));
		table.setItems(items);
	}

	private void handleKeyGenerated(boolean ok) {
		if (stage == null || !stage.isShowing()) {
			return;
		}

		if (ok) {
			updateListItems();
		} else {
			Alert alert = StyleFactory.getInstance().createAlert(
					Alert.AlertType.WARNING);
			alert.setHeaderText("Failed to create identity");
			alert.setContentText(Constants.SEE_LOGS_TEXT);
			alert.showAndWait();
		}
	}

	private boolean isDefaultId(int id) {
		final int defaultId = Config.getInstance().getDefaultId();
		return id == defaultId;
	}
}
