package jfms.ui;

import java.net.URL;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;

public class StyleFactory {
	public static final String STYLESHEET = "/jfms.css";
	private static final StyleFactory instance = new StyleFactory();

	private String stylesheet;

	public static StyleFactory getInstance() {
		return instance;
	}

	private StyleFactory() {
		URL url = StyleFactory.class.getResource(STYLESHEET);
		if (url != null) {
			stylesheet = STYLESHEET;
		}
	}

	public Scene createScene(Parent root) {
		Scene scene = new Scene(root);
		if (stylesheet != null) {
			scene.getStylesheets().add(stylesheet);
		}

		return scene;
	}

	public Scene createScene(Parent root, double width, double height) {
		Scene scene = new Scene(root, width, height);
		if (stylesheet != null) {
			scene.getStylesheets().add(stylesheet);
		}

		return scene;
	}

	public TextInputDialog createTextInputDialog() {
		TextInputDialog dialog = new TextInputDialog();
		if (stylesheet != null) {
			dialog.getDialogPane().getStylesheets().add(stylesheet);
		}

		return dialog;
	}

	public Alert createAlert(Alert.AlertType alertType) {
		Alert dialog = new Alert(alertType);
		if (stylesheet != null) {
			dialog.getDialogPane().getStylesheets().add(stylesheet);
		}

		return dialog;
	}

	public Alert createAlert(Alert.AlertType alertType, String contentText,
			ButtonType... buttons) {

		Alert dialog = new Alert(alertType, contentText, buttons);
		if (stylesheet != null) {
			dialog.getDialogPane().getStylesheets().add(stylesheet);
		}

		return dialog;
	}

	public Alert createAlert(Alert.AlertType alertType, String contentText) {
		Alert dialog = new Alert(alertType, contentText);
		if (stylesheet != null) {
			dialog.getDialogPane().getStylesheets().add(stylesheet);
		}

		return dialog;
	}

	public Dialog<ButtonType> createDialog() {
		Dialog<ButtonType> dialog = new Dialog<>();
		if (stylesheet != null) {
			dialog.getDialogPane().getStylesheets().add(stylesheet);
		}

		return dialog;
	}
}
