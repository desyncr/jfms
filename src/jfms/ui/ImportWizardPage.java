package jfms.ui;

import java.io.File;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import jfms.config.Constants;
import jfms.store.FmsImport;

public class ImportWizardPage implements WizardPage {
	private final WizardSettings settings;
	private final RadioButton freshRb = new RadioButton();
	private final RadioButton importRb = new RadioButton();

	public ImportWizardPage(WizardSettings settings) {
		this.settings = settings;
	}

	@Override
	public String getHeader() {
		return "Import Settings";
	}

	@Override
	public Node getContent() {
		ToggleGroup group = new ToggleGroup();

		freshRb.setText("Fresh installation (don't import anything)");
		freshRb.setToggleGroup(group);
		freshRb.setSelected(true);

		importRb.setText("Import existing FMS database");
		importRb.setToggleGroup(group);
		importRb.setDisable(!settings.getImportAllowed());

		Label locationLabel = new Label("Data will be stored in " +
				System.getProperty("user.dir"));
		Label infoLabel = new Label("Import disabled: jfms.d3b already exists");


		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));

		grid.add(freshRb, 0, 0);
		grid.add(importRb, 0, 1);
		grid.add(new Separator(), 0, 2);
		grid.add(locationLabel, 0, 3);
		if (!settings.getImportAllowed()) {
			grid.add(infoLabel, 0, 4);
		}
		return grid;
	}

	@Override
	public WizardPage nextPage() {
		if (settings.getFatalError()) {
			return null;
		}

		return new FreenetSettingsPage(settings);
	}

	@Override
	public boolean commit() {
		if (freshRb.isSelected()) {
			return true;
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Import FMS database");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("SQLite Databases", "*.db3"),
				new ExtensionFilter("All Files", "*.*"));

		File selectedFile = fileChooser.showOpenDialog(null);
		if (selectedFile == null) {
			return false;
		}

		if (!runImport(selectedFile)) {
			Alert importDialog = StyleFactory.getInstance()
					.createAlert(Alert.AlertType.WARNING);
			importDialog.setContentText("FMS Import failed.\n"
					+ Constants.SEE_LOGS_TEXT);
			importDialog.setResizable(true);
			importDialog.showAndWait();

			settings.setFatalError(true);
		}

		return true;
	}

	@Override
	public boolean hasNext() {
		return false;
	}

	private boolean runImport(File fmsDbFile) {
		FmsImport fmsImport = new FmsImport(fmsDbFile.toString());
		return fmsImport.startImport();
	}
}
