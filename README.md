CRAAPI
======

Completely Redundant ARDrone API - Java implementation of the ARDrone 2.0 interface intended for use with Matlab (made for school project; not polished nor complete...but it works)

By 'completely redundant' I mean that this implementation is unnecessary. I wrote most of this code prior to discovering the YADrone project (https://github.com/MahatmaX/YADrone), which is a much more complete Java interface for the ARDrone. While I have not used it myself, I strongly encourage you check out that project rather than this one. I went down a strange path with resource control and handling concurrent decisions/commands (currently half-baked in the code). It works, but it's unnecessary. Basically, you may control certain drone behaviors by specifying 'behaviors' that go along with drone states. For example, when in the 'grounded' state, the behavior is 'wait for a takeoff command'. While this sounds nice, the implementation is mostly silly.

That said, I wrote this code as part of a 'final project' for CLPS 1520 at Brown University. I have included Matlab code that uses the API to cause the drone to take off and then attempt to make small movements (up, down, left, and right) to follow the dominant motion it perceives (i.e., it tries to mirror your motions if it is looking at you). It seemed to me that a Java API was the best way to integrate Matlab and the ARDrone. It worked for me and maybe it will for you as well.

Be warned - This code will likely require some Java tinkering on your part. Matlab can be touchy when it comes to compiled versions of JAR files. In the particular version of Matlab I used (R2013a Student for Windows), I had to ensure that the Jar files were compiled for Java 6 (even though I've been using later versions).

<b>Licensing</b>

This software (CRAAPI) is provided under the MIT License (see LICENSE file). The Java project (Eclipse IDE) contains a Maven POM, which declares Xuggler as a dependency. This is only true if you intend to use the video support included in this project. Since this build of Xuggler is GPL, you will have to get it yourself (easy with Maven).

<b>Getting Started</b>

1. Download the repo
2. Download xuggler (via Maven, preferably - see java project) and place in the matlab/java directory (there is a README there with more details)
3. Try it out

Once you have downloaded the code and pre-built Java libraries (including xuggler), you can either tinker in Java or Matlab. From an API perspective, the Java portion handles the nuts and bolts of the ARDrone connection (socket management, message formatting and parsing, state management, and video processing). The Matlab interface abstracts most of these details away and is intended to provide a 'simple' interface to the drone.

If you are interested in Matlab, take a look at the <code>dronedriver_demo.m</code> script. This script shows you how to connect to the drone, process video, and command the drone to take simple actions. Note that without a drone to connect to, you may run into some issues (such as Matlab locking up while it attempts to connect). You can fiddle with the various flags in order to run demo/droneless versions of the script. One lesson I learned while playing around in Matlab is that Java (in matlab) can have some serious memory issues. It may be due to a leak within CRAAPI or due to poor cleanup in my Matlab code. Either way, take care with memory management. The drone video can really take a toll on Matlab.

If you want to tinker in Java, take a look at the <code>org.craapi.drone.matlab</code> and <code>org.craapi.drone.test</code> packages. These contain simple examples of API usage.

Good luck!
