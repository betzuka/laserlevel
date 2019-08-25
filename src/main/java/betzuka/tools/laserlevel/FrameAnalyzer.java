package betzuka.tools.laserlevel;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

public class FrameAnalyzer {
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
		
		double [] gaussianFit = fitGausian(intensityCurve);
		
		return new AnalyzedFrame(img.getWidth(), img.getHeight(), intensityCurve, gaussianFit, img);
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
	
	private static double [] fitGausian(double [] curve) {
		try {
			GaussianCurveFitter fitter = GaussianCurveFitter.create().withMaxIterations(1000);
			WeightedObservedPoints pts = new WeightedObservedPoints();
			for (int i=0;i<curve.length;i++) {
				pts.add(i, curve[i]);
			}
			double [] fit = fitter.fit(pts.toList());
			return fit;
		} catch (Exception e) {
			//we will get an exception if we cannot fit a guassian within max iterations
			//this occurs when the laser is not hitting the sensor
			return null;
		}
	}
}