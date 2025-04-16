package es.unizar.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import es.unizar.util.Literals;

public class Database {
	
	private Connection conn;
	
	public Database() {}
	
	public Connection getConnection() {
		return conn;
	}
	
	public void connect(String dbURL) throws ClassNotFoundException, SQLException {
		if (conn != null)
			return;

		try {
			Class.forName(Literals.SQLITE);
		} catch (ClassNotFoundException e) {
			throw new ClassNotFoundException("Driver not found");
		}
		
		conn = DriverManager.getConnection(dbURL);
		
		conn.setAutoCommit(false);
	}
	
	public void disconnect() {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				System.out.println("Couldn't disconnect from database");
			}
		}
		
		conn = null;
	}
	
	public void commit() {
		
		if (conn == null)
			return;
		
		try {
			conn.commit();
			//System.out.println("COMMITED");
		} catch (SQLException e) {
			try {
				conn.rollback();
				System.out.println("Rollback produced - info not correctly persisted");
			} catch (SQLException ex) {
				System.out.println(ex.getMessage());
			}
			System.out.println(e.getMessage());
		}
	}
	
}
