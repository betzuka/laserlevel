package betzuka.tools.laserlevel;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import net.java.dev.designgridlayout.DesignGridLayout;

public class PlaneWizard extends JPanel {

	
	private int numX, numY, numPoints;
	
	private double xSpacing, ySpacing;
	private double gridWidth, gridHeight;
		
	private class Sample {
		private String label;
		private int gridX, gridY;
		private double x, y, z;
		private Sample(String label, double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.label = label;
		}

		
	}
	
	private List<Sample> samples;
	
	
	
	private void init() {
		this.numPoints = numX*numY;
		this.samples = new ArrayList<PlaneWizard.Sample>();
		this.gridWidth = (numX-1)*xSpacing;
		this.gridHeight = (numY-1)*ySpacing;
		
		int sampleNum = 1;
		for (int y=0;y<numY; y++) {
			for (int x=0;x<numX;x++) {
				this.samples.add(new Sample("P" + sampleNum++, x*xSpacing, y*ySpacing, 0));
			}
		}
	}
	
	public PlaneWizard(int numX, int numY, double xSpacing, double ySpacing) {
		this.numX = numX;
		this.numY = numY;
		this.xSpacing = xSpacing;
		this.ySpacing = ySpacing;
		init();
		
		//add some fields
		JPanel offsets = new JPanel();
		
		DesignGridLayout layout = new DesignGridLayout(offsets);
		
		layout.row().grid(new JLabel("NumX")).add(new JTextField("" + numX))
		.grid(new JLabel("NumX")).add(new JTextField("" + numX))
		.grid(new JLabel("NumX")).add(new JTextField("" + numX))
		.grid(new JLabel("NumX")).add(new JTextField("" + numX));
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		/*
		offsets.add(new JLabel("NumX"));
		offsets.add(new JTextField("" + numX));
		offsets.add(new JLabel("NumY"));
		offsets.add(new JTextField("" + numY));
		offsets.add(new JLabel("SpaceX"));
		offsets.add(new JTextField("" + xSpacing));
		offsets.add(new JLabel("SpaceY"));
		offsets.add(new JTextField("" + ySpacing));
		
		
		SpringUtilities.makeGrid(offsets, 3, 2, 5, 5, 5, 5);
			*/	
		add(offsets);
		add(new GridPanel());
	}
	
	


	private int samplePtr = 0;
	
	public void recordSample(double z) {
		samples.get(samplePtr).z = z;
		samplePtr++;
		if (samplePtr==numPoints) {
			samplePtr = 0;
		}
	}
	
	public int getCurrentSampleIdx() {
		return samplePtr;
	}
	
	public List<Sample> getSamples() {
		return samples;
	}

	public void establishPlane(Sample s1, Sample s2, Sample s3) { 
		
	}
	
	private class GridPanel extends JPanel {
		@Override
	    public void paintComponent(Graphics g) {
			super.paintComponent(g);
			FontRenderContext frc =  new FontRenderContext(null, true, true);

			int margin = 50;
			int width = getWidth()-2*margin;
			int height = getHeight()-2*margin;
			
			double xScale = width/gridWidth;
			double yScale = height/gridHeight;
			
			int radius = 15;
			g.setColor(Color.RED);
			for (Sample s : samples) {
				int x = (int)(s.x * xScale)  + margin;
				int y = (int)(s.y * yScale)  + margin;
				
				Rectangle2D r = g.getFont().getStringBounds(s.label, frc);
				
				g.setColor(Color.BLACK);
				g.drawString(s.label, x - (int)(r.getWidth()/2d), y + (int)(r.getHeight()/2d));
				
				
				g.drawOval(x - radius, y- radius, 2*radius, 2*radius);
			}
		}
	}
	
	
	
	
	
}
