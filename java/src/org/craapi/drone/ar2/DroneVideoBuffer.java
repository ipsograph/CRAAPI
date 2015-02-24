package org.craapi.drone.ar2;

import java.util.concurrent.ExecutorService;

import edu.brown.cs.common.video.VideoConnection;
import edu.brown.cs.common.video.VideoConnectionBuffer;
import edu.brown.cs.common.video.VideoFrame;
import edu.brown.cs.common.video.VideoFrameBuffer;

public class DroneVideoBuffer<T> extends VideoConnectionBuffer<T> {
	
	public DroneVideoBuffer(VideoConnection<VideoFrame<T>> videoConnection, VideoFrameBuffer<T> frameBuffer) {
		super(videoConnection, frameBuffer);
	}

	public DroneVideoBuffer(VideoConnection<VideoFrame<T>> videoConnection, VideoFrameBuffer<T> frameBuffer,
			ExecutorService exec) {
		super(videoConnection, frameBuffer, exec);
	}

	@Override
	public boolean start() {
		
		if (!super.start()) {
			Util.printError("Connection may not have been established",
					null, false);
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean stop() {
		
		if (!super.stop()) {
			Util.printWarn("Connection may not have been closed");
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	protected void handleException(Exception e) {
		Util.printError("Failed to read video frame", e, true);
	}
}
