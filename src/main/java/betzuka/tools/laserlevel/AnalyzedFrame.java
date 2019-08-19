package betzuka.tools.laserlevel;

import java.awt.image.BufferedImage;

public class AnalyzedFrame {
	private final double [] intensityCurve;
	private final double [] gaussianFit;
	private final BufferedImage img;
	private final int width, height;
	
	public AnalyzedFrame(int width, int height, double[] intensityCurve, double[] gaussianFit, BufferedImage img) {
		this.intensityCurve = intensityCurve;
		this.gaussianFit = gaussianFit;
		this.img = img;
		this.width = width;
		this.height = height;
	}
	
	public double[] getIntensityCurve() {
		return intensityCurve;
	}
	public double[] getGaussianFit() {
		return gaussianFit;
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
		return gaussianFit!=null;
	}
	
	public double getMaxima() {
		if (!hasFit()) {
			return Double.NaN;
		}
		return gaussianFit[1];
	}
}
