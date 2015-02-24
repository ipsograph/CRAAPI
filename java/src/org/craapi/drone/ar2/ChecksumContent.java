package org.craapi.drone.ar2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 32-bit checksum data
 */
public class ChecksumContent implements NavOptionContent {

	protected int sum;

	@Override
	public boolean process(byte[] data) {

		if (data.length != 4) {
			Util.printWarn("Cannot parse NAVDATA checksum. "
					+ "Insufficient or execessive number of bytes remaining ("
					+ data.length + ")");
			return false;
		}

		try {
			final ByteBuffer bb = ByteBuffer.wrap(data).order(
					ByteOrder.LITTLE_ENDIAN);
			this.sum = bb.getInt();
			return true;
		} catch (Exception e) {
			Util.printError("Exception encountered"
					+ " while parsing NAVDATA checksum", e, true);
			return false;
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("   sum=").append(sum).append("\n");
		return sb.toString();
	}
}
