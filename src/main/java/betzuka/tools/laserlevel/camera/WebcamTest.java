package betzuka.tools.laserlevel.camera;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import com.github.sarxos.webcam.Webcam;

public class WebcamTest {

	
	public static void main(String [] args) throws Exception {
		Webcam cam = Webcam.getDefault();
		Dimension dim = new Dimension(960, 540);
		cam.setCustomViewSizes(dim);
		cam.setViewSize(dim);
		cam.open();
		
		BufferedImage i = cam.getImage();
		System.out.println(i.getWidth() + "x" + i.getHeight());
		System.out.println(cam.getViewSize());
		
		cam.close();
	}
	
}
