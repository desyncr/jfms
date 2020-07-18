jfms FAQ
========

Unfortunately, there is no documentation for jfms. I hope this FAQ helps
with some common issues.

## Why does jfms store data in my home directory?

You probably started jfms by double clicking the JAR file.  The
recommended way to start jfms is from the directory where the JAR file
is stored.

You can create a simple script that can be run from any directory. On
linux, you can use the following script to start jfms from any directory
(assuming the jar file is stored in the folder jfms in your home
directory).

	#/bin/sh
	cd $HOME/jfms
	java -jar jfms-bundle.jar

If you need to modify java arguments, they can be easily integrated into the script.

## How do I run jfms with Java 11+?

Go to <https://openjfx.io>, download the SDK from your platform and
unpack it.  You can start jfms with the following command line (of
course, you have to change `path/to` to the actual location):

	java --module-path /path/to/javafx-sdk-11/lib --add-modules=javafx.web -jar jfms-bundle.jar

In Ubuntu 18.10, you can use the OpenJFX package instead of downloading the SDK:

	apt-get install openjfx
	java --module-path /usr/share/openjfx/lib --add-modules=javafx.web -jar jfms-bundle.jar

## How do I reduce memory usage?

To limit memory usage, you can use the -Xmx switch.

For instance, the following command line limits heap memory usage to 256MB:

	java -Xmx256m -jar jfms-bundle.jar


## How do I change font sizes, colors, etc?

Layout cannot be changed directly but JavaFX supports style sheets.

The JavaFX CSS reference guide is available at
<https://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html>.

To use a stylesheet, create the file `jfms.css` in the folder where the
JAR file is located. You can find various existing themes on the
internet.

This example (not meant to be pretty) uses sets the font to Arial 14 and
the base color to blue.

	.root {
		-fx-font-family: "Arial";
		-fx-font-size: 14px;
		-fx-base: blue;
	}


For WebView, there is an additional CSS that has to be located in the
current directory.  If you start jfms via script (see the first
question), both paths will be the same. WebView uses standard CSS.

To use a WebView stylesheet, create the file `webview.css`. You can
start by modifying the default stylesheet:

	pre { white-space: pre-wrap; }
	.quote { color: green; }
	.signature { color: gray; }
	.highlight { background-color: yellow; }

`pre` is used for monospace rendering, `.quote` is used for quoted
messages, `.signature` for optional signatures displayed at the end of
the message, and `.highlight` for the search patterns in search results.

## How can I use the old JavaFX theme?

Modena is the default theme for JavaFX 8. Use the following command to switch
to the Caspian theme that shipped as default in JavaFX 2.x:

	java -Djavafx.userAgentStylesheetUrl=CASPIAN -jar jfms-bundle.jar

## Does jfms support high DPI displays?

You can use stylesheets to adapt the user interface (see the previous
question).

`jfms.css`:

	.root {
		-fx-font-size: 2em;
	}

`webview.css`:

	body { font-size: 2em; }
	pre { white-space: pre-wrap; }
	.quote { color: green; }
	.signature { color: gray; }
	.highlight { background-color: yellow; }

Theoretically, high DPI icons should be used automatically. If that
doesn't work, you can explicitly enable them in Settings &#8594;
Appearance &#8594; Themes.

## How do I use custom icons?

jfms looks for local icon themes in icons/&lt;theme> in the working
directory. Local themes can be selected via Settings &#8594; Appearance
&#8594; Themes.

It is easiest to start with one of the existing themes. They can be
found in resources/icons in the source tarball. jfms supports PNG files only, the SVG files are ignored.

## Why are "Show Emoticons as graphics" and "Detect Links" not working?

These feature are only available when WebView is enabled (Settings &#8594; Appearance &#8594; Messages).

WebView also enables different colors for quotes/signatures and
highlights the search pattern in search results.

## What's flow-based reputation?

Starting from version 0.8 peer trustlist trust calculation is based on
[Simone, A., Škorić, B., Zannone, N.: Flow-based reputation: more than
just ranking](/CHK@FjQePmwrDf77wsrbWVbs7Q5jwDI3JqVYeyf6EkKfGWw,09FZhpjvaLk6G~8fOceym1F08scJ1Ca5IueAPnAUWPs,AAMC--8/simo-skor-zann-12-IJITDM.pdf).

To match the nature of the FMS network NULL trusts are excluded by default
which is a major deviation from the algorithm in the paper. The result should
be similar to the pre-0.8 algorithm in jfms, but unfortunately we will lose
some properties of the new algorithm, e.g., a single identity will dominate the
trust levels of identities with few ratings.

Peer message trust is unchanged and still calculated as described on the FMS
site.

There are two related config options:

### Settings &#8594; Trust &#8594; Exclude NULL trust from trustlists trust calculation

To use the algorithm as described in the paper, disable this setting. Since the
vast majority of ratings in FMS is NULL (i.e., no trust level is assigned), a
neutral level of 50 will be assumed.

In practice this setting is not very useful because nearly all peer trust
levels will end up at 50.

### Settings &#8594; Trust &#8594; Weight of indirect trustlist trusts

This settings corresponds to α (multipled by 100) in the paper and indicates
the weight of indirect evidence.

α=0 completely disables peer trust and only locally trusted trustlists are
considered. α=100 is somewhat similiar to the pre-0.8 algorithm. The default
is α=50.

Here is a simplified example:

<table>
<tr>
	<th>α</th>
	<th>local trust</th>
	<th>peer trust</th>
	<th>final trust</th>
</tr>
<tr>
	<td>0</td>
	<td>50</td>
	<td>100</td>
	<td>50</td>
</tr>
<tr>
	<td>50</td>
	<td>50</td>
	<td>100</td>
	<td>75</td>
</tr>
<tr>
	<td>100</td>
	<td>50</td>
	<td>100</td>
	<td>100</td>
</tr>
</table>
