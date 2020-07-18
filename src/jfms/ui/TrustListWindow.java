package jfms.ui;

import java.util.Arrays;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.stage.Window;

import jfms.fms.FmsManager;
import jfms.fms.IdentityManager;
import jfms.fms.Trust;
import jfms.fms.TrustManager;
import jfms.store.Store;

public class TrustListWindow {
	private final Mode mode;
	private final int identityId;
	private final TableView<TrustInfo> table = new TableView<>();

	public enum Mode {
		TRUST_LIST,
		TRUSTEE_LIST
	}

	private static class TrustCellImpl extends TableCell<TrustInfo, Integer> {
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

	public TrustListWindow(Mode mode, int identityId) {
		this.mode = mode;
		this.identityId = identityId;
		createTrustTable();
	}

	public void show(Window ownerWindow) {
		StringBuilder str = new StringBuilder();
		if (mode == Mode.TRUST_LIST) {
			str.append("Trust list of ");
		} else {
			str.append("Identities that trust ");
		}
		String idName = FmsManager.getInstance().getIdentityManager()
			.getIdentity(identityId).getName();
		str.append(idName);


		Scene scene = StyleFactory.getInstance().createScene(table);

		Stage stage = new Stage();
		stage.initOwner(ownerWindow);

		stage.setTitle(str.toString());

		stage.setScene(scene);
		stage.show();
	}

	private void createTrustTable() {
		TableColumn<TrustInfo,String> nameColumn = new TableColumn<>("Name");
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		nameColumn.setSortType(TableColumn.SortType.ASCENDING);
		table.getColumns().add(nameColumn);

		TableColumn<TrustInfo,String> sskColumn = new TableColumn<>("SSK");
		sskColumn.setCellValueFactory(new PropertyValueFactory<>("ssk"));
		sskColumn.setVisible(false);
		table.getColumns().add(sskColumn);

		if (mode == Mode.TRUSTEE_LIST) {
			TableColumn<TrustInfo,Integer> weightColumn =
				new TableColumn<>("Weight");
			weightColumn.setCellFactory(t -> new TrustCellImpl());
			weightColumn.setCellValueFactory(
					new PropertyValueFactory<>("weight"));
			table.getColumns().add(weightColumn);
		}

		TableColumn<TrustInfo,Integer> trustListTrustColumn =
			new TableColumn<>("TrustList Trust");
		trustListTrustColumn.setCellFactory(t -> new TrustCellImpl());
		trustListTrustColumn.setCellValueFactory(
				new PropertyValueFactory<>("trustListTrust"));

		TableColumn<TrustInfo,String> trustListTrustCommentColumn =
			new TableColumn<>("Comment");
		trustListTrustCommentColumn.setCellValueFactory(
				new PropertyValueFactory<>("trustListTrustComment"));

		TableColumn<TrustInfo,Integer> messageTrustColumn =
			new TableColumn<>("Message Trust");
		messageTrustColumn.setCellFactory(t -> new TrustCellImpl());
		messageTrustColumn.setCellValueFactory(
				new PropertyValueFactory<>("messageTrust"));

		TableColumn<TrustInfo,String> messageTrustCommentColumn =
			new TableColumn<>("Comment");
		messageTrustCommentColumn.setCellValueFactory(
				new PropertyValueFactory<>("messageTrustComment"));

		table.getColumns().addAll(Arrays.asList(
					trustListTrustColumn, trustListTrustCommentColumn,
					messageTrustColumn, messageTrustCommentColumn));

		table.setItems(getTrustList());
		table.setTableMenuButtonVisible(true);
	}

	private ObservableList<TrustInfo> getTrustList() {
		ObservableList<TrustInfo> trustList = FXCollections.observableArrayList();


		Store store = FmsManager.getInstance().getStore();
		List<Trust> storeTrustList;
		if (mode == Mode.TRUST_LIST) {
			storeTrustList = store.getNumericTrustList(identityId);
		} else {
			storeTrustList = store.getNumericTrusteeList(identityId);
		}

		IdentityManager identityManager = FmsManager.getInstance().getIdentityManager();
		TrustManager trustManager = FmsManager.getInstance().getTrustManager();

		for (Trust t : storeTrustList) {
			final TrustInfo trustInfo = new TrustInfo();
			final jfms.fms.Identity identity =
				identityManager.getIdentity(t.getIdentityId());
			trustInfo.setName(identity.getName());
			trustInfo.setSsk(identity.getSsk());
			if (mode == Mode.TRUSTEE_LIST) {
				trustInfo.setWeight(trustManager.getPeerTrustListTrust(
							t.getIdentityId()));
			}
			trustInfo.setTrustListTrust(t.getTrustListTrustLevel());
			trustInfo.setTrustListTrustComment(t.getTrustListTrustComment());
			trustInfo.setMessageTrust(t.getMessageTrustLevel());
			trustInfo.setMessageTrustComment(t.getMessageTrustComment());

			trustList.add(trustInfo);
		}

		return trustList;
	}
}
