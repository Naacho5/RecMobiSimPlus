package es.unizar.util;

import java.awt.TextArea;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class TextAreaHandler extends Handler {
	
	TextArea textConsole = null;
	
	public TextAreaHandler(TextArea textConsole) {
		super();
		this.textConsole = textConsole;
	}

	@Override
	public void publish(LogRecord record) {
		
		if (textConsole != null) {
			textConsole.append(record.getMessage() + "\n");
			textConsole.setCaretPosition(textConsole.getText().length());
			textConsole.paint(textConsole.getGraphics());
		}
	}

	@Override
	public void flush() {
		//this.flush();
	}

	@Override
	public void close() throws SecurityException {
		//this.close();
	}
	
	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
	}
	
	public TextArea getTextArea() {
        return this.textConsole;
    }
	
	public void setTextArea(TextArea textArea) {
        this.textConsole = textArea;
    }

}
