package org.craapi.drone.ar2;

public class IgnoredOptionContent implements NavOptionContent {

	@Override
	public boolean process(byte[] data) {
		return false;
	}
}
