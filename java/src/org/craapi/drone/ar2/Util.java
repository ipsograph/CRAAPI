package org.craapi.drone.ar2;

import java.nio.ByteBuffer;

public class Util {
	
	public static boolean ERROR_ENABLED = true;
	public static boolean WARN_ENABLED = false;
	public static boolean INFO_ENABLED = false;
	public static boolean DEBUG_ENABLED = false;
	
	public static String encodeParameter(Float f) {
		return Integer.toString(Float.floatToIntBits(f));
	}
	
	public static long getUInteger(ByteBuffer bb) {
		return bb.getInt() & 0xffffffffL;
	}
	
	public static int getUShort(ByteBuffer bb) {
		return bb.getShort() & 0xffff;
	}
	
	public static float[] readFloatArray(ByteBuffer bb, int length) {
		float[] a = new float[length];
		for (int i=0; i<length; i++) {
			a[i] = bb.getFloat();
		}
		return a;
	}
	
	public static int[] readIntegerArray(ByteBuffer bb, int length) {
		int[] a = new int[length];
		for (int i=0; i<length; i++) {
			a[i] = bb.getInt();
		}
		return a;
	}
	
	public static void printWarn(String message) {
		if (WARN_ENABLED) {
			System.out.println("[WARN] " + message);
		}
	}
	
	public static void printInfo(String message) {
		if (INFO_ENABLED) {
			System.out.println("[INFO] " + message);
		}
	}
	
	public static void printDebug(String message) {
		if (DEBUG_ENABLED) {
			System.out.println("[DEBUG] " + message);
		}
	}
	
	public static void printError(String message, Exception e, boolean pst) {
		if (ERROR_ENABLED) {
			if (e != null && e.getMessage() != null) {
				message = message + " : " + e.getMessage();
			}
			System.err.println("[ERROR] " + message);
			if (e != null && pst) {
				e.printStackTrace(System.err);
			}
		}
	}
}
