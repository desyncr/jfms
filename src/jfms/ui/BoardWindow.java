package jfms.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.stage.Window;

import jfms.fms.FmsManager;
import jfms.store.Store;

public class BoardWindow {
	private Stage stage;
	private final TableView<BoardInfo> table = new TableView<>();
	private NewsPane newsPane;

	public BoardWindow() {
		createBoardTable();
	}

	private class BoardChangeListener implements ListChangeListener<BoardInfo> {
		private final ObservableList<BoardInfo> list;

		public BoardChangeListener(ObservableList<BoardInfo> list) {
			this.list = list;
		}

		@Override
		public void onChanged(ListChangeListener.Change<? extends BoardInfo> c) {
			while (c.next()) {
				if (c.wasUpdated()) {
					for (int i=c.getFrom(); i<c.getTo(); i++) {
						BoardInfo board = list.get(i);
						if (board.getIsSubscribed()) {
							newsPane.subscribeBoard(board.getName());
						} else {
							newsPane.unsubscribeBoard(board.getName());
						}
					}
				}
			}
		}
	}

	public void show(Window ownerWindow, NewsPane newsPane) {
		if (stage == null) {
			this.newsPane = newsPane;
			stage = new Stage();

			Scene scene = StyleFactory.getInstance().createScene(table);
			stage.initOwner(ownerWindow);
			stage.setTitle("Boards");
			stage.setScene(scene);
		}

		table.setItems(getBoardInfoList());
		table.scrollTo(0);
		updateSort();

		stage.show();
	}

	public void hide() {
		stage.hide();
		table.getItems().clear();
	}

	private void createBoardTable() {
		TableColumn<BoardInfo,String> nameColumn = new TableColumn<>("Name");
		nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		nameColumn.setSortType(TableColumn.SortType.ASCENDING);

		TableColumn<BoardInfo,String> messageCountColumn = new TableColumn<>("Messages");
		messageCountColumn.setCellValueFactory(new PropertyValueFactory<>("messageCount"));

		TableColumn<BoardInfo,Boolean> subscribedColumn = new TableColumn<>("Subscribed");
		subscribedColumn.setCellValueFactory(new PropertyValueFactory<>("isSubscribed"));
		subscribedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(subscribedColumn));
		subscribedColumn.setEditable(true);

		table.getColumns().addAll(Arrays.asList(nameColumn, messageCountColumn, subscribedColumn));
		table.setEditable(true);

		table.setPrefWidth(300);
		table.getSortOrder().add(nameColumn);
	}

	private ObservableList<BoardInfo> getBoardInfoList() {
		ObservableList<BoardInfo> boardInfos = FXCollections.observableArrayList(
				(BoardInfo b) -> new Observable[]{b.isSubscribedProperty()});

		Store store = FmsManager.getInstance().getStore();
		List<String> subscribedBoards = store.getSubscribedBoardNames();

		for (Map.Entry<String,Integer> e : store.getBoardInfos().entrySet()) {
			final BoardInfo boardInfo = new BoardInfo();
			boardInfo.setName(e.getKey());
			boardInfo.setMessageCount(e.getValue());
			boardInfo.setIsSubscribed(subscribedBoards.contains(e.getKey()));
			boardInfos.add(boardInfo);
		}

		boardInfos.addListener(new BoardChangeListener(boardInfos));
		return boardInfos;
	}

	private void updateSort() {
		ObservableList<TableColumn<BoardInfo,?>> sortOrder =
			table.getSortOrder();
		sortOrder.add(table.getColumns().get(0));
		sortOrder.remove(sortOrder.size() - 1);
	}
}
