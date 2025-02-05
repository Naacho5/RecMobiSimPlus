package es.unizar.controller;

import es.unizar.gui.Configuration;
import es.unizar.gui.MainSimulator;

public class Controller implements AppListener{

	@Override
	public void onClose() {
		if (MainSimulator.userRunnable != null) {
			// Stop thread + disconnect from DB
			MainSimulator.userRunnable.running = false;
			try {
				Configuration.simulation.disconnect();
			}
			catch (Exception e) {
				//e.printStackTrace();
			}
		}
		
		// Close/Exit
		System.exit(0);
	}

}
