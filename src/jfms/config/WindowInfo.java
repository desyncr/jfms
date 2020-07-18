package jfms.config;

public class WindowInfo {
	private final int width;
	private final int height;
	private final boolean maximized;

	public WindowInfo(int width, int height, boolean maximized) {
		this.width = width;
		this.height = height;
		this.maximized = maximized;
	}

	public WindowInfo(String winSpec) {
		int w = Constants.DEFAULT_WINDOW_WIDTH;
		int h  = Constants.DEFAULT_WINDOW_HEIGHT;

		String spec = winSpec;
		if (spec.endsWith("M")) {
			spec = spec.substring(0, spec.length()-1);
			this.maximized = true;
		} else {
			this.maximized = false;
		}

		String[] fields = spec.split("x");
		if (fields.length == 2) {
			try {
				w = Integer.parseInt(fields[0]);
				h = Integer.parseInt(fields[1]);
			} catch (NumberFormatException e) {
			}

			w = Math.max(Constants.MIN_WINDOW_WIDTH, w);
			h = Math.max(Constants.MIN_WINDOW_HEIGHT, h);
		}

		this.width = w;
		this.height = h;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(width);
		str.append('x');
		str.append(height);
		if (maximized) {
			str.append("M");
		}

		return str.toString();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public boolean isMaximized() {
		return maximized;
	}
}
