# Betzuka laser level

Java app to use a USB webcam to accurately detect the height of a laser line to a few microns (depending on how the webcam sensor is fixed and its pixel pitch). Works best if the camera is used without optics and the laser line is formed with a cylindrical lens 
so that it has a gaussian distribution of brightness. This app will locate the centre of the beam as the centre of the gaussian.

To be used with levelling machines.

This is a very early work in progress.

## Dependencies
Project depends on Apache Math 3.6.2 for fitting gaussians and OpenCV for interfacing to the webcam.

## Build
Clone this repository then build with maven.

`mvn package`

You may have to run this twice. Note that it will download the collosal OpenCV package then repackage everyting in a single jar which will be about 100Mb.

## Running

First connect your webcam. The find the jar file just built (it will be in the `target` directory). Run:

`java -cp laserlevel-1.0-SNAPSHOT.jar betzuka.tools.laserlevel.LaserLevel`
