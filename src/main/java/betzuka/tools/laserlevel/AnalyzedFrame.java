package betzuka.tools.laserlevel;

import java.awt.image.BufferedImage;

public class AnalyzedFrame {
	private final double [] intensityCurve;
	private final double maxima;
	private final BufferedImage img;
	private final int width, height;
	
	public AnalyzedFrame(int width, int height, double[] intensityCurve, double maxima, BufferedImage img) {
		this.intensityCurve = intensityCurve;
		this.maxima = maxima;
		this.img = img;
		this.width = width;
		this.height = height;
	}
	
	public double[] getIntensityCurve() {
		return intensityCurve;
	}
	public BufferedImage getImg() {
		return img;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
	
	public boolean hasFit() {
		return !Double.isNaN(maxima);
	}
	
	public double getMaxima() {
		if (!hasFit()) {
			return Double.NaN;
		}
		return maxima;
	}
}
