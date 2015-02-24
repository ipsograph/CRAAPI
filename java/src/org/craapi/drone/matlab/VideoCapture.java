package org.craapi.drone.matlab;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.craapi.drone.ar2.DroneVideoBuffer;
import org.craapi.drone.ar2.DroneVideoConnection;
import org.craapi.drone.ar2.Util;

import edu.brown.cs.common.video.FileVideoConnection;
import edu.brown.cs.common.video.VideoByteBuffer;
import edu.brown.cs.common.video.VideoFrame;
import edu.brown.cs.common.video.VideoFrameBuffer;
import edu.brown.cs.common.video.VideoFrameBufferBase;
import edu.brown.cs.common.video.xuggler.XugglerReader;

/**
 * Very simple streaming video interface (intended for use in Matlab)
 * <p>
 * This implementation favors speed over accuracy, so occasional frames may be
 * discarded. The actual frame rate is typically a little lower than the desired
 * frame rate.
 * <p>
 * his project uses xuggler libraries to process the drone video stream. I have
 * deliberately kept the video and drone portions of the code separate, since
 * the drone portion has no dependencies.
 * <p>
 * 
 * @author nate
 * @see <a
 *      href="http://xuggle.googlecode.com/svn/trunk/repo/share/java/xuggle/xuggle-xuggler/5.4/">xuggler
 *      jar builds</a>
 * 
 */
public class VideoCapture {

	protected static final int FLAG_720p = 1;
	protected static final byte[] EMPTY = new byte[0];
	protected VideoFrameBuffer<byte[]> frameBuffer;
	protected DroneVideoBuffer<byte[]> videoBuffer;
	protected VideoByteBuffer byteBuffer;
	protected AtomicBoolean capturing = new AtomicBoolean();
	protected ExecutorService imgExec;

	/**
	 * Starts the image capture thread. Subsequent calls to
	 * {@link #getFrame(long)} or {@link #getFrames(int, long)} will return the
	 * latest frames available from the device.
	 * <p>
	 * Once this is called, call {@link #stopImageCapture()} to shut down all
	 * threads.
	 * 
	 * @param frameRate
	 *            the desired (capture) frame rate
	 * @param nFramesToRetain
	 *            the maximum number of frames retained in the buffer
	 */
	public void startImageCapture(final int frameRate,
			final int nFramesToRetain) {

		if (capturing.compareAndSet(false, true)) {

			imgExec = Executors.newCachedThreadPool();

			final DroneVideoConnection<VideoFrame<byte[]>> connection = new DroneVideoConnection<VideoFrame<byte[]>>(
					new XugglerReader());
			frameBuffer = new VideoFrameBufferBase<byte[]>();
			videoBuffer = new DroneVideoBuffer<byte[]>(connection, frameBuffer, imgExec);
			videoBuffer.setBufferCapacity(nFramesToRetain);
			videoBuffer.setFps(frameRate);
			byteBuffer = new VideoByteBuffer(videoBuffer);

			imgExec.submit(new Runnable() {

				@Override
				public void run() {
					try {
						videoBuffer.start();
					} catch (Exception e) {
						Util.printError(
								"Failure during video to image capture", e,
								true);
					}
				}
			});
		}
	}
	
	public void testImageCapture(final int frameRate, final int nFramesToRetain, final int playbackFps, final String videoFilePath) {
		
		if (capturing.compareAndSet(false, true)) {

			imgExec = Executors.newCachedThreadPool();

			File video = new File(videoFilePath);
			XugglerReader rawReader = new XugglerReader(videoFilePath);
			rawReader.setOutputDimensions(360, 640);
			final FileVideoConnection<VideoFrame<byte[]>> connection = new FileVideoConnection<VideoFrame<byte[]>>(
					rawReader, video, playbackFps);
			frameBuffer = new VideoFrameBufferBase<byte[]>();
			videoBuffer = new DroneVideoBuffer<byte[]>(connection, frameBuffer, imgExec);
			videoBuffer.setBufferCapacity(nFramesToRetain);
			videoBuffer.setFps(frameRate);
			byteBuffer = new VideoByteBuffer(videoBuffer);

			imgExec.submit(new Runnable() {

				@Override
				public void run() {
					try {
						videoBuffer.start();
					} catch (Exception e) {
						Util.printError(
								"Failure during video to image capture", e,
								true);
					}
				}
			});
		}
	}

	/**
	 * Stops the video capture thread(s) and resets this {@link VideoCapture}
	 * object for future use.
	 */
	public void stopImageCapture() {
		if (capturing.compareAndSet(true, false)) {
			imgExec.shutdownNow();
			imgExec = null;
			videoBuffer.stop();
		}
	}

	/**
	 * Returns up to <code>nFrames</code> frames and waits up to
	 * <code>maxWaitMs</code> for those frames. If the desired number of frames
	 * are not obtained within this time, the result returns with fewer frames.
	 * If no frames are obtained, the result is an empty array. These are the
	 * latest frames available from the device. If this function is called
	 * infrequently, unclaimed frames are simply discarded.
	 * <p>
	 * The first dimension contains the frames (its length is the number of
	 * frames returned). Each frame (the second array dimension) is a 1D,
	 * row-major array of pixels.
	 * <p>
	 * The Matlab function <code>getFrameStats</code> attempts to guess width,
	 * height, and colorspace from the frame length.
	 * <p>
	 * In the future, it would be nice to provide a header along with each
	 * frame. The header could contain this information along with any
	 * annotations from the drone (e.g. reported kinematics).
	 * <p>
	 * Also, a pre-allocated byte array option would be nice. I am not sure how
	 * well it would work in Matlab.
	 * 
	 * @param nFrames
	 *            the maximum number of frames to return
	 * @param maxWaitMs
	 *            the maximum time to wait for the desired number of frames (in
	 *            milliseconds)
	 * @return the most recent video frames obtained from the video stream
	 */
	public byte[][] getFrames(int nFrames, long maxWaitMs) {
		return byteBuffer.getFrames(nFrames, maxWaitMs);
	}

	/**
	 * Returns the most recent frame from the video stream, waiting up to
	 * <code>maxWaitMs</code> milliseconds for the frame. If no frame is
	 * obtained, an empty array is returned.
	 * 
	 * @param maxWaitMs
	 *            the maximum time to wait for the frame (in milliseconds)
	 * @return the most recent video frame obtained from the video stream
	 */
	public byte[] getFrame(long maxWaitMs) {
		byte[][] frames = byteBuffer.getFrames(1, maxWaitMs);
		if (frames.length > 0) {
			return frames[0];
		} else {
			return EMPTY;
		}
	}

	/**
	 * Test entry point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		main();
	}

	/**
	 * To test from Matlab...
	 */
	public static void mainForMatlab() {
		final Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				main();
			}
		});
		t.start();
	}

	/**
	 * Test: run for 60 seconds, periodically report frame stats and approximate
	 * frame rate
	 */
	public static void main() {
		VideoCapture video = new VideoCapture();
		video.startImageCapture(10, 200);

		long start = System.currentTimeMillis();
		int nFrames = 0;
		while ((System.currentTimeMillis() - start) < 60000) {

			byte[][] frames = video.getFrames(5, 1000);
			if (frames.length > 0) {
				nFrames += frames.length;
				if (nFrames % 20 == 0) {
					System.out.println("Got " + frames.length + " frames...");
					System.out.println("Size is : "
							+ (frames.length * frames[0].length));
				}
			}

			Thread.yield();
		}
		System.out.println("Frame rate was about " + ((float) nFrames)
				/ ((System.currentTimeMillis() - start) / 1000.f));

		video.stopImageCapture();
	}
}
