package betzuka.tools.laserlevel;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

public class FrameAnalyzer {
	private Set<FrameListener> listeners = new HashSet<FrameListener>();
	private Camera cam;
//	private Mat mat;
	private int width, height;
	private final Settings settings;
	public FrameAnalyzer(Camera cam, Settings settings) {
		this.cam = cam;
		this.settings = settings;
	//	this.mat = new Mat();
		AnalyzedFrame firstFrame = analyzeNextFrame();
		this.width = firstFrame.getWidth();
		this.height = firstFrame.getHeight();
		
	}
	
	public void addListener(FrameListener l) {
		listeners.add(l);
	}
	
	private void notifyListeners(AnalyzedFrame frame) {
		Set<FrameListener> toRemove = null;
		for (FrameListener l : listeners) {
			if (l.onFrame(frame)) {
				if (toRemove==null) {
					toRemove = new HashSet<FrameListener>();
				}
				toRemove.add(l);
			}
		}
		if (toRemove!=null) {
			listeners.removeAll(toRemove);
		}
	}
	
	public void dispose() {
		cam.dispose();
	}
	
	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public AnalyzedFrame analyzeNextFrame() {
		//read frame from camera
		BufferedImage img = cam.nextFrame();
		
		byte [] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
		double [] intensityCurve = calcCurve(img.getHeight(), img.getWidth() , pixels, settings.getSmoothingFactor(), settings.isInvertGreyscale());
		
		double maxima = findMaxima(intensityCurve);
		
		AnalyzedFrame frame = new AnalyzedFrame(img.getWidth(), img.getHeight(), intensityCurve, maxima, img);
		notifyListeners(frame);
		return frame;
	}
			

	private static double [] calcCurve(int rows, int cols, byte [] pixels, int smoothingFactor, boolean invert) {
		
		int [] rowIntensity = new int[rows];
		for (int i=0;i<cols;i++) {
			for (int j=0;j<rows;j++) {
				rowIntensity[j] += (invert ? pixels[j*cols + i]^0xFF : pixels[j*cols + i]) & 0xFF;
			}
		}
			
		int max = 0;
		int min = Integer.MAX_VALUE;
		for (int i=0;i<rows;i++) {
			if (rowIntensity[i]>max) {
				max = rowIntensity[i];
			}
			if (rowIntensity[i]<min) {
				min = rowIntensity[i];
			}
		}
		int range = max-min;
		double [] curve = new double[rows];
		//shift and scale row intensities 0->1
		for (int i=0;i<rows;i++) {
			curve[i] = (rowIntensity[i] - min)/(double)range;
		}
		
		double [] smooth = new double[curve.length];
		
		//smooth with nearest neighbour
		for (int i=smoothingFactor;i<curve.length-smoothingFactor;i++) {
			
			for (int j=-smoothingFactor;j<=smoothingFactor;j++) {
				smooth[i] += curve[i+j];
			}
			
			smooth[i] /= (double)(2*smoothingFactor + 1);
		}
		
		return smooth;
	}
	
	private double findMaxima(double [] curve) {
		if (settings.getModel()==0) {
			return fitGausian(curve);
		} else if (settings.getModel()==1) {
			return fitMaxLocalEnergy(curve);
		} else if (settings.getModel()==2) {
			return fitParabolic(curve);
		} else if (settings.getModel()==3) {
			return fitMaxLocalEnergyThenGaussian(curve);
		} else if (settings.getModel()==4) {
			return fitMaxLocalEnergyThenParabolic(curve);
		}
		throw new IllegalStateException();
	}
	
	private static double fitGausian(double [] curve) {
		try {
			GaussianCurveFitter fitter = GaussianCurveFitter.create().withMaxIterations(1000);
			WeightedObservedPoints pts = new WeightedObservedPoints();
			for (int i=0;i<curve.length;i++) {
				pts.add(i, curve[i]);
			}
			double [] fit = fitter.fit(pts.toList());
			return fit[1];
		} catch (Exception e) {
			//we will get an exception if we cannot fit a guassian within max iterations
			//this occurs when the laser is not hitting the sensor
			return Double.NaN;
		}
	}
	
	public static int fitMaxLocalEnergy(double [] curve) {
		int window = 10;
		double maxEnergy = Double.NEGATIVE_INFINITY;
		int maxIdx = 0;
		for (int i=window;i<curve.length-window;i++) {
			double energy = 0;
			for (int j=i-window;j<=i+window;j++) {
				energy += curve[j];
			}
			if (energy>maxEnergy) {
				maxEnergy = energy;
				maxIdx = i;
			}
		}
		return maxIdx;
	}
	
	public static double fitParabolic(double [] curve) {
		
		try {
			PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2).withMaxIterations(1000);
			WeightedObservedPoints pts = new WeightedObservedPoints();
			for (int i=0;i<curve.length;i++) {
				pts.add(i, curve[i]);
			}
			double [] fit = fitter.fit(pts.toList());
			
			//fit contains degree 2 polynomial (parabola) parameters in order of increasing degree, e.g. y = p0 + p1x + p2x^2
			//derivative d/dx y = 0 + p1 + 2*p2x
			//slope is zero at maxima
			//p1 + 2*p2x = 0
			//x = -p1 / (2*p2)
			return -fit[1]/(2*fit[2]);
			
		} catch (Exception e) {
			//we will get an exception if we cannot fit a guassian within max iterations
			//this occurs when the laser is not hitting the sensor
			return Double.NaN;
		}
		
	}
	
	public static double fitMaxLocalEnergyThenParabolic(double [] curve) {
		
		int localMax = fitMaxLocalEnergy(curve);
		
		int window = 10;
		
		double [] subCurve = new double[window*2+1];
		
		int windowStart = localMax-window;
		
		for (int j=0;j<subCurve.length;j++) {
			int curveIdx = j+windowStart;
			if (j<0 || j>=curve.length) {
				return Double.NaN;
			}
			subCurve[j] = curve[curveIdx];
		}
		
		double fit = fitParabolic(subCurve);
		
		if (!Double.isNaN(fit)) {
			fit += windowStart;
		}
		
		return fit;
		
		
	}
	
	public static double fitMaxLocalEnergyThenGaussian(double [] curve) {
		
		int localMax = fitMaxLocalEnergy(curve);
		
		int window = 10;
		
	
		
		double [] subCurve = new double[window*2+1];
		
		int windowStart = localMax-window;
		
		
		for (int j=0;j<subCurve.length;j++) {
			int curveIdx = j+windowStart;
			if (j<0 || j>=curve.length) {
				return Double.NaN;
			}
			subCurve[j] = curve[curveIdx];
		}
		
		double fit = fitGausian(subCurve);
		
		if (!Double.isNaN(fit)) {
			fit += windowStart;
		}
		
		return fit;
		
		
	}
	
}