package org.craapi.drone.ar2;

import java.io.IOException;
import java.io.OutputStream;

import edu.brown.cs.common.video.TcpVideoConnection;
import edu.brown.cs.common.video.VideoStreamReader;

public class DroneVideoConnection<T> extends TcpVideoConnection<T> {

	private static final byte[] DRONE_ADDRESS = new byte[] { (byte) 192,
			(byte) 168, 1, 1 };
	private static final byte[] HANDSHAKE = new byte[] { 0x01, 0x00, 0x00, 0x00 };
	private static final int VIDEO_PORT = 5555;

	public DroneVideoConnection(VideoStreamReader<T> reader) {
		super(reader, DRONE_ADDRESS, VIDEO_PORT);
	}
	
	@Override
	protected void handshake(OutputStream os) throws IOException {
		os.write(HANDSHAKE);
	}
	
	@Override
	protected void handleConnectException(Exception e) {
		Util.printError("Failed to connect to video port:", e, true);
	}
	
	@Override
	protected void handleReadException(Exception e) {
		Util.printError("Failed to read next frame from stream", e, true);
	}
}
