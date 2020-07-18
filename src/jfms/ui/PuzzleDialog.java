package jfms.ui;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import jfms.config.Constants;
import jfms.fms.FmsManager;
import jfms.fms.IntroductionPuzzle;
import jfms.fms.PuzzleListener;
import jfms.store.Store;

public class PuzzleDialog implements PuzzleListener {
	private static final Logger LOG = Logger.getLogger(PuzzleDialog.class.getName());

	private final int localIdentityId;
	private final Dialog<ButtonType> dialog;
	private final GridPane grid = new GridPane();
	private final LocalDate date;
	private final List<IntroductionPuzzle> puzzles = new ArrayList<>();
	private final List<TextField> solutions = new ArrayList<>();
	private int gridRow = 0;

	public PuzzleDialog(int localIdentityId) {
		this.localIdentityId = localIdentityId;
		this.date = LocalDate.now(ZoneOffset.UTC);

		dialog = StyleFactory.getInstance().createDialog();
		dialog.setTitle("Announce identity");
		dialog.setHeaderText("Please solve some of the following CAPTCHAs\n"
				+ "(images will appear when they are downloaded)");

		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(2, 5, 2, 5));


		ScrollPane sp = new ScrollPane(grid);
		sp.setPrefSize(350, 400);
		dialog.getDialogPane().setContent(sp);

		List<ButtonType> buttons = dialog.getDialogPane().getButtonTypes();
		buttons.add(ButtonType.CANCEL);
		buttons.add(ButtonType.OK);

		Button okButton = (Button)dialog.getDialogPane().lookupButton(ButtonType.OK);
		okButton.setOnAction(new OkHandler());
	}

	private void handlePuzzleAdded(IntroductionPuzzle puzzle,
			String publisher) {

		if (!dialog.isShowing()) {
			return;
		}

		final Image img =
			new Image(new ByteArrayInputStream(puzzle.getData()));
		if (img.isError()) {
			LOG.log(Level.FINE, "failed to load image");
			return;
		}

		if (img.getWidth() > Constants.MAX_CAPTCHA_WIDTH ||
				img.getHeight() > Constants.MAX_CAPTCHA_HEIGHT) {

			LOG.log(Level.FINE, "invalid image dimensons: {0}x{1}",
				new Object[]{img.getWidth(), img.getHeight()});
			return;
		}

		puzzles.add(puzzle);

		final ImageView imageView = new ImageView(img);
		Tooltip.install(imageView, new Tooltip(publisher));

		TextField solution = new TextField();
		solutions.add(solution);

		grid.addRow(gridRow++, imageView, solution);
	}

	private class ErrorHandler implements Runnable {
		final String message;

		public ErrorHandler(String message) {
			this.message = message;
		}

		@Override
		public void run() {
			if (!dialog.isShowing()) {
				return;
			}

			Alert alert = StyleFactory.getInstance().createAlert(
					Alert.AlertType.ERROR);
			alert.setHeaderText("Failed to retrieve introduction puzzles");
			alert.setContentText(message);
			alert.showAndWait();
		}
	}

	private class OkHandler implements EventHandler<ActionEvent> {
		@Override
		public void handle(ActionEvent e) {
			for (int i=0; i<solutions.size(); i++) {
				String solution = solutions.get(i).getText();
				if (!solution.isEmpty()) {
					IntroductionPuzzle puzzle = puzzles.get(i);
					Store store = FmsManager.getInstance().getStore();
					store.saveIdentityIntroduction(localIdentityId, date,
						puzzle.getUuid(), solution);
				}
			}
		}
	}

	public boolean showAndWait() {
		Future<?> future =
			FmsManager.getInstance().startPuzzleThread(date, this);
		if (future == null) {
			LOG.log(Level.WARNING, "Failed to start puzzle thread");
			return false;
		}

		Optional<ButtonType> result = dialog.showAndWait();
		future.cancel(true);

		return result.isPresent() && result.get() == ButtonType.OK;
	}

	@Override
	public void onPuzzleAdded(IntroductionPuzzle puzzle, String publisher) {
		Platform.runLater(() -> handlePuzzleAdded(puzzle, publisher));
	}

	@Override
	public void onError(String message) {
		Platform.runLater(new ErrorHandler(message));
	}
}
