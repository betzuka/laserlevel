package betzuka.tools.laserlevel;

public class Settings {
	
	private boolean invertGreyscale;
	private double umPerPixel = 1;
	private int smoothingFactor = 20;
	private int samplesToAverage = 10;
	private int model = 4;
	
	public boolean isInvertGreyscale() {
		return invertGreyscale;
	}

	public void setInvertGreyscale(boolean invertGreyscale) {
		this.invertGreyscale = invertGreyscale;
	}

	public double getUmPerPixel() {
		return umPerPixel;
	}

	public void setUmPerPixel(double umPerPixel) {
		this.umPerPixel = umPerPixel;
	}

	public int getSmoothingFactor() {
		return smoothingFactor;
	}

	public void setSmoothingFactor(int smoothingFactor) {
		this.smoothingFactor = smoothingFactor;
	}


	public int getSamplesToAverage() {
		return samplesToAverage;
	}

	public void setSamplesToAverage(int samplesToAverage) {
		this.samplesToAverage = samplesToAverage;
	}

	public int getModel() {
		return model;
	}

	public void setModel(int model) {
		this.model = model;
	}


}
