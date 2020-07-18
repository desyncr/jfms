package jfms.ui;

import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

import jfms.config.Config;
import jfms.fms.FmsManager;
import jfms.fms.LocalIdentity;
import jfms.fms.TrustManager;
import jfms.store.Store;

public class IdentityTrustPane {
	private static final Pattern TRUSTLEVEL_PATTERN = Pattern
		.compile("100|\\p{Digit}?\\p{Digit}");

	private final GridPane grid;
	private final ComboBox<String> trustlistCb = new ComboBox<>();
	private final TextField peerMessageTrustText = new TextField();
	private final TextField peerTrustListTrustText= new TextField();
	private final Spinner<Integer> localMessageTrustSpinner = new Spinner<>();
	private final TextField localTrustListTrustComment = new TextField();
	private final Spinner<Integer> localTrustListTrustSpinner = new Spinner<>();
	private final TextField localMessageTrustComment = new TextField();
	private final Button trustListButton = new Button();
	private final Button trusteeListButton = new Button();
	private final Button applyButton = new Button();

	private Map<Integer, LocalIdentity> localIds;
	private int localIdentityId = -1;
	private int selectedIdentityId = -1;
	private Consumer<jfms.fms.Trust> trustChangedCallback;
	private Consumer<String> trustlistChangedCallback;


	public IdentityTrustPane(boolean showApplyButton) {
		Label trustIdLabel = new Label("Local Trust List");
		trustlistCb.getSelectionModel().selectedItemProperty().addListener(
			(ov, oldVal, newVal) -> {
				localIdentityId = getLocalIdentityId(newVal);
				if (trustlistChangedCallback != null) {
					trustlistChangedCallback.accept(newVal);
				}
				updateTrustFields(selectedIdentityId);
			});

		Label localMessageTrustLabel = new Label("Local Message Trust");
		localMessageTrustSpinner.setMaxWidth(80);
		initTrustSpinner(localMessageTrustSpinner);
		localMessageTrustComment.setTooltip(new Tooltip("Message Trust Comment"));

		Label localTrustListTrustLabel = new Label("Local Trust List Trust");
		localTrustListTrustSpinner.setMaxWidth(80);
		initTrustSpinner(localTrustListTrustSpinner);
		localTrustListTrustComment.setTooltip(new Tooltip("Trust List Trust Comment"));

		Label peerTrustListTrustLabel = new Label("Peer Trust List Trust");
		peerTrustListTrustText.setMaxWidth(80);
		peerTrustListTrustText.setEditable(false);

		Label peerMessageTrustLabel = new Label("Peer Message Trust");
		peerMessageTrustText.setMaxWidth(80);
		peerMessageTrustText.setEditable(false);

		Label trustListLabel = new Label("Trust List");
		trustListButton.setDisable(true);
		trustListButton.setText("Show...");
		trustListButton.setOnAction((ActionEvent e) -> {
			final TrustListWindow window = new TrustListWindow(
					TrustListWindow.Mode.TRUST_LIST, selectedIdentityId);
			window.show(null);
		});

		Label trusteeListLabel = new Label("Trusted by");
		trusteeListButton.setDisable(true);
		trusteeListButton.setText("Show...");
		trusteeListButton.setOnAction((ActionEvent e) -> {
			final TrustListWindow window = new TrustListWindow(
					TrustListWindow.Mode.TRUSTEE_LIST, selectedIdentityId);
			window.show(null);
		});

		applyButton.setText("Save Local Trust Settings");
		applyButton.setDisable(true);
		applyButton.setOnAction(e -> applySettings());


		grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(2);
		grid.setPadding(new Insets(2, 5, 2, 5));

		int row = 0;
		grid.addRow(row++, trustIdLabel, trustlistCb);
		GridPane.setColumnSpan(trustlistCb, 2);
		grid.addRow(row++, localMessageTrustLabel,
				localMessageTrustSpinner, localMessageTrustComment);
		grid.addRow(row++, localTrustListTrustLabel,
				localTrustListTrustSpinner, localTrustListTrustComment);
		grid.addRow(row++, peerMessageTrustLabel, peerMessageTrustText);
		grid.addRow(row++, peerTrustListTrustLabel, peerTrustListTrustText);
		grid.addRow(row++, trustListLabel, trustListButton);
		grid.addRow(row++, trusteeListLabel, trusteeListButton);
		if (showApplyButton) {
			grid.add(applyButton, 2, row++);
		}
	}

	public Node getNode() {
		return grid;
	}

	public void setTrustChangedCallback(Consumer<jfms.fms.Trust> callback) {
		trustChangedCallback = callback;
	}

	public void setTrustlistChangedCallback(Consumer<String> callback) {
		trustlistChangedCallback = callback;
	}

	public void updateTrustCb() {
		final int defaultId = Config.getInstance().getDefaultId();
		final Store store = FmsManager.getInstance().getStore();
		localIds = store.retrieveLocalIdentities();

		trustlistCb.setValue("");
		ObservableList<String> ids = FXCollections.observableArrayList();
		for (Map.Entry<Integer, LocalIdentity> e : localIds.entrySet()) {
			final LocalIdentity id = e.getValue();
			if (defaultId == e.getKey()) {
				trustlistCb.setValue(id.getFullName());
			}
			ids.add(id.getFullName());
		}

		trustlistCb.setItems(ids);
	}

	public void applySettings() {
		if (localIdentityId == -1) {
			return;
		}

		jfms.fms.Trust trust = new jfms.fms.Trust();
		Integer messageTrust = getTrustSpinnerValue(localMessageTrustSpinner);
		if (messageTrust != null) {
			trust.setMessageTrustLevel(messageTrust);
		}
		trust.setMessageTrustComment(localMessageTrustComment.getText());

		Integer trustListTrust = getTrustSpinnerValue(localTrustListTrustSpinner);
		if (trustListTrust != null) {
			trust.setTrustListTrustLevel(trustListTrust);
		}
		trust.setTrustListTrustComment(localTrustListTrustComment.getText());

		FmsManager.getInstance().getTrustManager()
			.updateLocalTrust(localIdentityId, selectedIdentityId, trust);

		if (trustChangedCallback != null) {
			trustChangedCallback.accept(trust);
		}
	}

	public String getSelectedLocalIdentity() {
		return trustlistCb.getValue();
	}

	public void updateTrustFields(int identityId) {
		selectedIdentityId = identityId;

		final boolean disabled = localIdentityId == -1 ||
			selectedIdentityId == -1;

		jfms.fms.Trust trust = null;
		if (!disabled) {
			final Store store = FmsManager.getInstance().getStore();
			trust = store.getLocalTrust(localIdentityId, selectedIdentityId);
		}
		if (trust == null) {
			trust = new jfms.fms.Trust();
		}

		int peerMessageTrust;
		int peerTrustListTrust;
		if (selectedIdentityId != -1) {
			TrustManager trustManager =
				FmsManager.getInstance().getTrustManager();
			peerMessageTrust = trustManager
				.getPeerMessageTrust(selectedIdentityId);
			peerTrustListTrust = trustManager
				.getPeerTrustListTrust(selectedIdentityId);

			jfms.fms.Identity id = FmsManager.getInstance()
				.getIdentityManager().getIdentity(selectedIdentityId);
			boolean publishTrustList = false;
			if (id != null) {
				publishTrustList = id.getPublishTrustList();
			}

			trustListButton.setDisable(!publishTrustList);
			trusteeListButton.setDisable(false);
		} else {
			peerMessageTrust = -1;
			peerTrustListTrust = -1;
			trustListButton.setDisable(true);
			trusteeListButton.setDisable(true);
		}

		String peerMessageTrustString;
		if (peerMessageTrust != -1) {
			peerMessageTrustString = Integer.toString(peerMessageTrust);
		} else {
			peerMessageTrustString = "";
		}

		String peerTrustListTrustString;
		if (peerTrustListTrust >= 0) {
			peerTrustListTrustString = Integer.toString(peerTrustListTrust);
		} else {
			peerTrustListTrustString = "";
		}

		int localMessageTrustLvl = trust.getMessageTrustLevel();
		String localMessageTrustString;
		if (localMessageTrustLvl != -1) {
			localMessageTrustString = Integer.toString(localMessageTrustLvl);
		} else {
			localMessageTrustString = "";
		}

		int localTrustListTrustLvl = trust.getTrustListTrustLevel();
		String localTrustListTrustString;
		if (localTrustListTrustLvl != -1) {
			localTrustListTrustString = Integer.toString(localTrustListTrustLvl);
		} else {
			localTrustListTrustString = "";
		}

		localMessageTrustSpinner.getEditor().setText(localMessageTrustString);
		localMessageTrustComment.setText(trust.getMessageTrustComment());
		updateTrustSpinner(localMessageTrustSpinner);

		localTrustListTrustSpinner.getEditor().setText(localTrustListTrustString);
		localTrustListTrustComment.setText(trust.getTrustListTrustComment());
		updateTrustSpinner(localTrustListTrustSpinner);

		peerMessageTrustText.setText(peerMessageTrustString);
		peerTrustListTrustText.setText(peerTrustListTrustString);

		applyButton.setDisable(disabled);
	}

	public int getLocalIdentityId(String localIdentityName) {
		int storeId= -1;
		for (Map.Entry<Integer, LocalIdentity> e : localIds.entrySet()) {
			final LocalIdentity id = e.getValue();
			if (id.getFullName().equals(localIdentityName)) {
				storeId = e.getKey();
			}
		}

		return storeId;
	}

	private void initTrustSpinner(Spinner<Integer> spinner) {
		spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,100));
		spinner.setEditable(true);
		spinner.getEditor().focusedProperty().addListener(
			(ObservableValue<? extends Boolean> observable,
			 Boolean oldValue, Boolean newValue) -> {
				if (!newValue) {
					updateTrustSpinner(spinner);
				}
			});
	}

	private void updateTrustSpinner(Spinner<Integer> spinner) {
		final String trustStr = spinner.getEditor().getText();

		boolean trustValid = false;
		if (!trustStr.isEmpty()) {
			Matcher m = TRUSTLEVEL_PATTERN.matcher(trustStr);
			trustValid = m.matches();
		}

		if (trustValid) {
			// force update
			spinner.increment(0);
		} else {
			spinner.getEditor().clear();
		}
	}

	private Integer getTrustSpinnerValue(Spinner<Integer> spinner) {
		Integer trustValue = null;
		if (!spinner.getEditor().getText().isEmpty()) {
			trustValue = spinner.getValue();
		}

		return trustValue;
	}
}
