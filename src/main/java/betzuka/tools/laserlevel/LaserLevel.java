package betzuka.tools.laserlevel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EtchedBorder;
import javax.swing.table.AbstractTableModel;

import betzuka.tools.laserlevel.camera.SaxosCamera;


public class LaserLevel extends JFrame implements FrameListener {

	
	private FrameAnalyzer analyser;
	private AnalyzedFrame frame;
	private Measurement measurement;
	private Settings settings = new Settings();
	private JDialog settingsDialog;
	private boolean initialized = false;
	private Object initMutex = new Object();
	
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
			JPanel content = new JPanel(new FlowLayout());
			JPanel curvePanel = new CurvePanel();
			curvePanel.setPreferredSize(dim);
			JPanel vidPanel = new VidPanel();
			vidPanel.setPreferredSize(dim);
			content.add(vidPanel);
			content.add(curvePanel);
			
			MeasurementPanel measurementPanel = new MeasurementPanel();
			measurementPanel.setPreferredSize(dim);
			
			content.add(measurementPanel);
			
			add(content, BorderLayout.CENTER);
		
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
	


	private class MeasurementPanel extends JPanel {
		private JLabel error;
		private List<String> measurements = new ArrayList<String>();
		
		private MeasurementPanel() {
		//	setLayout(new GridLayout(2, 1));
			
			setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Measure"));
			
			
			AbstractTableModel model = new AbstractTableModel() {
				private String [] colNames = {"Num", "Measurement"};
				
				@Override
				public int getRowCount() {
					return measurements.size();
				}

				@Override
				public int getColumnCount() {
					return 2;
				}

				@Override
				public Object getValueAt(int rowIndex, int columnIndex) {
					if (columnIndex==0) {
						return rowIndex + 1;
					} else {
						return measurements.get(rowIndex);
					}
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
			zeroButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (frame!=null) {
						//make sure we really want to do this
						if (measurements.isEmpty() ||  JOptionPane.YES_OPTION==JOptionPane.showConfirmDialog(LaserLevel.this, "Re-zero ? Are you sure, all measurements will be lost!", "Warning", JOptionPane.OK_CANCEL_OPTION)) {
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
									measurements.clear();
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
			});
			
			
			JButton measure = new JButton("Measure");
			
			progress.setPreferredSize(new Dimension(50, (int)progress.getPreferredSize().getHeight()));
			measure.addActionListener(new ActionListener() {
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
								measurements.add(String.format("%.4f", pixelToUM(avg-measurement.getZero())));
								progress.setText("");
								measure.setEnabled(true);
								model.fireTableDataChanged();
								
							}
						}));
					}
					
				}
			});
			
			JButton clear = new JButton("Clear");
			clear.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (!measurements.isEmpty() && JOptionPane.YES_OPTION==JOptionPane.showConfirmDialog(LaserLevel.this, "Are you sure, all measurements will be lost!", "Warning", JOptionPane.OK_CANCEL_OPTION)) {
						measurements.clear();
						model.fireTableDataChanged();
					}
				}
			});
			
			
			JButton copyToClip = new JButton("Copy to clipboard");
			copyToClip.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (!measurements.isEmpty()) {
						StringBuilder sb = new StringBuilder();
						sb.append("Num\tMeasurement\n");
						for (int i=0;i<measurements.size();i++) {
							sb.append(i+1);
							sb.append("\t");
							sb.append(measurements.get(i));
							sb.append("\n");
						}
						Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
					}
				}
			});
			
			
			error = new JLabel("Not zero'd");
			error.setFont(error.getFont().deriveFont(24f));
			error.setPreferredSize(new Dimension(150, (int)error.getPreferredSize().getHeight()));
			
			setLayout(new BorderLayout());
			
			JPanel north = new JPanel();
			north.add(error);
			north.add(zeroButton);
			north.add(measure);
			north.add(progress);
			add(north, BorderLayout.NORTH);
			
			add(tablePane, BorderLayout.CENTER);
			
			JPanel south = new JPanel();
			south.add(clear);
			south.add(copyToClip);
			
			add(south, BorderLayout.SOUTH);
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
				g.drawImage(frame.getImg(), 0, 0, null);
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
				g.setColor(Color.GREEN);
				double [] curve = frame.getIntensityCurve();
				for (int i=0;i<curve.length;i++) {
					g.drawLine(0, i, (int) Math.round(curve[i]*getWidth()), i);
				}
				if (frame.hasFit()) {
					g.setColor(Color.RED);
					int maxAt = (int)Math.round(frame.getMaxima());
					g.drawLine(0, maxAt, getWidth(), maxAt);
				}
				if (measurement.getZero()!=null) {
					g.setColor(Color.BLUE);
					int maxAt = (int)Math.round(measurement.getZero());
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
		m.setSize(1920, 1024);
		m.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		m.setVisible(true);
		m.selectCam();
		
		while (true) {
			m.updateAnalyser();
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) {}
		}
	}

	

}
