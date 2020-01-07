package betzuka.tools.laserlevel;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EtchedBorder;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;

import betzuka.tools.laserlevel.camera.SaxosCamera;


public class LaserLevel extends JFrame implements FrameListener {

	
	private FrameAnalyzer analyser;
	private AnalyzedFrame frame;
	private Measurement measurement;
	private Settings settings = new Settings();
	private JDialog settingsDialog;
	private boolean initialized = false;
	private Object initMutex = new Object();
	private LinearMeasurement linear = new LinearMeasurement();
	
	private static class Measurement {
		private Double zero;

		public Double getZero() {
			return zero;
		}

		public void setZero(Double zero) {
			this.zero = zero;
		}

	}

	public double pixelToUM(double pixel) {
		return pixel * settings.getUmPerPixel();
	}
	
	
	@Override
	public boolean onFrame(AnalyzedFrame frame) {
		this.frame = frame;
		repaint();
		return false;
	}
	
	public void init(String camName) {
		synchronized(initMutex) {
			initialized = false;
			getContentPane().removeAll();
			if (analyser!=null) {
				analyser.dispose();
			}
			analyser = new FrameAnalyzer(new SaxosCamera(camName, settings), settings);
			analyser.addListener(this);
			measurement = new Measurement();
	
			setLayout(new BorderLayout());
			Dimension dim = new Dimension(analyser.getWidth(), analyser.getHeight());
			Dimension minDim =  new Dimension(analyser.getWidth()/5, analyser.getHeight()/5);
			JPanel videos = new JPanel(new GridLayout(1, 0));
			JPanel curvePanel = new CurvePanel();
			//curvePanel.setMaximumSize(dim);
		//	curvePanel.setMinimumSize(minDim);
			curvePanel.setPreferredSize(dim);
			JPanel vidPanel = new VidPanel();
			vidPanel.setPreferredSize(dim);
		//	vidPanel.setMaximumSize(dim);
			//vidPanel.setMinimumSize(minDim);
			videos.add(vidPanel);
			videos.add(curvePanel);
			//videos.setMaximumSize(new Dimension(analyser.getWidth()*2, analyser.getHeight()));
			//JPanel content = new JPanel();
		//	content.setLayout(new BoxLayout(content, BoxLayout.LINE_AXIS));
			
			
			
			JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			
			split.add(videos);
			
			JTabbedPane tabs = new JTabbedPane();
			
			MeasurementPanel measurementPanel = new MeasurementPanel();
			tabs.addTab("Linear", measurementPanel);
			
			
			
			split.add(tabs);
		//	measurementPanel.setPreferredSize(dim);
			
			//content.add(measurementPanel);
			//content.add(videos);
			//content.add(measurementPanel);
			add(split, BorderLayout.CENTER);
			//add(chartPanel, BorderLayout.SOUTH);
		
			//add(new MeasurementPanel(), BorderLayout.SOUTH);
			this.revalidate();
			
			initialized = true;
		}
	}
	
	public void updateAnalyser() {
		synchronized(initMutex) {
			if (initialized) {
				analyser.analyzeNextFrame();
			}
		}
	}
	
	public LaserLevel() {
		settingsDialog = new BeanDialog(this, "Preferences", settings);
		
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("Options");
		
		JMenuItem cams = new JMenuItem("Select camera");
		cams.addActionListener((e) -> {
			selectCam();
		});
		
		JMenuItem prefs = new JMenuItem("Preferences");
		prefs.addActionListener((e) -> {
			settingsDialog.setVisible(true);
		});
		
		menu.add(cams);
		menu.add(prefs);
		menuBar.add(menu);
		
		this.setJMenuBar(menuBar);
		
	}

	public void selectCam() {
		String [] camNames = null;
		while ((camNames = SaxosCamera.listCams()).length==0) {
			JOptionPane.showMessageDialog(this, "Please attach a webcam", "No cam found", JOptionPane.WARNING_MESSAGE);
		}
		
		
		String chosenCam = null;
		
		if (camNames.length>1) {
		
			while ((chosenCam = (String)JOptionPane.showInputDialog(this, "Select a camera", "Camera", JOptionPane.PLAIN_MESSAGE, null, camNames, camNames[0]))==null) {}
		} else {
			chosenCam = camNames[0];
		}
		
		init(chosenCam);
	}
	

	private class ChartPanel extends JPanel {
		
		private XYChart chart;
		private XChartPanel<XYChart> chartPanel;
		private boolean absolute = true;
		private ChartPanel() {
			setLayout(new BorderLayout());
			chart = new XYChartBuilder().width(400).height(150).build();
			chart.getStyler().setLegendVisible(false);
			chartPanel = new XChartPanel<>(chart);
			add(chartPanel, BorderLayout.CENTER);
			
			JRadioButton abs = new JRadioButton("Absolute");
			abs.setSelected(true);
			JRadioButton residual = new JRadioButton("Residual");
			ButtonGroup grp = new ButtonGroup();
			grp.add(abs);
			grp.add(residual);
			JPanel grpPanel = new JPanel();
			grpPanel.add(abs);
			grpPanel.add(residual);
			add(grpPanel, BorderLayout.SOUTH);
			
			ActionListener listener = (e) -> {
				absolute = e.getActionCommand().equals("Absolute");
				updateSeries();
			};
			
			abs.addActionListener(listener);;
			residual.addActionListener(listener);
			
		}
		
		private void updateSeries() {
			chart.removeSeries("Measurements");
			chart.removeSeries("LinearFit");
			chart.removeSeries("Residuals");
			chart.removeSeries("Zero");
			if (!linear.isEmpty()) {
				List<LinearMeasurement.Sample> samples = linear.getSamples();
				double [] x = new double[samples.size()];
				double [] y = new double[samples.size()];
				double [] linY = new double[samples.size()];
				double [] linYError = new double[samples.size()];
				
				for (int i=0;i<samples.size();i++) {
					LinearMeasurement.Sample s = samples.get(i);
					x[i] = s.getX();
					y[i] = s.getY();
					linY[i] = s.getLinY();
					linYError[i] = s.getLinYError();
				}
				
				
				
				
				if (absolute) {
					double [][] interpolation = Utils.interpolateCurve(chart.getWidth(), x, y);
					chart.addSeries("LinearFit", x, linY).setMarker(SeriesMarkers.NONE).setLineColor(Color.BLUE);
					chart.addSeries("Measurements", interpolation[0], interpolation[1]).setMarker(SeriesMarkers.NONE).setLineColor(Color.RED);
				} else {
					//draw a line at zero
					chart.addSeries("Zero", new double [] {x[0], x[x.length-1]}, new double [] {0,0}).setMarker(SeriesMarkers.NONE).setLineColor(Color.BLUE);
					double [][] residuals = Utils.interpolateCurve(chart.getWidth(), x, linYError);
					chart.addSeries("Residuals", residuals[0], residuals[1]).setMarker(SeriesMarkers.NONE).setLineColor(Color.RED);
				}
								
				chartPanel.repaint();
			}
		}
		
	}

	private class MeasurementPanel extends JPanel {
		private JLabel error;
		private ChartPanel chartPanel;
		
		private MeasurementPanel() {
		//	setLayout(new GridLayout(2, 1));
	//		setLayout(new BorderLayout());
			
			setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Measure"));
			String [] colNames = {"X", "Measured", "Residual", "Scrape", "Shim"};
			
			AbstractTableModel model = new AbstractTableModel() {
				
				
				private String formatValue(double v) {
					return String.format("%.4f", v);
				}
				
				@Override
				public int getRowCount() {
					return linear.getSamples().size();
				}

				@Override
				public int getColumnCount() {
					return 5;
				}

				@Override
				public Object getValueAt(int rowIndex, int columnIndex) {
					LinearMeasurement.Sample sample = linear.getSamples().get(rowIndex);
					switch(columnIndex) {
					case 0: return formatValue(sample.getX());
					case 1: return formatValue(sample.getY());
					case 2: return formatValue(sample.getLinYError());
					case 3: return formatValue(sample.getScrape());
					case 4: return formatValue(sample.getShim());
					}
					return null;
				}

				@Override
				public String getColumnName(int column) {
					return colNames[column];
				}

				@Override
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return false;
				}
				
			};
			JTable measurementsTable = new JTable(model);
			measurementsTable.setShowGrid(true);
			measurementsTable.setRowSelectionAllowed(false);
			JScrollPane tablePane = new JScrollPane(measurementsTable); 
			JLabel progress = new JLabel(" ");
			JButton zeroButton = new JButton("Zero");
			
			AbstractAction zeroAction = new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (frame!=null) {
						//make sure we really want to do this
						if (linear.isEmpty() ||  JOptionPane.YES_OPTION==JOptionPane.showConfirmDialog(LaserLevel.this, "Re-zero ? Are you sure, all measurements will be lost!", "Warning", JOptionPane.OK_CANCEL_OPTION)) {
							zeroButton.setEnabled(false);
							analyser.addListener(new SampleAverager(settings.getSamplesToAverage(), new SampleAverager.Tracker() {
								@Override
								public void onUpdate(int samplesTaken) {
									progress.setText(samplesTaken + "/" + settings.getSamplesToAverage());
									progress.repaint();
								}
								
								@Override
								public void onComplete(double avg) {
									
									measurement.setZero(avg);
									linear.clear();
									progress.setText("");
									zeroButton.setEnabled(true);
									model.fireTableDataChanged();
									
								}
							}));
						}
						
					} else {
						measurement.setZero(null);
					}
				}
			};
			
			zeroButton.addActionListener(zeroAction);
			
			zeroButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0), "Z");
			zeroButton.getActionMap().put("Z", zeroAction);
			
			JButton measure = new JButton("Measure");
			
			progress.setPreferredSize(new Dimension(50, (int)progress.getPreferredSize().getHeight()));
			
			AbstractAction measureAction = new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (frame!=null && frame.hasFit() && measurement.getZero()!=null) {
						measure.setEnabled(false);
						analyser.addListener(new SampleAverager(settings.getSamplesToAverage(), new SampleAverager.Tracker() {
							@Override
							public void onUpdate(int samplesTaken) {
								progress.setText(samplesTaken + "/" + settings.getSamplesToAverage());
								progress.repaint();
							}
							
							@Override
							public void onComplete(double avg) {
								SwingUtilities.invokeLater(() -> {
								linear.add(pixelToUM(avg-measurement.getZero()));
								progress.setText("");
								measure.setEnabled(true);
								model.fireTableDataChanged();
								chartPanel.updateSeries();
								});
							}
						}));
					}
				}
			};
			
			measure.addActionListener(measureAction);
			
			measure.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "M");
			measure.getActionMap().put("M", measureAction);
						
			JButton clear = new JButton("Clear");
			clear.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (!linear.isEmpty() && JOptionPane.YES_OPTION==JOptionPane.showConfirmDialog(LaserLevel.this, "Are you sure, all measurements will be lost!", "Warning", JOptionPane.OK_CANCEL_OPTION)) {
						linear.clear();
						model.fireTableDataChanged();
						chartPanel.updateSeries();
					}
				}
			});
			
			
			JButton copyToClip = new JButton("Copy to clipboard");
			copyToClip.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (!linear.isEmpty()) {
						Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(linear.toString()), null);
					}
				}
			});
			
			
			error = new JLabel("Not zero'd");
			error.setFont(error.getFont().deriveFont(24f));
			error.setPreferredSize(new Dimension(150, (int)error.getPreferredSize().getHeight()));
			
			
			JPanel top = new JPanel();
			top.setLayout(new BoxLayout(top, BoxLayout.PAGE_AXIS));
			
			JPanel north = new JPanel();
			north.add(error);
			north.add(zeroButton);
			north.add(measure);
			north.add(progress);
			//add(north, BorderLayout.NORTH);
			top.add(north);
			
			//add(tablePane, BorderLayout.CENTER);
			top.add(tablePane);
			
			JPanel south = new JPanel();
			south.add(clear);
			south.add(copyToClip);
			
			top.add(south);
			
			chartPanel = new ChartPanel();
			
			JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			
			split.add(top);
			split.add(chartPanel);
			
			//south.add(chartPanel);
			//add(chartPanel);
			setLayout(new BorderLayout());
			add(split, BorderLayout.CENTER);
			//add(south, BorderLayout.SOUTH);
		}

		private Double takeMeasurementNow() {
			if (frame!=null && frame.hasFit() && measurement.getZero()!=null) {
				return pixelToUM(frame.getMaxima()-measurement.getZero());
			}
			return null;
		}
		
		@Override
	    public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Double m = takeMeasurementNow();
			if (m==null) {
				error.setText("");
			} else {
				error.setText(String.format("%.4f", m));
			}
		}
	}

	private class VidPanel extends JPanel {
		@Override
	    public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (frame!=null) {
				g.drawImage(frame.getImg(), 0, 0, getWidth(), getHeight(), null);
			}
		}
	}

	private class CurvePanel extends JPanel {
		@Override
	    public void paintComponent(Graphics g) {
			super.paintComponent(g);
			
			
			
			
			
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			if (frame!=null) {
				//create a new image to draw on
				BufferedImage buf = new BufferedImage(analyser.getWidth(), analyser.getHeight(), BufferedImage.TYPE_INT_RGB);
				
				Graphics2D bg = buf.createGraphics();
				bg.setColor(Color.BLACK);
				bg.fillRect(0, 0, buf.getWidth(), buf.getHeight());
				bg.setColor(Color.GREEN);
				double [] curve = frame.getIntensityCurve();
				for (int i=0;i<curve.length;i++) {
					bg.drawLine(0, i, (int) Math.round(curve[i]*buf.getWidth()), i);
				}
				g.drawImage(buf, 0, 0, getWidth(), getHeight(), null);
				
				bg.dispose();
				if (frame.hasFit()) {
					g.setColor(Color.RED);
					int maxAt = (int)Math.round(frame.getMaxima()/(double)analyser.getHeight() * getHeight());
					g.drawLine(0, maxAt, getWidth(), maxAt);
				}
				if (measurement.getZero()!=null) {
					g.setColor(Color.BLUE);
					int maxAt = (int)Math.round(measurement.getZero()/(double)analyser.getHeight() * getHeight());
					g.drawLine(0, maxAt, getWidth(), maxAt);
				}
			}
		}
	}


	public static void main(String [] args) throws Exception {
		// UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		 for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if ("Nimbus".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		            break;
		        }
		    }
		LaserLevel m = new LaserLevel();
		m.setSize(1024, 768);
		m.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		m.setVisible(true);
		m.selectCam();
		m.pack();
		while (true) {
			m.updateAnalyser();
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {}
		}
	}

	

}
