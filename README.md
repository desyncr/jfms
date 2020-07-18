jfms
====

jfms is a Java implementation of the Freenet Message System (FMS)] using the
JavaFX toolkit.

It is a graphical application in style of a classic newsreader or email client.

Requirements
------------

Requirements:

* Java 8 (or higher) with JavaFX
* JDBC driver for SQLite (included in the bundle)
* network access to a Freenet node

JavaFX is included in Oracle Java SE 8 to 10. OpenJDK users may need to install
OpenJFX.

Java 11 no longer includes JavaFX. You have to download JavaFX SDK for your
platform from <https://openjfx.io>.

Unfortunately, most distributions do not provide a JDBC driver package.

The easiest (but not safest) way to get a working JDBC driver is to download it
from <https://bitbucket.org/xerial/sqlite-jdbc>. The site provides a JDBC4
driver with precompiled native code for common platforms. Copy the downloaded
JAR file into the lib directory.

Building
--------

A build file for Apache Ant is provided. Run it by executing

	ant dist

Of course you may also use your favorite IDE instead of Ant.

If you get errors, make sure you are using Java 8+ and JavaFX is available.
(The JDBC driver is not required for compiling, only for running jfms.)

To include a JDBC driver in the created JAR file, put the driver in the lib
directory (a JAR file with _jdbc_ in its name) and run:

	ant bundle

If necessary, adapt `jdbc.license` in the `build.xml` before redistributing the
JAR file.

Running
-------

If you have built or downloaded a bundle, you can run jfms by double-clicking
the JAR file or start it from the console:

	java -jar jfms-bundle.jar

To tell Java 11 (or later) to make use of a download OpenJFX SDK, you have
to pass additional arguments:

	java --module-path /path/to/javafx-sdk-11/lib/ --add-modules=javafx.web -jar jfms-bundle.jar

Otherwise, you have to make sure that the JDBC driver is in the classpath:

Start it via Ant:

	ant run

You can also launch it manually:

	java -cp lib/sqlite-jdbc-<version>.jar:jfms.jar jfms.Jfms

On the first run a configuration wizard will pop up. The default settings
should work for most users.

It may take a while until you see messages. Currently, downloading the
initial set of trust lists may take more than one hour. This step should be
necessary only once.

All data will be stored in the working directory:

* `jfms.db3`: SQLite database containing identities and messages
* `jfms.properties`: configuration file
* `jfms.N.log`: log files

License
-------

The source code of jfms is in the public domain (see `UNLICENSE`), but the
icons  are not (see `resources/icons/breeze/COPYING-ICONS` and
`resources/icons/oxygen/COPYING`).
