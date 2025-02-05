package es.unizar.util;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/*
 * Class that gives "DEBUG_MESSAGES" logger's messages format as the following:
 * 
 * 	- WARNING or higher:
 * 		class, methodName:
 * 		   message
 * 
 * - INFO or lower:
 * 		message
 */
public class DebugFormatter extends Formatter {

	@Override
	public String format(LogRecord record) {
		
		String formattedMessage = "";
		
		if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
			//formattedMessage += record.getResourceBundleName() + ", ";
			formattedMessage += record.getSourceClassName() + ", ";
			formattedMessage += record.getSourceMethodName() + ":\n";
			formattedMessage += "   " + record.getMessage() + "\n";
		} else {
			formattedMessage += record.getMessage() + "\n";
		}
		
		return formattedMessage;
	}

}
