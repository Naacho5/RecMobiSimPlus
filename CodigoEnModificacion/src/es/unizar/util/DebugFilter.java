package es.unizar.util;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

/*
 * Class that filters which messages should be printed in "DEBUG_MESSAGES" logger
 * 
 * https://stackoverflow.com/questions/48310498/filtering-level-of-logger-messages
 * Filter sequence in which log messages are filtered:
 * 	1. Context-wide filters
 * 	2. Level declared at logger level
 * 	3. Appender filter
 */
public class DebugFilter implements Filter {

	/*
	 * WHEN LEVEL IS LOW (ALL, FINEST, FINER...) -> 
	 * If logger's level is higher than message's level, this filter won't be applied
	 * 
	 * Print only messages which show high consuming times (>= DEBUG_MIN_TIME_TO_BE_PRINTED)
	 */
	@Override
	public boolean isLoggable(LogRecord record) {
		
		if (record.getMessage() == Literals.BEGINNING_DEBUG_MESSAGE)
			return true;
		if (record.getMessage() == Literals.ENDING_DEBUG_MESSAGE)
			return true;
		
		int timeSpent;
			
		try{
			// Get the last word (number) which will contain a number that represents
			timeSpent = Integer.valueOf(record.getMessage().substring(record.getMessage().lastIndexOf(" ")+1));
		} catch(Exception e) {
			timeSpent = -1;
		}
		
		//System.out.print("RECORD: " + record.getMessage() + "; ");
		// If time in that operation >= 5 ms
		if (timeSpent >= Literals.DEBUG_MIN_TIME_TO_BE_PRINTED) {
			//System.out.println("\t\t\t\tLAST NUMBER: " + timeSpent);
			return true;
		}
		else {
			//System.out.println();
			return false;
		}
	}

}
