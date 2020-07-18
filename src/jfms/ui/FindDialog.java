package jfms.ui;

import java.util.List;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

import jfms.store.MessageSearchCriteria;

public class FindDialog {
	private final Dialog<ButtonType> dialog;
	private final TextField fromText = new TextField();
	private final TextField subjectText = new TextField();
	private final TextField boardText = new TextField();
	private final TextField uuidText = new TextField();
	private final TextField bodyText = new TextField();
	private static final String BOLD_STYLE = "-fx-font-weight: bold;";

	public FindDialog(Window ownerWindow) {
		GridPane grid = new GridPane();

		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));

		Label fromLabel = new Label("From:");
		Label subjectLabel = new Label("Subject:");
		Label boardLabel = new Label("Board:");
		Label uuidLabel = new Label("UUID:");
		Label bodyLabel = new Label("Body:");
		Label infoLabel =
			new Label("Search results will create a new Virtual Folder.");

		fromLabel.setStyle(BOLD_STYLE);
		subjectLabel.setStyle(BOLD_STYLE);
		boardLabel.setStyle(BOLD_STYLE);
		uuidLabel.setStyle(BOLD_STYLE);
		bodyLabel.setStyle(BOLD_STYLE);

		int row = 0;
		grid.addRow(++row, fromLabel, fromText);
		grid.addRow(++row, subjectLabel, subjectText);
		grid.addRow(++row, boardLabel, boardText);
		grid.addRow(++row, uuidLabel, uuidText);
		grid.addRow(++row, bodyLabel, bodyText);
		grid.add(new javafx.scene.control.Separator(), 0, ++row, 2, 1);
		grid.add(infoLabel, 0, ++row, 2, 1);

		// second column gets any extra width
		ColumnConstraints col1 = new ColumnConstraints();
		ColumnConstraints col2 = new ColumnConstraints();
		col2.setHgrow(Priority.ALWAYS);
		grid.getColumnConstraints().addAll(col1, col2);

		dialog = StyleFactory.getInstance().createDialog();
		dialog.initOwner(ownerWindow);
		dialog.setTitle("Find Messages");
		dialog.setResizable(true);
		dialog.getDialogPane().setContent(grid);

		List<ButtonType> buttons = dialog.getDialogPane().getButtonTypes();
		buttons.add(ButtonType.OK);
		buttons.add(ButtonType.CANCEL);
	}

	public MessageSearchCriteria showAndWait() {
		Optional<ButtonType> result = dialog.showAndWait();
		ButtonType buttonType = ButtonType.CANCEL;
		if (result.isPresent())  {
			buttonType =  result.get();
		}

		if (buttonType != ButtonType.OK) {
			return null;
		}

		MessageSearchCriteria msc = new MessageSearchCriteria();
		if (!fromText.getText().isEmpty()) {
			msc.setFrom(fromText.getText());
		}

		if (!subjectText.getText().isEmpty()) {
			msc.setSubject(subjectText.getText());
		}

		if (!boardText.getText().isEmpty()) {
			msc.setBoard(boardText.getText());
		}

		if (!uuidText.getText().isEmpty()) {
			msc.setUUID(uuidText.getText());
		}

		if (!bodyText.getText().isEmpty()) {
			msc.setBody(bodyText.getText());
		}

		return msc;
	}
}
