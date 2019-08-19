# Betzuka laser level

Java app to use a USB webcam to resolve the location of a laser line to a few microns (depending on how the webcam sensor is fixed and its pixel pitch). Works best if the camera is used without optics and the laser line is formed with a cylindrical lens 
so that it has a gaussian distribution of brightness. It wont work as well with the ultra cheap £3 laser line generators that use a diffraction pattern rather than a glass lens to create the line. This app will locate the centre of the beam as the centre of the gaussian shaped intensity profile when reading intensity orthogonal to the laser line.

Goal is to use it for levelling machines, bringing multiple objects (e.g. linear rails) into plane, making things extremely straight, etc. The sensor should be orientated at 90 degrees if reading a horizontal laser line, or vica-versa if reading a vertical laser line.

![Sensor orientation](/doc/imgs/sensor_orientation.jpg?raw=true)

This is a very early work in progress, screenshot is working off a standard Dewalt DW088 builder's levelling laser. The webcam is the sensor board from a £7 vga camera that has been rehoused in a machined block.

The screenshot is of the sensor block zero'd (blue line) then raised by placing it on a piece of copy paper, showing a reading of ~29 pixels at the gaussian centre (red line). The paper is ~80 microns thick so it is resolving to ~3 microns or 1 ten-thousandth of an inch if you are that way inclined, pretty good for £7.

![Screenshot](/doc/imgs/screen_shot_1.png?raw=true)
![Sensor](/doc/imgs/sensor_block_1.jpg?raw=true)

## Dependencies
Project depends on Apache Math 3.6.2 for fitting gaussians and OpenCV for interfacing to the webcam.

## Build
Clone this repository then build with maven.

`mvn package`

You may have to run this twice. Note that it will download the colossal OpenCV package then repackage everything into a single jar which will be about 100Mb. I will endeavour to remove the OpenCV dependency as it is only used to read the webcam.

## Running

First connect your webcam. Then find the jar file just built (it will be in the `target` directory). Run:

`java -cp laserlevel-1.0-SNAPSHOT.jar betzuka.tools.laserlevel.LaserLevel`
