package org.craapi.drone;

public class Bid implements Comparable<Bid> {

	private final Integer value;
	private final boolean cooperative;
	private final Object bidder;

	public Bid(Object bidder, Integer value, boolean cooperative) {
		this.bidder = bidder;
		this.value = value;
		this.cooperative = cooperative;
	}
	
	public Object getBidder() {
		return bidder;
	}

	public Integer getValue() {
		return value;
	}

	public boolean isCooperative() {
		return cooperative;
	}

	@Override
	public int compareTo(Bid o) {
		int v = 0;
		if (value == null) {
			if (o.getValue() == null) {
				v = 0;
			} else {
				v = -1;
			}
		} else {
			if (o.getValue() == null) {
				v = 1;
			} else {
				v = value.compareTo(o.getValue());
			}
		}
		if (v == 0) {
			if (!isCooperative()) {
				if (o.isCooperative()) {
					v = 1;
				} else {
					v = 0;
				}
			} else {
				if (!o.isCooperative()) {
					v = -1;
				} else {
					v = 0;
				}
			}
		}
		return v;
	}
}
