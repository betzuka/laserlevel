package betzuka.tools.laserlevel;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

public class Utils {
	public static double[][] interpolateCurve(int numPoints, double[] x, double [] y) {
		if (x.length<3) {
			return new double [][] {x, y};
		}
		SplineInterpolator asi = new SplineInterpolator();
		PolynomialSplineFunction psi = asi.interpolate(x, y);
		double [] newX = new double[numPoints];
		double [] newY = new double[numPoints];
		
		double minX = x[0];
		double maxX = x[x.length-1];
		
		double range = maxX - minX;
		double step = range/(double)(numPoints-1);
				
		for (int i=0;i<numPoints;i++) {
			newX[i] = minX + i*step;
			newY[i] = newX[i]<=maxX ? psi.value(newX[i]) : y[y.length-1];
		}
		return new double [][] {newX, newY};
	}
}
