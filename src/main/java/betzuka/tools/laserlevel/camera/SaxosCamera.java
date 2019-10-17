package betzuka.tools.laserlevel.camera;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import com.github.sarxos.webcam.WebcamResolution;

import betzuka.tools.laserlevel.Camera;
import betzuka.tools.laserlevel.Settings;

public class SaxosCamera implements Camera {
	private Settings settings;
	
	private Webcam cam;
	
	public SaxosCamera(String camName, Settings settings) {
		this.settings = settings;
		
		Dimension[] nonStandardResolutions = new Dimension[] {
			WebcamResolution.PAL.getSize(),
			WebcamResolution.HD.getSize(),
			new Dimension(1920, 1080),
			new Dimension(1280, 720),
			new Dimension(800,600),
		};
		
		cam = Webcam.getWebcamByName(camName);
		cam.setCustomViewSizes(nonStandardResolutions);
		cam.setViewSize(new Dimension(settings.getFrameWidth(), settings.getFrameHeight()));
		cam.open();
	}

	public void dispose() {
		cam.close();
	}
	
	public static String [] listCams() {
		List<Webcam> cams = Webcam.getWebcams();
		String [] names = new String[cams.size()];
		for (int i=0;i<names.length;i++) {
			names[i] = cams.get(i).getName();
		}
		return names;
	}
	
	
	@Override
	public BufferedImage nextFrame() {
		BufferedImage raw = cam.getImage();
		
		//convert to greyscale
		BufferedImage gray = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		Graphics g = gray.getGraphics();
		g.drawImage(raw, 0, 0, null);
		g.dispose();
		
		BufferedImage img = new BufferedImage(raw.getHeight(), raw.getWidth(), BufferedImage.TYPE_BYTE_GRAY);
		
		//get pixels and rotate 90 degrees
		byte [] srcPixels = ((DataBufferByte) gray.getRaster().getDataBuffer()).getData();
		byte [] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
		
		for (int y=0;y<gray.getHeight();y++) {
			for (int x=0;x<gray.getWidth();x++) {
				int destX = y;
				int destY = gray.getWidth() - x - 1;
				pixels[destY * gray.getHeight() + destX] = srcPixels[y * gray.getWidth() + x];
			}
		}
		
		
		
		return img;
	}
	
	
}
