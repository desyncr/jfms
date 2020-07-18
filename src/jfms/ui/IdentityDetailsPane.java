package jfms.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import jfms.config.Constants;
import jfms.fms.AddedInfo;
import jfms.fms.DateIndex;
import jfms.fms.FmsManager;
import jfms.fms.RequestType;
import jfms.store.Store;

public class IdentityDetailsPane {
	private static final String UNKNOWN_TEXT = "<unknown>";

	private final GridPane grid;
	private final TextField nameText = new TextField();
	private final TextField sskText = new TextField();
	private final TextArea signatureText = new TextArea();
	private final TextField avatarText = new TextField();
	private final AvatarImage avatar = new AvatarImage(false);
	private final CheckBox disableAvatarCb = new CheckBox();
	private final TextField freesiteText = new TextField();
	private final CheckBox singleUseCb = new CheckBox();
	private final CheckBox publishTrustListCb = new CheckBox();
	private final CheckBox publishBoardListCb = new CheckBox();
	private final TextField addedByText = new TextField();
	private final TextField dateAddedText = new TextField();
	private final TextField lastSeenText = new TextField();
	private final TextField messageCountText = new TextField();

	private int identityId = -1;

	public IdentityDetailsPane() {
		Label nameLabel = new Label("Name");
		nameText.setEditable(false);

		Label sskLabel = new Label("SSK");
		sskText.setEditable(false);

		Label signatureLabel = new Label("Signature");
		GridPane.setValignment(signatureLabel, VPos.TOP);
		signatureText.setPrefRowCount(5);
		signatureText.setWrapText(true);
		signatureText.setEditable(false);

		Label avatarLabel = new Label("Avatar");
		avatarText.setEditable(false);

		Label disableAvatarLabel = new Label("disable Avatar");
		disableAvatarCb.setDisable(true);
		disableAvatarCb.selectedProperty().addListener(
			(ov, oldVal, newVal) -> {
				final Store store = FmsManager.getInstance().getStore();
				store.setAvatarDisabled(identityId, newVal);
			});

		Label freesiteLabel = new Label("Freesite");
		freesiteText.setEditable(false);

		Label singleUseLabel = new Label("Single Use");
		singleUseCb.setDisable(true);

		Label publishTrustListLabel = new Label("Publish Trust List");
		publishTrustListCb.setDisable(true);

		Label publishBoardListLabel = new Label("Publish Board List");
		publishBoardListCb.setDisable(true);

		Label addedByLabel = new Label("Add method");
		addedByText.setEditable(false);

		Label dateAddedLabel = new Label("Date added");
		dateAddedText.setEditable(false);

		Label lastSeenLabel = new Label("Last Seen");
		lastSeenText.setEditable(false);

		Label messageCountLabel = new Label("Messages received");
		messageCountText.setEditable(false);


		grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(2);
		grid.setPadding(new Insets(2, 5, 2, 5));

		int row = 0;
		grid.addRow(row++, nameLabel, nameText);
		grid.addRow(row++, sskLabel, sskText);
		grid.addRow(row++, signatureLabel, signatureText);
		grid.addRow(row++, avatarLabel, avatarText);
		grid.add(avatar.getImageView(), 1, row++);
		grid.addRow(row++, disableAvatarLabel, disableAvatarCb);
		grid.addRow(row++, freesiteLabel, freesiteText);
		grid.addRow(row++, singleUseLabel, singleUseCb);
		grid.addRow(row++, publishTrustListLabel, publishTrustListCb);
		grid.addRow(row++, publishBoardListLabel, publishBoardListCb);
		grid.addRow(row++, addedByLabel, addedByText);
		grid.addRow(row++, dateAddedLabel, dateAddedText);
		grid.addRow(row++, lastSeenLabel, lastSeenText);
		grid.addRow(row++, messageCountLabel, messageCountText);
	}

	public Node getNode() {
		return grid;
	}

	public void updateDetails(int selectedIdentityId) {
		identityId = selectedIdentityId;
		jfms.fms.Identity id = null;
		if (identityId != -1) {
			id = FmsManager.getInstance().getIdentityManager()
					.getIdentity(identityId);
		}

		if (id == null) {
			nameText.setText("");
			sskText.setText("");
			signatureText.setText("");
			avatarText.clear();
			avatar.setIdentityId(identityId);
			disableAvatarCb.setDisable(true);
			disableAvatarCb.setSelected(false);
			freesiteText.setText("");
			singleUseCb.setSelected(false);
			publishTrustListCb.setSelected(false);
			publishBoardListCb.setSelected(false);
			addedByText.clear();
			dateAddedText.clear();
			lastSeenText.clear();
			messageCountText.clear();

			return;
		}

		String name = id.getName();
		if (name == null) {
			name = UNKNOWN_TEXT;
		}
		nameText.setText(name);
		sskText.setText(id.getSsk());
		signatureText.setText(id.getSignature());

		avatarText.setText(id.getAvatar());
		avatar.setIdentityId(identityId);
		disableAvatarCb.setDisable(id.getAvatar() == null);
		final Store store = FmsManager.getInstance().getStore();
		boolean isDisabled = store.isAvatarDisabled(identityId);
		disableAvatarCb.setSelected(isDisabled);

		if (id.getFreesiteEdition() >= 0) {
			StringBuilder str = new StringBuilder("USK");
			str.append(id.getSsk().substring(3));
			str.append("fms/");
			str.append(id.getFreesiteEdition());
			str.append('/');
			freesiteText.setText(str.toString());
		} else {
			freesiteText.setText("");
		}

		singleUseCb.setSelected(id.getSingleUse());
		publishTrustListCb.setSelected(id.getPublishTrustList());
		publishBoardListCb.setSelected(id.getPublishBoardList());

		AddedInfo addedInfo = store.getAddedInfo(identityId);
		addedByText.setText(addedInfo.getAddedByAsString());
		dateAddedText.setText(
				addedInfo.getDateAdded().format(DateTimeFormatter.ISO_LOCAL_DATE));
		DateIndex lastSeenDateIndex = store.getLastRequestDateIndex(identityId, RequestType.IDENTITY);
		LocalDate lastSeen = lastSeenDateIndex.getDate();
		if (lastSeen.equals(Constants.FALLBACK_DATE)) {
			lastSeenText.setText("never");
		} else {
			lastSeenText.setText(lastSeen.format(DateTimeFormatter.ISO_LOCAL_DATE));
		}
		messageCountText.setText(Integer.toString(store.getMessageCount(identityId)));
	}
}
