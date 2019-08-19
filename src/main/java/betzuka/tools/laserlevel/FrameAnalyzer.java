package betzuka.tools.laserlevel;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class FrameAnalyzer {
	private VideoCapture cam;
	private Mat mat;
	private int width, height;
	public FrameAnalyzer(VideoCapture cam) {
		this.cam = cam;
		this.mat = new Mat();
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
		cam.read(mat);
		//convert to greyscale
		Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
		//rotate the image since we want the to measure in the 'long' direction
		Core.rotate(mat, mat, Core.ROTATE_90_CLOCKWISE);
		
		//get the pixels
		byte [] pixels = new byte[mat.rows() * mat.cols()];
		mat.get(0, 0, pixels);
		
		double [] intensityCurve = calcCurve(mat.rows(), mat.cols(), pixels);
		
		double [] gaussianFit = fitGausian(intensityCurve);
		
		//create an image of the camera frame for display purposes
		BufferedImage img = new BufferedImage(mat.cols(),mat.rows(), BufferedImage.TYPE_BYTE_GRAY);
		System.arraycopy(pixels, 0, ((DataBufferByte) img.getRaster().getDataBuffer()).getData(), 0, pixels.length);
		
		return new AnalyzedFrame(mat.cols(), mat.rows(), intensityCurve, gaussianFit, img);
	}
			
	private static double [] calcCurve(int rows, int cols, byte [] pixels) {
		
		int [] rowIntensity = new int[rows];
		for (int i=0;i<cols;i++) {
			for (int j=0;j<rows;j++) {
				rowIntensity[j] += pixels[j*cols + i] & 0xFF;
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
		for (int i=1;i<curve.length-1;i++) {
			smooth[i] = (curve[i-1] + curve[i] + curve[i+1]) / 3d;
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