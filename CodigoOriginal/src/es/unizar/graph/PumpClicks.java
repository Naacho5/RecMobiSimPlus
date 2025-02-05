package es.unizar.graph;

import org.graphstream.ui.view.ViewerPipe;

public class PumpClicks implements Runnable {
	
	private ViewerPipe pipe;
	private LoopController control;
			
	public PumpClicks(ViewerPipe pipe,LoopController control) {
		this.pipe = pipe;
		this.control = control;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!control.stop) {
			pipe.pump();
		}
	}

}
