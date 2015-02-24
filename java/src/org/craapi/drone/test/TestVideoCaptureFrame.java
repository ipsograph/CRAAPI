package org.craapi.drone.test;

import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.craapi.drone.ar2.DroneVideoBuffer;
import org.craapi.drone.ar2.DroneVideoConnection;

import edu.brown.cs.common.video.BufferedImageReader;
import edu.brown.cs.common.video.FileVideoConnection;
import edu.brown.cs.common.video.VideoConnectionBuffer;
import edu.brown.cs.common.video.VideoFrame;
import edu.brown.cs.common.video.VideoFrameBufferBase;
import edu.brown.cs.common.video.xuggler.XugglerReader;

public class TestVideoCaptureFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	private BufferedImage image = null;

	public static void main(String[] args) {
		new TestVideoCaptureFrame(false);
	}

	public TestVideoCaptureFrame(boolean live) {
		super("Test Video");

		setSize(640, 360);
		setVisible(true);

		final XugglerReader rawReader = new XugglerReader(live ? null : "C:/dev/temp/TEST.MP4");
		rawReader.setOutputDimensions(360, 640);
		final BufferedImageReader imgReader = new BufferedImageReader(
				rawReader, 360, 640, BufferedImage.TYPE_3BYTE_BGR);

		final VideoConnectionBuffer<BufferedImage> buffer;
		if (live) {
			final DroneVideoConnection<VideoFrame<BufferedImage>> connection = new DroneVideoConnection<VideoFrame<BufferedImage>>(
					imgReader);
			buffer = new DroneVideoBuffer<BufferedImage>(
					connection, new VideoFrameBufferBase<BufferedImage>());
		} else {
			File video = new File("C:/dev/temp/TEST.MP4");
			final FileVideoConnection<VideoFrame<BufferedImage>> connection = new FileVideoConnection<VideoFrame<BufferedImage>>(
					imgReader, video, 30);
			buffer = new DroneVideoBuffer<BufferedImage>(
					connection, new VideoFrameBufferBase<BufferedImage>());
		}

		// stop stream processing
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				buffer.stop();
				System.exit(0);
			}
		});

		buffer.start();

		while (buffer.isStarted()) {

			try {
				final VideoFrame<BufferedImage>[] frame = buffer.poll(1, 1000);
				if (frame != null && frame.length == 1) {
					image = frame[0].getContents();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							repaint();
						}
					});
				}
			} catch (Exception e) {
				System.err
						.println("[ERROR] Issue encountered while processing frame: "
								+ e.getMessage() == null ? "" : e.getMessage());
				e.printStackTrace(System.err);
			}
		}

		buffer.stop();
	}

	public synchronized void paint(Graphics g) {
		if (image != null) {
			g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
		}
	}
}
