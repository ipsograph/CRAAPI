package org.craapi.drone.ar2;

/**
 * 
 * 16-bit id
 * 16-bit size
 * size-byte data
 * 
 */
public class NavOption {
	
	protected E_OptionTag tag;
	protected int size;
	protected byte[] data;
	protected NavOptionContent parsedContent;
	
	public E_OptionTag getTag() {
		return tag;
	}
	
	public NavOptionContent getParsedContent() {
		return parsedContent;
	}
	
	public void setTagAndData(int tag, byte[] data) {
		try {
			if (tag == -1 || tag == 0xffff) {
				this.tag = E_OptionTag.NAVDATA_CKS;
			} else {
				E_OptionTag temp = E_OptionTag.values()[tag];
				// XXX hack due to incomplete options tag enumeration
				if (temp == E_OptionTag.NAVDATA_CKS) {
					this.tag = null;
				} else {
					this.tag = temp;
				}
			}
			this.parsedContent = this.tag.createContent(data);
		} catch (Exception e) {
			this.tag = null;
		}
	}
	
	public static enum E_OptionTag {
		NAVDATA_DEMO(NavdemoContent.class),	// Minimum data needed
		NAVDATA_TIME,			// ARDrone current time
		NAVDATA_RAW_MEASURES,	// Raw measures (acceleros & gyros) coming from PIC
		NAVDATA_PHYS_MEASURES,	// Filtered values after control processing
		NAVDATA_GYROS_OFFSETS,	// Gyros offsets
		NAVDATA_EULER_ANGLES,	// Fused euler angles
		NAVDATA_REFERENCES,	
		NAVDATA_TRIMS,
		NAVDATA_RC_REFERENCES,
		NAVDATA_PWM,			// Data used to control motors
		NAVDATA_ALTITUDE,		// Estimated values with a relation to altitude
		NAVDATA_VISION_RAW,		// Vision's estimated velocities
		NAVDATA_VISION,			// Data used when computing vision
		NAVDATA_VISION_PERF,	// Performance data collected when profiling vision code
		NAVDATA_TRACKERS_SEND,	// Position of all trackers computed by vision
		NAVDATA_VISION_DETECT,	// Position of the chemney detected by vision
		NAVDATA_WATCHDOG,		// Tells if there was an abnormal delay between two navdata packets
		NAVDATA_IPHONE_ANGLES,	// Used to send back to iPhone its attitude (was an attempt to compute latency between ardrone & iPhone)
		NAVDATA_ADC_DATA_FRAME,	// Used in remote control. Sends data frame coming from PIC
		NAVDATA_VIDEO_STREAM,
		NAVDATA_CKS(ChecksumContent.class);		// Checksum
		
		private final Class<? extends NavOptionContent> contentClass;
		
		private E_OptionTag() {
			this(IgnoredOptionContent.class);
		}
		
		private E_OptionTag(Class<? extends NavOptionContent> contentClass) {
			this.contentClass = contentClass;
		}
		
		public NavOptionContent createContent(byte[] data) {
			try {
				NavOptionContent c = contentClass.newInstance();
				if (c.process(data)) {
					return c;
				} else {
					return null;
				}
			} catch (Exception e) {
				Util.printError("Failed to create NAVDATA option content", e, true);
				return null;
			}
		}
	}
}
