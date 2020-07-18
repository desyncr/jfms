package jfms.captcha;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javax.imageio.ImageIO;

import jfms.fms.IntroductionPuzzle;

/**
 * Simple CAPTCHA generator.
 * This is a Java port of SimpleCaptcha in the reference implementation. The
 * generated CAPTCHA consists of random letters obfuscated by random lines and
 * arcs. Random colors are used and noise is added in the last step.
 */
public class SimpleCaptcha {
	private static final Logger LOG = Logger.getLogger(SimpleCaptcha.class.getName());

	private static final int WIDTH = 110;
	private static final int HEIGHT= 50;
	private static final int LETTER_COUNT = 5;

	private final Random random = new Random();

	/**
	 * Generate a CAPTCHA with the specified image file format.
	 * Must be called from FX application thread.
	 * @param formatName file format of the image to generate
	 * @return IntroductionPuzzle containing the CAPTCHA.
	 * UUID needs to be set by caller
	 */
	public IntroductionPuzzle generate(String formatName) {
		String text = generateText();
		byte[] data = generateImage(text, formatName);
		if (data == null) {
			LOG.log(Level.WARNING, "failed to generate CAPTCHA");
			return null;
		}

		IntroductionPuzzle puzzle = new IntroductionPuzzle();
		puzzle.setType("captcha");
		puzzle.setMimeType("image/" + formatName);
		puzzle.setSolution(text);
		puzzle.setData(data);

		return puzzle;
	}

	private byte[] generateImage(String text, String formatName) {
		final Canvas canvas = new Canvas(WIDTH, HEIGHT);
		final GraphicsContext gc = canvas.getGraphicsContext2D();

		// add random lines
		int lineCount = 10 + random.nextInt(10);
		for (int i=0; i<lineCount; i++) {
			gc.setStroke(randomColor(100, 150));
			drawRandomLine(gc);
		}

		// add random arcs
		int arcCount = 10 + random.nextInt(10);
		for (int i=0; i<arcCount; i++) {
			gc.setStroke(randomColor(100, 150));
			drawRandomArc(gc);
		}

		// draw a random line in the color of each letter
		final Color[] letterColors = new Color[LETTER_COUNT];
		for (int i=0; i<LETTER_COUNT; i++) {
			letterColors[i] = randomColor(0, 150);

			gc.setStroke(letterColors[i]);
			drawRandomLine(gc);
		}

		// draw individual letters
		gc.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
		for (int i=0; i<LETTER_COUNT; i++) {
			gc.setFill(letterColors[i]);
			double x = 5 + i*20 + random.nextDouble()*10;
			double y = 20 + random.nextDouble()*20;
			gc.fillText(text.substring(i, i+1), x, y);
		}

		WritableImage snapshot = canvas.snapshot(new SnapshotParameters(), null);
		BufferedImage img = SwingFXUtils.fromFXImage(snapshot, null);
		BufferedImage finalImg = new BufferedImage(WIDTH, HEIGHT,
				BufferedImage.TYPE_INT_RGB);
		// remove alpha channel and add noise
		for (int x=0; x<WIDTH; x++) {
			for (int y=0; y<HEIGHT; y++) {
				finalImg.setRGB(x,y, addNoise(img.getRGB(x,y)));
			}
		}

		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			boolean writerFound = ImageIO.write(finalImg, formatName, bos);
			if (!writerFound) {
				LOG.log(Level.WARNING, "No image writer found");
				return null;
			}

			return bos.toByteArray();
		} catch (IOException e) {
			LOG.log(Level.WARNING, "failed to generate CAPTCHA", e);
			return null;
		}
	}

	private String generateText() {
		// numbers, upper and lower case characters
		// exclude look-alike characters 0, O, 1, I, l
		final String allowedChars =
			"23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

		StringBuilder str = new StringBuilder();
		for (int i=0; i<LETTER_COUNT; i++) {
			int index = random.nextInt(allowedChars.length());
			str.append(allowedChars.charAt(index));
		}

		return str.toString();
	}

	private int addNoise(int argb) {
		// discard alpha channel
		int out = 0;
		for (int i=0; i<3; i++) {
			int oldVal = (argb>>16) & 0xff;
			int newVal = Math.max(0, Math.min(oldVal + random.nextInt(20) - 10, 0xff));

			out <<= 8;
			out += newVal;

			argb <<= 8;
		}

		return out;
	}

	private Color randomColor(int start, int maxOffset) {
		int r = start + random.nextInt(maxOffset);
		int g = start + random.nextInt(maxOffset);
		int b = start + random.nextInt(maxOffset);

		return Color.rgb(r, g, b);
	}

	private void drawRandomLine(GraphicsContext gc) {
		double startx = random.nextDouble() * WIDTH;
		double starty = random.nextDouble() * HEIGHT;
		double endx = random.nextDouble() * WIDTH;
		double endy = random.nextDouble() * HEIGHT;

		gc.strokeLine(startx, starty, endx, endy);
	}

	private void drawRandomArc(GraphicsContext gc) {
		double x = random.nextDouble() * WIDTH;
		double y = random.nextDouble() * HEIGHT;
		double diameter = random.nextDouble() * HEIGHT;
		double startAngle = random.nextDouble() * 360;
		double arcExtent = random.nextDouble() * 360;

		gc.strokeArc(x, y, diameter, diameter, startAngle,
				arcExtent, ArcType.OPEN);
	}
}
