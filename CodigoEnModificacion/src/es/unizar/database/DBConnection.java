/*
 * @(#)Context.java  1.0.0  27/09/14
 *
 * MOONRISE
 * Webpage: http://webdiis.unizar.es/~maria/?page_id=250
 * 
 * University of Zaragoza - Distributed Information Systems Group (SID)
 * http://sid.cps.unizar.es/
 *
 * The contents of this file are subject under the terms described in the
 * MOONRISE_LICENSE file included in this distribution; you may not use this
 * file except in compliance with the License.
 *
 * Contributor(s):
 *  RODRIGUEZ-HERNANDEZ, MARIA DEL CARMEN <692383[3]unizar.es>
 *  ILARRI, SERGIO <silarri[3]unizar.es>
 */
package es.unizar.database;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Database connection.
 *
 * @author Maria del Carmen Rodriguez-Hernandez
 */
public class DBConnection {

	/**
	 * URL of the database.
	 */
	public String dbURL;
	/**
	 * Object used for executing a static SQL statement and returning the
	 * results it produces.
	 */
	public Statement statement;
	/**
	 * A connection (session) with a specific database.
	 */
	public Connection connection;
	/**
	 * Constant "org.sqlite.JDBC"
	 */
	public static final String SQLITE = "org.sqlite.JDBC";

	/**
	 * Constructor.
	 *
	 * @param dbURL
	 *            Database URL.
	 */
	public DBConnection(String dbURL) {
		this.dbURL = dbURL;
		this.statement = null;
		this.connection = null;
	}
}
