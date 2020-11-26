package de.srsoftware.web4rail;

public enum Protocol{	
	DCC14(14),
	DCC27(27),
	DCC28(28),
	DCC128(128),
	MOTO(14),
	FLEISCH(15),
	SELECTRIX(31);
	
	public int steps;
	
	Protocol(int steps) {
		this.steps = steps;
	}
}
