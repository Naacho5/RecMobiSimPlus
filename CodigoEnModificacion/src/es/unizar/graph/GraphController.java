package es.unizar.graph;

import java.awt.event.MouseEvent;

import javax.swing.JTextArea;
import javax.swing.event.MouseInputListener;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

public class GraphController implements ViewerListener{
	
	ViewerPipe pipe;
	Graph graph;
	JTextArea infoText;
	
	public GraphController(ViewerPipe pipe,Graph graph,LoopController control,JTextArea infoText) {
		this.pipe = pipe;
		this.graph = graph;
		this.infoText = infoText;
		PumpClicks pc = new PumpClicks(pipe,control);
		Thread th = new Thread(pc);
		th.start();
	}

	@Override
	public void buttonPushed(String arg0) {
		// TODO Auto-generated method stub
		if(arg0.contains("_")) {
			Edge e = graph.getEdge(arg0);
			infoText.setText(e.getNode0().getId()+" -> "+e.getNode1().getId());
		}else {
			Node n = graph.getNode(arg0);
			String info = "ID: "+arg0+System.lineSeparator()+"nodeType: "+n.getAttribute("nodeType")+System.lineSeparator();
			for(String key: n.attributeKeys().toList()) {
				if(!key.contains(".") && !key.equals("xyz") && !key.equals("nodeType")) info += key+": "+n.getAttribute(key)+System.lineSeparator();
			}
			infoText.setText(info);
		}
	}

	@Override
	public void buttonReleased(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseLeft(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseOver(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void viewClosed(String arg0) {
		// TODO Auto-generated method stub
		
	}

}
