package jfms.ui;

import java.time.LocalDate;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import jfms.config.Config;
import jfms.fms.FmsManager;
import jfms.store.Store;

public class MaintenanceWindow {
	private Stage stage;
	private final GridPane grid = new GridPane();
	private final Button removeAllButton;
	private final Label removeAllCountText = new Label();
	private final Button removeInactiveButton;
	private final Label removeInactiveCountText = new Label();

	public MaintenanceWindow() {
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));

		Label infoLabel = new Label(
		"Identities from the database that were created more than 20 days "
		+ "ago and you haven't received messages of can be removed.\n"
		+ "Identities will be readded if they appear in a trust list.\n"
		+ "Removal is only allowed in offline mode.");
		infoLabel.setWrapText(true);
		infoLabel.setPrefWidth(400);

		Label removeAllLabel = new Label("Remove all identities that never sent a message");
		removeAllButton = new Button("Remove");
		removeAllButton.setOnAction(e -> {
			final LocalDate today = LocalDate.now();
			final Store store = FmsManager.getInstance().getStore();
			store.removeIdentities(today, true);
		});

		Label removeInactiveLabel = new Label("Remove only identities that were not seen recently");
		removeInactiveButton = new Button("Remove");
		removeAllButton.setOnAction(e -> {
			final LocalDate today = LocalDate.now();
			final Store store = FmsManager.getInstance().getStore();
			store.removeIdentities(today, false);
		});

		int row = 0;
		grid.addRow(++row, removeAllLabel, removeAllCountText, removeAllButton);
		grid.addRow(++row, removeInactiveLabel, removeInactiveCountText,
				removeInactiveButton);
		grid.add(new Separator(), 0, ++row, 3, 1);
		grid.add(infoLabel, 0, ++row, 3, 1);
	}

	public void show(Window ownerWindow) {
		if (stage == null) {
			Scene scene = StyleFactory.getInstance().createScene(grid);

			stage = new Stage();
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.initOwner(ownerWindow);
			stage.setTitle("Database Maintenance");
			stage.setScene(scene);
		}

		boolean removeDisabled = !Config.getInstance().getOffline();
		removeAllButton.setDisable(removeDisabled);
		removeInactiveButton.setDisable(removeDisabled);

		final LocalDate today = LocalDate.now();
		final Store store = FmsManager.getInstance().getStore();
		int removeInactiveCount = store.countRemovableIdentities(today, true);
		removeInactiveCountText.setText(Integer.toString(removeInactiveCount));

		int removeAllCount = store.countRemovableIdentities(today, false);
		removeAllCountText.setText(Integer.toString(removeAllCount));

		stage.show();
	}
}
