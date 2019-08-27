package betzuka.tools.laserlevel;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.regression.SimpleRegression;

public class LinearMeasurement {

	private List<Sample> samples = new ArrayList<>();
	
	public static class Sample {
		private final double x, y;
		private double linY;
		private double linYError;
		private double shim;
		private double scrape;
		
		private Sample(double x, double y) {
			this.x = x;
			this.y = y;
		}
		
		private void update(SimpleRegression linearFit) {
			linY = linearFit.predict(x);
			linYError = y - linY;
		}

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}

		public double getLinY() {
			return linY;
		}

		public double getLinYError() {
			return linYError;
		}

		public double getShim() {
			return shim;
		}

		public double getScrape() {
			return scrape;
		}
		
	}
	
	public void add(double y) {
		add(samples.size(), y);
	}
	
	public void add(double x, double y) {
		samples.add(new Sample(x, y));
		recalc();
	}
	
	public boolean isEmpty() {
		return samples.isEmpty();
	}
	
	public void clear() {
		samples.clear();
	}

	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("X\tMEASURED\tLINE\tRESIDUAL\tSCRAPE\tSHIM\n");
		for (Sample s : samples) {
			sb.append(String.format("%.4f\t%.4f\t%.4f\t%.4f\t%.4f\t%.4f\n", s.x, s.y, s.linY, s.linYError, s.scrape, s.shim));
		}
		return sb.toString();
	}
	
		
	public List<Sample> getSamples() {
		return samples;
	}

	private void recalc() {
		if (samples.size()>=3) {
			SimpleRegression linearFit = new SimpleRegression(true);
			for (Sample s : samples) {
				linearFit.addData(s.x, s.y);
			}
			double minYError = Double.POSITIVE_INFINITY, maxYError = Double.NEGATIVE_INFINITY;
			for (Sample s : samples) {
				s.update(linearFit);
				if (s.linYError > maxYError) {
					maxYError = s.linYError;
				} 
				if (s.linYError < minYError) {
					minYError = s.linYError;
				}
			}
			
			for (Sample s : samples) {
				//make highest point zero for shimming, we are going to shim up all the low points to this height
				s.shim = maxYError - s.linYError;
				//make lowest point zero for scraping, we are going to scrape off all the high areas
				s.scrape = s.linYError - minYError;
			}
			
		}
	}
	
}
