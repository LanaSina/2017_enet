package startup;

import graphics.ExperimentThread;

public class Starter {
	

	public static void main(String[] args) {		
		new Thread(new ExperimentThread()).start();		
	}

}
