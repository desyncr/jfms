package jfms.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import jfms.fms.FmsManager;
import jfms.fms.TrustManager;
import jfms.fms.Validator;
import jfms.store.Store;

public class IdentityWindow {
	private static final Logger LOG = Logger.getLogger(IdentityWindow.class.getName());
	private static final String UNKNOWN_TEXT = "<unknown>";

	private Stage stage;
	private final SplitPane splitPane;
	private final TableView<Identity> table;
	private final IdentityDetailsPane detailsPane = new IdentityDetailsPane();
	private final IdentityTrustPane trustPane = new IdentityTrustPane(true);
	private final TextField filterText = new TextField();
	private String lastFilter;


	private static class NameCellImpl extends TableCell<Identity, String> {
		@Override
		public void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setText(null);
				setGraphic(null);
			} else {
				if (item == null) {
					setText(UNKNOWN_TEXT);
				} else {
					setText(item);
				}
				setGraphic(null);
			}
		}
	}

	private static class TrustCellImpl extends TableCell<Identity, Integer> {
		@Override
		public void updateItem(Integer item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null || item == -1) {
				setText(null);
				setGraphic(null);
			} else {
				setText(item.toString());
				setGraphic(null);
			}
		}
	}

	public IdentityWindow() {
		table = createIdentityTable();

		trustPane.setTrustChangedCallback(t -> updateRow(t));
		trustPane.setTrustlistChangedCallback(id -> updateTrustList(id));

		Button refreshButton = new Button("Refresh");
		// TODO preserve sort order
		refreshButton.setOnAction(e -> {
			table.setItems(createIdentityList());
			table.scrollTo(0);
			updateTrustList(trustPane.getSelectedLocalIdentity());
		});

		Button addButton = new Button("Add Identity...");
		addButton.setOnAction(e -> showAddIdentityDialog());

		filterText.setPromptText("Enter name or SSK");
		filterText.textProperty().addListener((Observable v) -> {
			if (!Objects.equals(getFilter(), lastFilter)) {
				table.setItems(createIdentityList());
				table.scrollTo(0);
				updateTrustList(trustPane.getSelectedLocalIdentity());
			}
		});

		GridPane buttonGrid = new GridPane();
		buttonGrid.setHgap(10);
		buttonGrid.setVgap(2);
		buttonGrid.setPadding(new Insets(2, 5, 2, 5));
		buttonGrid.addRow(0, new Label("Filter"), filterText);
		buttonGrid.addRow(1, addButton, refreshButton);

		VBox leftPane = new VBox(table, buttonGrid);
		VBox.setVgrow(table, Priority.ALWAYS);

		VBox rightPane = new VBox(createTrustPane(), createDetailsPane());
		ScrollPane scrollPane = new ScrollPane(rightPane);
		scrollPane.setFitToWidth(true);

		splitPane = new SplitPane(leftPane, scrollPane);
		splitPane.setDividerPositions(0.4f);
	}

	private class IdentitySelectionChangeListener implements ChangeListener<Identity> {
		@Override
		public void changed(ObservableValue<? extends Identity> observable,
				Identity oldValue, Identity newValue) {

			int identityId = -1;
			if (newValue != null) {
				identityId = newValue.getId();
			}

			detailsPane.updateDetails(identityId);
			trustPane.updateTrustFields(identityId);
		}
	}

	public void show(Window ownerWindow) {
		if (stage == null) {
			Scene scene = StyleFactory.getInstance().createScene(splitPane);

			stage = new Stage();
			stage.initOwner(ownerWindow);
			stage.setTitle("Identities");
			stage.setScene(scene);
		}

		filterText.clear();
		lastFilter = null;
		table.setItems(createIdentityList());
		table.scrollTo(0);
		updateSort();

		trustPane.updateTrustCb();
		updateTrustList(trustPane.getSelectedLocalIdentity());

		stage.show();
	}

	public void hide() {
		stage.hide();
		table.getItems().clear();
	}


	private void updateTrustList(String localIdentityName) {
		final Map<Integer, Integer> localTrustListTrust;
		final Map<Integer, Integer> localMessageTrust;

		int localIdentityId = trustPane.getLocalIdentityId(localIdentityName);
		if (localIdentityId != -1) {
			final Store store = FmsManager.getInstance().getStore();
			localTrustListTrust = store.getLocalTrustListTrusts(localIdentityId, 0);
			localMessageTrust = store.getLocalMessageTrusts(localIdentityId);
		} else {
			localTrustListTrust = Collections.emptyMap();
			localMessageTrust = Collections.emptyMap();
		}

		for (Identity i : table.getItems()) {
			i.setLocalTrustListTrust(TrustManager
					.trustLevelToInt(localTrustListTrust.get(i.getId())));
			i.setLocalMessageTrust(TrustManager
					.trustLevelToInt(localMessageTrust.get(i.getId())));
		}
	}

	private TableView<Identity> createIdentityTable() {
		TableColumn<Identity,String> nameColumn = new TableColumn<>("Name");
		nameColumn.setPrefWidth(160);
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("Name"));
		nameColumn.setCellFactory(t -> new NameCellImpl());
		nameColumn.setComparator(Comparator.nullsLast(nameColumn.getComparator()));
		nameColumn.setSortType(TableColumn.SortType.ASCENDING);

		TableColumn<Identity,Integer> trustColumn = new TableColumn<>("Local\nMessage\nTrust");
		trustColumn.setCellFactory(t -> new TrustCellImpl());
		trustColumn.setCellValueFactory(new PropertyValueFactory<>("localMessageTrust"));

		TableColumn<Identity,Integer> trustListTrustColumn =
			new TableColumn<>("Local\nTrustlist\nTrust");
		trustListTrustColumn.setCellFactory(t -> new TrustCellImpl());
		trustListTrustColumn.setCellValueFactory(
				new PropertyValueFactory<>("localTrustListTrust"));

		TableColumn<Identity,Integer> peerTrustColumn = new TableColumn<>("Peer\nMessage\nTrust");
		peerTrustColumn.setCellFactory(t -> new TrustCellImpl());
		peerTrustColumn.setCellValueFactory(new PropertyValueFactory<>("peerMessageTrust"));

		TableColumn<Identity,Integer> peerTrustListTrustColumn =
			new TableColumn<>("Peer\nTrustlist\nTrust");
		peerTrustListTrustColumn.setCellFactory(t -> new TrustCellImpl());
		peerTrustListTrustColumn.setCellValueFactory(
				new PropertyValueFactory<>("peerTrustListTrust"));

		TableColumn<Identity,String> sskColumn = new TableColumn<>("SSK");
		sskColumn.setCellValueFactory(new PropertyValueFactory<>("ssk"));
		sskColumn.setVisible(false);

		TableView<Identity> tableView = new TableView<>();
		tableView.getColumns().addAll(Arrays.asList(
					nameColumn, sskColumn, trustColumn, trustListTrustColumn,
					peerTrustColumn, peerTrustListTrustColumn));

		tableView.getSortOrder().add(nameColumn);
		tableView.getSelectionModel().selectedItemProperty()
			.addListener(new IdentitySelectionChangeListener());

		tableView.setTableMenuButtonVisible(true);

		return tableView;
	}

	private TitledPane createTrustPane() {
		TitledPane titledPane = new TitledPane("Trust", trustPane.getNode());
		titledPane.setAnimated(false);

		return titledPane;
	}

	private TitledPane createDetailsPane() {
		TitledPane titledPane = new TitledPane("Details", detailsPane.getNode());
		titledPane.setAnimated(false);

		return titledPane;
	}

	private String getFilter() {
		String filter = filterText.getText();
		if (filter.length() < 3) {
			return null;
		}

		return filter.toLowerCase(Locale.ENGLISH);
	}

	private ObservableList<Identity> createIdentityList() {
		ObservableList<Identity> identityList = FXCollections.observableArrayList();
		TrustManager trustManager = FmsManager.getInstance().getTrustManager();

		final String filter = getFilter();
		lastFilter = filter;

		for (Map.Entry<Integer, jfms.fms.Identity> e : FmsManager.getInstance()
				.getIdentityManager().getIdentities().entrySet()) {

			final int identityId = e.getKey();
			final jfms.fms.Identity id= e.getValue();
			if (id== null) {
				continue;
			}

			if (filter != null) {
				final String fullName = id.getFullName()
					.toLowerCase(Locale.ENGLISH);
				if (!fullName.contains(filter)) {
					continue;
				}
			}

			Identity rowId = new Identity();
			rowId.setId(identityId);
			rowId.setName(id.getName());
			rowId.setSsk(id.getSsk());
			rowId.setPeerMessageTrust(trustManager.getPeerMessageTrust(identityId));
			rowId.setPeerTrustListTrust(trustManager.getPeerTrustListTrust(identityId));

			identityList.add(rowId);
		}

		return identityList;
	}

	private void addIdentity(String ssk) {
		if (!Validator.isValidSsk(ssk)) {
			LOG.log(Level.INFO, "Ignoring identity {0} (SSK invalid)", ssk);
			return;
		}

		FmsManager.getInstance().getIdentityManager().addManualIdentity(ssk);
	}

	private void showAddIdentityDialog() {
		Dialog<String> dialog = StyleFactory.getInstance()
				.createTextInputDialog();
		dialog.setTitle("Add Identity");
		dialog.setHeaderText("Enter SSK");
		dialog.setGraphic(null);

		Optional<String> result = dialog.showAndWait();
		if (result.isPresent() && !result.get().isEmpty()) {
			addIdentity(result.get());
		}
	}

	private void updateRow(jfms.fms.Trust trust) {
		// update row in table
		Identity id
			= table.getSelectionModel().getSelectedItem();
		if (id != null) {
			id.setLocalTrustListTrust(trust.getTrustListTrustLevel());
			id.setLocalMessageTrust(trust.getMessageTrustLevel());

			updateSort();
		}
	}

	private void updateSort() {
		ObservableList<TableColumn<Identity,?>> sortOrder =
			table.getSortOrder();
		sortOrder.add(table.getColumns().get(0));
		sortOrder.remove(sortOrder.size() - 1);
	}
}
