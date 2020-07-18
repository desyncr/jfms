package jfms.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.Node;
import javafx.scene.control.TextArea;

class MessageBodyTextView implements MessageBodyView {
	private static final Logger LOG = Logger.getLogger(MessageBodyTextView.class.getName());

	private final TextArea textArea = new TextArea();
	private String body;
	private String signature;
	private boolean muteQuotes = false;
	private boolean showSignature = false;


	public MessageBodyTextView() {
		textArea.setPrefColumnCount(80);
		textArea.setPrefRowCount(25);
		textArea.setWrapText(true);
		textArea.setEditable(false);
	}

	@Override
	public Node getNode() {
		return textArea;
	}

	@Override
	public void setText(String body, String signature) {
		this.body = body;
		this.signature = signature;

		renderMessage();
	}

	@Override
	public void setMuteQuotes(boolean muteQuotes) {
		this.muteQuotes = muteQuotes;
		renderMessage();
	}

	@Override
	public void setShowEmoticons(boolean showEmoticons) {
	}

	@Override
	public void setShowSignature(boolean showSignature) {
		this.showSignature = showSignature;
		renderMessage();
	}

	@Override
	public void setDetectLinks(boolean detectLinks) {
	}

	@Override
	public void setHighlight(String text) {
	}

	@Override
	public void setUseMonospaceFont(boolean useMonospaceFont) {
		if (useMonospaceFont) {
			textArea.setStyle("-fx-font-family: Monospace");
		} else {
			textArea.setStyle(null);
		}

		renderMessage();
	}

	private void renderMessage() {
		if (!muteQuotes) {
			textArea.setText(body);
		} else {
			try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
				StringBuilder str = new StringBuilder();
				boolean firstLine = true;
				boolean inQuote = false;

				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith(">")) {
						if (!inQuote) {
							if (!firstLine) {
								str.append('\n');
							}
							str.append("> [quoted text muted]");
						}

						inQuote = true;
					} else {
						inQuote = false;

						if (!firstLine) {
							str.append('\n');
						}
						str.append(line);
					}

					firstLine = false;
				}

				textArea.setText(str.toString());
			} catch (IOException e) {
				LOG.log(Level.WARNING, "failed to render message", e);
			}
		}

		if (showSignature && signature != null) {
			textArea.appendText("\n\n-- \n" + signature);
		}
	}
}
