package jfms.captcha;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

import jfms.fms.IntroductionPuzzle;

public class CaptchaGenerator {
	private static final Logger LOG = Logger.getLogger(CaptchaGenerator.class.getName());

	private final Lock lock = new ReentrantLock();
	private final Condition cond = lock.newCondition();
	private volatile IntroductionPuzzle puzzle;

	private class PuzzleTask implements Runnable {
		@Override
		public void run() {
			lock.lock();
			try {
				SimpleCaptcha captcha = new SimpleCaptcha();
				puzzle = captcha.generate("bmp");

				cond.signal();
			} finally {
				lock.unlock();
			}
		}
	}

	public synchronized IntroductionPuzzle generatePuzzle() {
		LOG.log(Level.FINEST, "Requesting CAPTCHA generation");

		puzzle = null;
		Platform.runLater(new PuzzleTask());

		lock.lock();
		try {
			while (puzzle == null) {
				cond.await();
			}
		} catch (InterruptedException e) {
			LOG.log(Level.WARNING, "CaptchaGenerator was interrupted", e);
		} finally {
			lock.unlock();
		}

		LOG.log(Level.FINEST, "CAPTCHA generation finished");

		return puzzle;
	}
}
