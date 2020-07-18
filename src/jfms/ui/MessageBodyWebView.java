package jfms.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

class MessageBodyWebView implements MessageBodyView {
	private static final Logger LOG = Logger.getLogger(MessageBodyWebView.class.getName());
	// ? (query) is not allowed
	private static final Pattern URI_PATTERN = Pattern.compile(
			"(CHK|SSK|USK)@[\\p{Alnum}~-]{43},[\\p{Alnum}~-]{43},[\\p{Alnum}~-]{7}(/[\\p{Alnum}\\-#._~%!$&'()*+,;=:@]*)*");

	private final WebView webView = new WebView();
	private final WebEngine webEngine;
	private final boolean useCustomStyleSheet;
	private String body;
	private String signature;
	private boolean muteQuotes = false;
	private boolean showSignature = false;
	private boolean useMonospaceFont = false;
	private boolean showEmoticons = false;
	private boolean detectLinks = false;
	private String highlight;

	private static void encodeHTML(StringBuilder out, String str)
	{
		for (int i = 0; i < str.length(); i++) {
			final char c = str.charAt(i);
			if (c > 0x7f || c=='"' || c=='&' || c=='<' || c=='>') {
				out.append("&#");
				out.append((int)c);
				out.append(';');
			} else {
				out.append(c);
			}
		}
	}

	public MessageBodyWebView() {
		webEngine = webView.getEngine();
		StringBuilder str = new StringBuilder(System.getProperty("user.dir"));
		str.append(File.separatorChar);
		str.append("webview");

		useCustomStyleSheet = new File("webview.css").exists();
		if (useCustomStyleSheet) {
			webEngine.setUserStyleSheetLocation("file:webview.css");
		}


		webEngine.setUserDataDirectory(new File(str.toString()));
		webEngine.setJavaScriptEnabled(false);
	}

	@Override
	public Node getNode() {
		return webView;
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
		this.showEmoticons = showEmoticons;
		renderMessage();
	}

	@Override
	public void setShowSignature(boolean showSignature) {
		this.showSignature = showSignature;
		renderMessage();
	}

	@Override
	public void setDetectLinks(boolean detectLinks) {
		this.detectLinks = detectLinks;
		renderMessage();
	}

	@Override
	public void setHighlight(String highlight) {
		this.highlight = highlight;
	}

	@Override
	public void setUseMonospaceFont(boolean useMonospaceFont) {
		this.useMonospaceFont = useMonospaceFont;
		renderMessage();
	}

	private void renderMessage() {
		if (body == null) {
			webEngine.loadContent("<html/>");
			return;
		}

		StringBuilder str = new StringBuilder();
		str.append("<html>\n");
		str.append("<head>\n");
		if (!useCustomStyleSheet) {
			str.append("<style type=\"text/css\">\n");
			str.append("body { margin-bottom: 25px; margin-right: 25px;}\n");
			str.append("pre { white-space: pre-wrap; }\n");
			str.append(".quote { color: green; }\n");
			str.append(".signature { color: gray; }\n");
			str.append(".highlight { background-color: yellow; }\n");
			str.append("</style>\n");
		}
		str.append("</head>\n");
		str.append("<body>\n");

		if (useMonospaceFont) {
			str.append("<pre>");
		}

		str.append(renderBody());

		if (showSignature && signature != null) {
			addNewline(str);
			addNewline(str);
			str.append("<span class=\"signature\">");
			str.append("--&nbsp;");

			try (BufferedReader reader = new BufferedReader(new StringReader(signature))) {
				String line;
				while ((line = reader.readLine()) != null) {
					addNewline(str);
					encodeHTML(str, line);
				}
			} catch (IOException e) {
				LOG.log(Level.WARNING, "failed to render message", e);
			}

			str.append("</span>");
		}

		if (useMonospaceFont) {
			str.append("</pre>");
		}

		str.append("\n</body>\n");
		str.append("</html>");

		final String html = str.toString();
		webEngine.loadContent(html);
	}

	private String renderBody() {
		StringBuilder str = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new StringReader(body))) {
			boolean firstLine = true;
			boolean inQuote = false;
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith(">")) {
					if (!inQuote) {
						str.append("<span class=\"quote\">");
						if (muteQuotes) {
							if (!firstLine) {
								addNewline(str);
							}
							str.append("&gt; [quoted text muted]");
						}
					}
					inQuote = true;
				} else {
					if (inQuote) {
						str.append("</span>");
					}
					inQuote = false;
				}

				if (!muteQuotes || !inQuote) {
					if (!firstLine) {
						addNewline(str);
					}
					encodeHTML(str, line);
				}

				firstLine = false;
			}

			if (inQuote) {
				str.append("</span>");
			}
		} catch (IOException e) {
			LOG.log(Level.WARNING, "failed to render message", e);
		}



		String html = str.toString();

		if (showEmoticons) {
			html = Emoticons.getInstance().replaceByImgTags(html);
		}

		if (detectLinks) {
			html = replaceLinks(html);
		}

		if (highlight != null && !highlight.isEmpty()) {
			html = replaceHighlights(html);
		}

		return html;
	}

	private String replaceLinks(String input) {
		StringBuffer output = new StringBuffer();
		Matcher m = URI_PATTERN.matcher(input);

		while (m.find()) {
			final String group = Matcher.quoteReplacement(m.group());

			StringBuilder str = new StringBuilder();
			str.append("<a href=\"");
			str.append(group);
			str.append("\" title=\"Right-click to copy link\">");
			str.append(group);
			str.append("</a>");

			m.appendReplacement(output, str.toString());
		}
		m.appendTail(output);

		return output.toString();
	}

	private String replaceHighlights(String input) {
		StringBuilder escaped = new StringBuilder();
		encodeHTML(escaped, highlight);
		Pattern pattern = Pattern.compile(escaped.toString(),
				Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
		StringBuffer output = new StringBuffer();
		Matcher m = pattern.matcher(input);

		while (m.find()) {
			if (!isText(input, m.start())) {
				// skip if we are not in text-only node (e.g., within a tag)
				continue;
			}

			final String group = Matcher.quoteReplacement(m.group());

			StringBuilder str = new StringBuilder();
			str.append("<span class=\"highlight\">");
			str.append(group);
			str.append("</span>");

			m.appendReplacement(output, str.toString());
		}
		m.appendTail(output);

		return output.toString();
	}

	private boolean isText(String input, int pos) {
		int bracketCount = 0;
		for (int i=0; i<pos; i++) {
			if (input.charAt(i) == '<') {
				bracketCount++;
			} else if (input.charAt(i) == '>') {
				bracketCount--;
			}
		}

		return (bracketCount % 2) == 0;
	}

	private void addNewline(StringBuilder out) {
		out.append('\n');
		if (!useMonospaceFont) {
			out.append("<br/>");
		}
	}
}
