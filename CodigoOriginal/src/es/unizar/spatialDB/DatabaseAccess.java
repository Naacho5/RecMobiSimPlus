package es.unizar.spatialDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import es.unizar.editor.model.Connectable;
import es.unizar.editor.model.Corner;
import es.unizar.editor.model.Door;
import es.unizar.editor.model.Drawable;
import es.unizar.editor.model.Item;
import es.unizar.editor.model.MapEditorModel;
import es.unizar.editor.model.Room;
import es.unizar.editor.model.RoomSeparator;
import es.unizar.editor.model.Stairs;
import es.unizar.util.Pair;

public class DatabaseAccess {

	String user = "postgres";
	String password = "database";
	String port = "5432";
	String databaseName = "RecMobiSimDB";
	Connection conn = null;
	
	String loadedMapName = null;
	int loadedMapId = -1;
				
	public DatabaseAccess() {System.out.println("new");}
	
	public DatabaseAccess(String user, String password, String port, String databaseName) {
		this.user = user;
		this.password = password;
		this.port = port;
		this.databaseName = databaseName;
	}
	
	public void setAttributes(String user, String password, String port, String databaseName) {
		this.user = user;
		this.password = password;
		this.port = port;
		this.databaseName = databaseName;
	}
		
	public String getUser() {
		return user;
	}

	public String getPort() {
		return port;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void connect() throws SQLException {
		String url = "jdbc:postgresql://localhost:"+port+"/"+databaseName;
		Properties props = new Properties();
		props.setProperty("user", user);
		props.setProperty("password", password);
		try {
			conn = DriverManager.getConnection(url, props);
			System.out.println("Connected to database "+url);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			conn = null;
			throw e;
		}
	}
	
	public boolean isConnected() {
		return (conn != null);
	}
	
	public void disconnect() {
		try {
			conn.close();
			conn = null;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void createTables() throws SQLException{
		try {
			
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT 1 FROM PG_TABLES WHERE TABLENAME='map';");
			int res = 0;
			if (rs.next()) {
			    res = rs.getInt(1);
			}
			rs.close();
			//System.out.println(res);
			if(res == 0) {
				//st = conn.createStatement();
				String sql = "CREATE TABLE IF NOT EXISTS MAP " +
					"(ID INT PRIMARY KEY, " +
					"NAME VARCHAR(200) NOT NULL UNIQUE, " +
					"WIDTH INT NOT NULL, " +
					"HEIGHT INT NOT NULL, " +
					"PIXEL_REPRESENTS_IN_METERS DOUBLE PRECISION NOT NULL, " +
					"DRAW_ICON_DIMENSION INT NOT NULL);";
				sql += "CREATE TABLE IF NOT EXISTS ROOM " +
					"(LABEL INT, " +
					"MAP INT REFERENCES MAP(ID) ON DELETE CASCADE, " +
					"GEOM GEOMETRY(POLYGON) NOT NULL, " +
					"PRIMARY KEY(LABEL,MAP));";
				sql += "CREATE TABLE IF NOT EXISTS SUBROOM_SEPARATOR " +
						"(LABEL DECIMAL, " +
						"ROOM_LABEL INT, " +
						"MAP INT, " +
						"GEOM GEOMETRY(LINESTRING) NOT NULL, " +
						"FOREIGN KEY(ROOM_LABEL,MAP) REFERENCES ROOM(LABEL,MAP) ON DELETE CASCADE, " +
						"PRIMARY KEY(LABEL,ROOM_LABEL,MAP));";
				sql += "CREATE TYPE CONN_TYPE AS ENUM ('DOOR', 'STAIRS'); " + 
					   "CREATE TABLE IF NOT EXISTS CONNECTABLE " +
						"(ID DECIMAL, " +
						"MAP INT, " +
					    "ROOM_LABEL INT, " +
						"LOCATION GEOMETRY(POINT) NOT NULL, " +
						"TYPE CONN_TYPE NOT NULL, " +
						"URL_IMAGE TEXT NOT NULL, " +
						"FOREIGN KEY(ROOM_LABEL,MAP) REFERENCES ROOM(LABEL,MAP) ON DELETE CASCADE, " +
						"PRIMARY KEY(ID,MAP,TYPE));";
				sql += "CREATE TABLE IF NOT EXISTS ITEM " +
							"(ID DECIMAL, " +
							"MAP INT, " +
							"ROOM_LABEL INT, " +
							"LOCATION GEOMETRY(POINT) NOT NULL, " +
							"URL_IMAGE TEXT NOT NULL, " +
							"TITLE TEXT NOT NULL, " +
							"WIDTH DOUBLE PRECISION, " +
							"HEIGHT DOUBLE PRECISION, " +
							"NATIONALITY TEXT, " +
							"BEGIN_DATE TIME, " +
							"END_DATE TIME, " +
							"DATE TEXT, " +
							"ITEM_LABEL TEXT, " +
							"FOREIGN KEY(ROOM_LABEL,MAP) REFERENCES ROOM(LABEL,MAP) ON DELETE CASCADE, " +
							"PRIMARY KEY(ID,MAP));";
				sql += "CREATE TABLE IF NOT EXISTS CONNECTION" +
						"(ID SERIAL PRIMARY KEY, " +
						"ID_CONN1 DECIMAL, " +
						"MAP_CONN1 INT, " +
						"TYPE_CONN1 CONN_TYPE, " +
						"ID_CONN2 DECIMAL, " +
						"MAP_CONN2 INT, " +
						"TYPE_CONN2 CONN_TYPE, " +
						"FOREIGN KEY(ID_CONN1,MAP_CONN1,TYPE_CONN1) REFERENCES CONNECTABLE(ID,MAP,TYPE) ON DELETE CASCADE, " +
						"FOREIGN KEY(ID_CONN2,MAP_CONN2,TYPE_CONN2) REFERENCES CONNECTABLE(ID,MAP,TYPE) ON DELETE CASCADE, " +
					    "CHECK(MAP_CONN1 = MAP_CONN2));";
				sql += "CREATE TABLE IF NOT EXISTS SIMULATION" +
						"(ID SERIAL PRIMARY KEY, " +
						"MAP_NAME TEXT, " +
						"MAP_ID INT REFERENCES MAP(ID) ON DELETE CASCADE, " +
						"TIME_AVAILABLE_USER INT, " +
						"DELAY_OBSERVING_ITEM INT, " +
						"USER_SPEED DECIMAL, " +
						"KM_TO_PIXEL DECIMAL, " +
						"TIME_ON_STAIRS INT, " +
						"BEGIN_TIME TIMESTAMP);";
				sql += "CREATE TABLE IF NOT EXISTS USER_SIM" +
						"(ID INT, " +
						"IS_SPECIAL BOOLEAN, " +
						"SIMULATION INT REFERENCES SIMULATION(ID) ON DELETE CASCADE, " +
						"PATH GEOMETRY(LINESTRING), " +
						"PRIMARY KEY(ID,SIMULATION));";
				sql += "CREATE TABLE IF NOT EXISTS VISIT" +
						"(ID INT, " +
						"SIMULATION INT REFERENCES SIMULATION(ID) ON DELETE CASCADE, " +
						"ROOM_LABEL INT, " +
						"MAP INT, " +
						"USER_ID INT, " +
						"DURATION DECIMAL, " +
						"PATH GEOMETRY(LINESTRING), " +
						"PRIMARY KEY(ID,USER_ID,SIMULATION), " +
						"FOREIGN KEY(USER_ID,SIMULATION) REFERENCES USER_SIM(ID,SIMULATION) ON DELETE CASCADE, " +
						"FOREIGN KEY(ROOM_LABEL,MAP) REFERENCES ROOM(LABEL,MAP) ON DELETE CASCADE);";
				sql += "CREATE TABLE IF NOT EXISTS ITEM_OBSERVATION" +
						"(ID INT," +
						"VISIT INT, " +
						"USER_ID INT, " +
						"SIMULATION INT, " +
						"ITEM_ID INT, " +
						"MAP INT, " +
						"PRIMARY KEY(ID,VISIT,SIMULATION), " +
						"FOREIGN KEY(ITEM_ID,MAP) REFERENCES ITEM(ID,MAP) ON DELETE CASCADE, " +
						"FOREIGN KEY(VISIT,USER_ID,SIMULATION) REFERENCES VISIT(ID,USER_ID,SIMULATION) ON DELETE CASCADE);";
				st.executeUpdate(sql);
				st.close();
			}
			
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			throw e;
		}

	}
	
	
	public int saveMap(MapEditorModel model) throws SQLException, NullPointerException {
		try {
			
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM MAP WHERE NAME='"+model.getName()+"';");
			int res = 0;
			if (rs.next()) {
			    res = rs.getInt(1);
			}
			rs.close();
			System.out.println(res);
			if(res == 0) {
				//st = conn.createStatement();
				rs = st.executeQuery("SELECT MAX(ID) FROM MAP;");
				int numMaps = 0;
				if (rs.next()) {
				    numMaps = rs.getInt(1);
				}
				rs.close();
				st.close();
				int mapID = numMaps+1;			
				
				PreparedStatement pst = conn.prepareStatement("INSERT INTO MAP(ID, NAME, WIDTH, HEIGHT, PIXEL_REPRESENTS_IN_METERS, DRAW_ICON_DIMENSION) VALUES (?,?,?,?,?,?);");
				pst.setInt(1, mapID);
				pst.setString(2, model.getName());
				pst.setInt(3, model.getMAP_W());
				pst.setInt(4, model.getMAP_H());
				pst.setDouble(5, model.getPixelRepresentsInMeters());
				pst.setInt(6, model.getDRAWING_ICON_DIMENSION());
				pst.executeUpdate();
				
				for(Room r : model.getRooms()) {
					String walls = "LINESTRING(";
					for(Corner c : r.getCorners()) {
						walls += c.getVertex_xy().getX()+" "+c.getVertex_xy().getY()+",";
						//if(c != r.getCorners().get(r.getNumCorners()-1)) walls += ","; 
					}
					walls += r.getCorners().get(0).getVertex_xy().getX()+" "+r.getCorners().get(0).getVertex_xy().getY()+")";
					pst = conn.prepareStatement("INSERT INTO ROOM(LABEL, MAP, GEOM) VALUES (?,?,ST_MakePolygon(ST_GeomFromText(?)));");
					// pst.setInt(1, r.getLabel());
					pst.setLong(1, r.getLabel()); // Modificado por Nacho Palacio 2025-05-24
					pst.setInt(2, mapID);
					pst.setString(3, walls);
					//System.out.println(pst);
					pst.executeUpdate();					
					for (RoomSeparator sep: r.getRoomSeparators()) {
						pst = conn.prepareStatement("INSERT INTO SUBROOM_SEPARATOR(LABEL, ROOM_LABEL, MAP, GEOM) VALUES (?,?,?,ST_GeomFromText(?));");
						pst.setLong(1, sep.getVertex_label());
						// pst.setInt(2, r.getLabel());
						pst.setLong(2, r.getLabel()); // Modificado por Nacho Palacio 2025-05-24
						pst.setInt(3, mapID);
						String wall = "LINESTRING("+sep.getC1().getVertex_xy().getX()+" "+sep.getC1().getVertex_xy().getY()+",";
						wall += sep.getC2().getVertex_xy().getX()+" "+sep.getC2().getVertex_xy().getY()+")";
						pst.setString(4, wall);
						pst.executeUpdate();
					}		
					pst.close();
				}
																
				Set<Pair<Connectable, Connectable>> connected = new HashSet<Pair<Connectable, Connectable>>();
				for(Door d : model.getDoors()) {
					pst = conn.prepareStatement("INSERT INTO CONNECTABLE(ID, MAP, ROOM_LABEL, LOCATION, TYPE, URL_IMAGE) VALUES (?,?,?,ST_GeomFromText(?),?::conn_type,?);");
					pst.setInt(1, (int)d.getVertex_label());
					pst.setInt(2, mapID);
					if(d.getRoom() == null) {
						pst.setNull(3,Types.INTEGER);
					}else {
						// pst.setInt(3, d.getRoom().getLabel());
						pst.setLong(3, d.getRoom().getLabel()); // Modificado por Nacho Palacio 2025-05-24
					}
					//pst.setInt(4, mapID);
					pst.setString(4, "POINT("+d.getVertex_xy().getX()+" "+d.getVertex_xy().getY()+")");
					pst.setString(5, "DOOR");
					pst.setString(6,d.getUrlIcon());
					pst.executeUpdate();
					pst.close();
					for (Connectable c: d.getConnectedTo()) {
						Pair<Connectable, Connectable> pair = new Pair<Connectable, Connectable>(d, c);
						Pair<Connectable, Connectable> pairSwitched = new Pair<Connectable, Connectable>(c, d);
						
						if(!connected.contains(pair) && !connected.contains(pairSwitched) && c != null)
							connected.add(pair);						
					}					
				}				
				for(Stairs d : model.getStairs()) {
					pst = conn.prepareStatement("INSERT INTO CONNECTABLE(ID, MAP, ROOM_LABEL, LOCATION, TYPE, URL_IMAGE) VALUES (?,?,?,ST_GeomFromText(?),?::conn_type,?);");
					pst.setLong(1, d.getVertex_label());
					pst.setInt(2, mapID);
					if(d.getRoom() == null) {
						pst.setNull(3,Types.INTEGER);
					}else {
						// pst.setInt(3, d.getRoom().getLabel());
						pst.setLong(3, d.getRoom().getLabel()); // Modificado por Nacho Palacio 2025-05-24
					}
					//pst.setNull(4, Types.INTEGER);
					pst.setString(4, "POINT("+d.getVertex_xy().getX()+" "+d.getVertex_xy().getY()+")");
					pst.setString(5, "STAIRS");
					pst.setString(6,d.getUrlIcon());
					pst.executeUpdate();
					pst.close();
					for (Connectable c: d.getConnectedTo()) {
						Pair<Connectable, Connectable> pair = new Pair<Connectable, Connectable>(d, c);
						Pair<Connectable, Connectable> pairSwitched = new Pair<Connectable, Connectable>(c, d);
						
						if(!connected.contains(pair) && !connected.contains(pairSwitched) && c != null)
							connected.add(pair);						
					}					
				}
				for(Pair<Connectable, Connectable> pair: connected) {
					pst = conn.prepareStatement("INSERT INTO CONNECTION(ID_CONN1, MAP_CONN1, TYPE_CONN1, ID_CONN2, MAP_CONN2, TYPE_CONN2) VALUES (?,?,?::conn_type,?,?,?::conn_type);");
					pst.setInt(1, (int)((Drawable)pair.getF()).getVertex_label());
					pst.setInt(2, mapID);
					if(pair.getF() instanceof Door) pst.setString(3, "DOOR");
					else pst.setString(3, "STAIRS");					
					pst.setInt(4, (int)((Drawable)pair.getS()).getVertex_label());
					pst.setInt(5, mapID);
					if(pair.getS() instanceof Door) pst.setString(6, "DOOR");
					else pst.setString(6, "STAIRS");
					pst.executeUpdate();
					pst.close();
				}
				for(Item i : model.getItems()) {
					pst = conn.prepareStatement("INSERT INTO ITEM(ID, MAP, ROOM_LABEL, LOCATION, URL_IMAGE, TITLE, WIDTH, HEIGHT, NATIONALITY, BEGIN_DATE, END_DATE, DATE, ITEM_LABEL) VALUES (?,?,?,ST_GeomFromText(?),?,?,?,?,?,to_timestamp(?,'HH24:MI:SS'),to_timestamp(?,'HH24:MI:SS'),?,?);");
					pst.setLong(1, i.getVertex_label());
					pst.setInt(2, mapID);
					// pst.setInt(3, i.getRoom().getLabel());
					pst.setLong(3, i.getRoom().getLabel()); // Modificado por Nacho Palacio 2025-05-24
					pst.setString(4, "POINT("+i.getVertex_xy().getX()+" "+i.getVertex_xy().getY()+")");
					pst.setString(5,i.getUrlIcon());
					pst.setString(6,i.getTitle());
//					pst.setDouble(7,i.getWidth());
//					pst.setDouble(8,i.getHeight());
					try {
						pst.setDouble(7,i.getWidth());
						pst.setDouble(8,i.getHeight());
					}
					catch (Exception e) {
						System.out.println(e);
						pst.setDouble(7,0.0);
						pst.setDouble(8,0.0);
					}
					pst.setString(9,i.getNationality());
					if(i.getBeginDate() != null && i.getBeginDate().matches("[0-2][0-9]:[0-5][0-9]")) {
						pst.setString(10, i.getBeginDate()+":00");
					}else {
						pst.setNull(10, Types.TIME);
					}
					if(i.getEndDate() != null && i.getEndDate().matches("[0-2][0-9]:[0-5][0-9]")) {
						pst.setString(11, i.getEndDate()+":00");
					}else {
						pst.setNull(11, Types.TIME);
					}
					pst.setString(12, i.getDate());
					pst.setString(13,i.getItemLabel());
					pst.executeUpdate();
				}
				
				
				
			}else {
				PreparedStatement pst = conn.prepareStatement("UPDATE MAP SET (WIDTH, HEIGHT, PIXEL_REPRESENTS_IN_METERS, DRAW_ICON_DIMENSION) = (?,?,?,?) WHERE NAME = ?;");
				pst.setInt(1, model.getMAP_W());
				pst.setInt(2, model.getMAP_H());
				pst.setDouble(3, model.getPixelRepresentsInMeters());
				pst.setInt(4, model.getDRAWING_ICON_DIMENSION());
				pst.setString(5, model.getName());
				pst.executeUpdate();
				
				pst = conn.prepareStatement("SELECT ID FROM MAP WHERE NAME = ?;");
				pst.setString(1, model.getName());
				ResultSet rs2 = pst.executeQuery();
				int mapID = -1;
				if (rs2.next()) {
					mapID = rs2.getInt(1);
				}
				rs2.close();
				pst = conn.prepareStatement("DELETE FROM SUBROOM_SEPARATOR WHERE MAP = "+mapID+";");
				pst.executeUpdate();
				pst = conn.prepareStatement("DELETE FROM CONNECTION WHERE MAP_CONN1 = "+mapID+";");
				pst.executeUpdate();
				pst = conn.prepareStatement("DELETE FROM CONNECTABLE WHERE MAP = "+mapID+";");
				pst.executeUpdate();
				pst = conn.prepareStatement("DELETE FROM ITEM WHERE MAP = "+mapID+";");
				pst.executeUpdate();
				pst = conn.prepareStatement("DELETE FROM ROOM WHERE MAP = "+mapID+";");
				pst.executeUpdate();
				for(Room r : model.getRooms()) {
					String walls = "LINESTRING(";
					for(Corner c : r.getCorners()) {
						walls += c.getVertex_xy().getX()+" "+c.getVertex_xy().getY()+",";
						//if(c != r.getCorners().get(r.getNumCorners()-1)) walls += ","; 
					}
					walls += r.getCorners().get(0).getVertex_xy().getX()+" "+r.getCorners().get(0).getVertex_xy().getY()+")";
					pst = conn.prepareStatement("INSERT INTO ROOM(LABEL, MAP, GEOM) VALUES (?,?,ST_MakePolygon(ST_GeomFromText(?)));");
					// pst.setInt(1, r.getLabel());
					pst.setLong(1, r.getLabel()); // Modificado por Nacho Palacio 2025-05-24
					pst.setInt(2, mapID);
					pst.setString(3, walls);
					//System.out.println(pst);
					pst.executeUpdate();					
					for (RoomSeparator sep: r.getRoomSeparators()) {
						pst = conn.prepareStatement("INSERT INTO SUBROOM_SEPARATOR(LABEL, ROOM_LABEL, MAP, GEOM) VALUES (?,?,?,ST_GeomFromText(?));");
						pst.setLong(1, sep.getVertex_label());
						// pst.setInt(2, r.getLabel());
						pst.setLong(2, r.getLabel()); // Modificado por Nacho Palacio 2025-05-24
						pst.setInt(3, mapID);
						String wall = "LINESTRING("+sep.getC1().getVertex_xy().getX()+" "+sep.getC1().getVertex_xy().getY()+",";
						wall += sep.getC2().getVertex_xy().getX()+" "+sep.getC2().getVertex_xy().getY()+")";
						pst.setString(4, wall);
						pst.executeUpdate();
					}		
					pst.close();
				}
				Set<Pair<Connectable, Connectable>> connected = new HashSet<Pair<Connectable, Connectable>>();
				for(Door d : model.getDoors()) {
					pst = conn.prepareStatement("INSERT INTO CONNECTABLE(ID, MAP, ROOM_LABEL, LOCATION, TYPE, URL_IMAGE) VALUES (?,?,?,ST_GeomFromText(?),?::conn_type,?);");
					pst.setInt(1, (int)d.getVertex_label());
					pst.setInt(2, mapID);
					if(d.getRoom() == null) {
						pst.setNull(3,Types.INTEGER);
					}else {
						// pst.setInt(3, d.getRoom().getLabel());
						pst.setLong(3, d.getRoom().getLabel()); // Modificado por Nacho Palacio 2025-05-24
					}
					//pst.setInt(4, mapID);
					pst.setString(4, "POINT("+d.getVertex_xy().getX()+" "+d.getVertex_xy().getY()+")");
					pst.setString(5, "DOOR");
					pst.setString(6,d.getUrlIcon());
					pst.executeUpdate();
					pst.close();
					for (Connectable c: d.getConnectedTo()) {
						Pair<Connectable, Connectable> pair = new Pair<Connectable, Connectable>(d, c);
						Pair<Connectable, Connectable> pairSwitched = new Pair<Connectable, Connectable>(c, d);
						
						if(!connected.contains(pair) && !connected.contains(pairSwitched) && c != null)
							connected.add(pair);						
					}					
				}				
				for(Stairs d : model.getStairs()) {
					pst = conn.prepareStatement("INSERT INTO CONNECTABLE(ID, MAP, ROOM_LABEL, LOCATION, TYPE, URL_IMAGE) VALUES (?,?,?,ST_GeomFromText(?),?::conn_type,?);");
					pst.setLong(1, d.getVertex_label());
					pst.setInt(2, mapID);
					if(d.getRoom() == null) {
						pst.setNull(3,Types.INTEGER);
					}else {
						// pst.setInt(3, d.getRoom().getLabel());
						pst.setLong(3, d.getRoom().getLabel()); // Modificado por Nacho Palacio 2025-05-24
					}
					//pst.setNull(4, Types.INTEGER);
					pst.setString(4, "POINT("+d.getVertex_xy().getX()+" "+d.getVertex_xy().getY()+")");
					pst.setString(5, "STAIRS");
					pst.setString(6,d.getUrlIcon());
					pst.executeUpdate();
					pst.close();
					for (Connectable c: d.getConnectedTo()) {
						Pair<Connectable, Connectable> pair = new Pair<Connectable, Connectable>(d, c);
						Pair<Connectable, Connectable> pairSwitched = new Pair<Connectable, Connectable>(c, d);
						
						if(!connected.contains(pair) && !connected.contains(pairSwitched) && c != null)
							connected.add(pair);						
					}					
				}
				for(Pair<Connectable, Connectable> pair: connected) {
					pst = conn.prepareStatement("INSERT INTO CONNECTION(ID_CONN1, MAP_CONN1, TYPE_CONN1, ID_CONN2, MAP_CONN2, TYPE_CONN2) VALUES (?,?,?::conn_type,?,?,?::conn_type);");
					pst.setInt(1, (int)((Drawable)pair.getF()).getVertex_label());
					pst.setInt(2, mapID);
					if(pair.getF() instanceof Door) pst.setString(3, "DOOR");
					else pst.setString(3, "STAIRS");					
					pst.setInt(4, (int)((Drawable)pair.getS()).getVertex_label());
					pst.setInt(5, mapID);
					if(pair.getS() instanceof Door) pst.setString(6, "DOOR");
					else pst.setString(6, "STAIRS");
					pst.executeUpdate();
					pst.close();
				}
				for(Item i : model.getItems()) {
					pst = conn.prepareStatement("INSERT INTO ITEM(ID, MAP, ROOM_LABEL, LOCATION, URL_IMAGE, TITLE, WIDTH, HEIGHT, NATIONALITY, BEGIN_DATE, END_DATE, DATE, ITEM_LABEL) VALUES (?,?,?,ST_GeomFromText(?),?,?,?,?,?,to_timestamp(?,'HH24:MI:SS'),to_timestamp(?,'HH24:MI:SS'),?,?);");
					pst.setLong(1, i.getVertex_label());
					pst.setInt(2, mapID);
					// pst.setInt(3, i.getRoom().getLabel());
					pst.setLong(3, i.getRoom().getLabel()); // Modificado por Nacho Palacio 2025-05-24
					pst.setString(4, "POINT("+i.getVertex_xy().getX()+" "+i.getVertex_xy().getY()+")");
					pst.setString(5,i.getUrlIcon());
					pst.setString(6,i.getTitle());
//					pst.setDouble(7,i.getWidth());
//					pst.setDouble(8,i.getHeight());
					try {
						pst.setDouble(7,i.getWidth());
						pst.setDouble(8,i.getHeight());
					}
					catch (Exception e) {
						System.out.println(e);
						pst.setDouble(7,0.0);
						pst.setDouble(8,0.0);
					}
					pst.setString(9,i.getNationality());
					if(i.getBeginDate() != null && i.getBeginDate().matches("[0-2][0-9]:[0-5][0-9]")) {
						pst.setString(10, i.getBeginDate()+":00");
					}else {
						pst.setNull(10, Types.TIME);
					}
					if(i.getEndDate() != null && i.getEndDate().matches("[0-2][0-9]:[0-5][0-9]")) {
						pst.setString(11, i.getEndDate()+":00");
					}else {
						pst.setNull(11, Types.TIME);
					}
					pst.setString(12, i.getDate());
					pst.setString(13,i.getItemLabel());
					pst.executeUpdate();
				}
				pst.close();
				
			}
			return res;						
		} catch (SQLException|NullPointerException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			throw e;
		}
	}

	
	public List<String> getMapNames(){
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT NAME FROM MAP;");
			List<String> names = new ArrayList<String>();
			while (rs.next()) {
			    names.add(rs.getString(1));
			}
			rs.close();
			return names;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return null;
	}
	
	public List<Integer> getMapIds(){
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT ID FROM MAP;");
			List<Integer> ids = new ArrayList<Integer>();
			while (rs.next()) {
				ids.add(rs.getInt(1));
			}
			rs.close();
			return ids;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return null;
	}
	
	public ResultSet getMap(String mapName) throws SQLException{
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT * FROM MAP WHERE NAME = '"+mapName+"';");
			return rs;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	
	public void deleteMap(int mapID) throws SQLException{
		try {
			PreparedStatement pst = conn.prepareStatement("DELETE FROM MAP WHERE ID = "+mapID+";");
			pst.executeUpdate();
			pst = conn.prepareStatement("DELETE FROM CONNECTABLE WHERE MAP = "+mapID+";");
			pst.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	
	public ResultSet getRoomsOfMap(int mapID) throws SQLException{
		try {
			Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
			ResultSet rs = st.executeQuery("SELECT ST_AsText(geom),ST_AsText(ST_PointOnSurface(geom)) AS CENTER,* FROM ROOM WHERE MAP = "+mapID+" ORDER BY ST_Area(geom) DESC;");
			return rs;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	
	public List<Integer> getRoomLabelsOfMap(int mapID) throws SQLException{
		try {
			Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
			ResultSet rs = st.executeQuery("SELECT ID FROM ROOM WHERE MAP = "+mapID+" ORDER BY ID ASC;");
			List<Integer> labels = new ArrayList<Integer>();
			while (rs.next()) {
				labels.add(rs.getInt(1));
			}
			return labels;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	
	public List<Integer> getRoomLabelsOfMap(String mapName) throws SQLException{
		try {
			Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
			ResultSet rs = st.executeQuery("SELECT ROOM.LABEL FROM ROOM,MAP WHERE ROOM.MAP = MAP.ID AND MAP.NAME = '"+mapName+"' ORDER BY ID ASC;");
			List<Integer> labels = new ArrayList<Integer>();
			while (rs.next()) {
				labels.add(rs.getInt(1));
			}
			return labels;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
		
	public ResultSet getConnectablesOfMap(int mapID) throws SQLException{
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT ST_AsText(location),* FROM CONNECTABLE WHERE MAP = "+mapID+";");
			return rs;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	
	public ResultSet getConnectionsOfConnectables(int mapID) throws SQLException{
		try {
			PreparedStatement st = conn.prepareStatement("SELECT * FROM CONNECTION WHERE MAP_CONN1 = "+mapID+";");
			ResultSet rs = st.executeQuery();
			return rs;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	
	public ResultSet getItemsOfMap(int mapID) throws SQLException{
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT ST_AsText(location),to_char(begin_date,'HH24:MI') as begin_date_string, to_char(end_date,'HH24:MI') as end_date_string,* FROM ITEM WHERE MAP = "+mapID+";");
			return rs;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	
	public int getNumItemsOfMap(int mapID){
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ITEM WHERE MAP = "+mapID+";");
			if(rs.next()) return rs.getInt(1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	
	public ResultSet getSeparatorsOfMap(int mapID) throws SQLException{
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT ST_AsText(geom),* FROM SUBROOM_SEPARATOR WHERE MAP = "+mapID+";");
			return rs;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	
	public double getTotalMapArea(int mapID) {
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT SUM(ST_Area(geom)) FROM ROOM WHERE MAP = "+mapID+";");
			if(rs.next()) return rs.getDouble(1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	
	public double getAreaOfRoom(int mapID, int roomLabel) {
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT ST_Area(geom) FROM ROOM WHERE LABEL = "+roomLabel+" AND MAP = "+mapID+";");
			if(rs.next() && rs.isFirst() && rs.isLast()) return rs.getDouble(1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	
	public int getNumberItemsInRoom(int mapID, int roomLabel) {
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ITEM WHERE ROOM_LABEL = "+roomLabel+" AND MAP = "+mapID+";");
			if(rs.next() && rs.isFirst() && rs.isLast()) return rs.getInt(1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	
	public ResultSet getItemsOfMap(int mapID, int roomLabel) throws SQLException{
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT ST_AsText(location),to_char(begin_date,'HH24:MI') as begin_date_string, to_char(end_date,'HH24:MI') as end_date_string,* FROM ITEM WHERE ROOM_LABEL = "+roomLabel+" AND MAP = "+mapID+";");
			return rs;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	
	public List<String> getNamesOfTables() throws SQLException{
		try {
			List<String> names = Arrays.asList("map","room","subroom_separator","connectable","connection","item");
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("select table_name from information_schema.tables where table_schema = 'public' and table_type = 'BASE TABLE' and table_name <> 'spatial_ref_sys';");
			List<String> results = new ArrayList<String>();
			while (rs.next()) {
				if(names.contains(rs.getString(1))) {
					results.add(rs.getString(1));
				}
			}
			rs.close();
			return results;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	
	public List<String> getNamesOfSimulationTables() throws SQLException{
		try {
			List<String> names = Arrays.asList("simulation","user_sim","visit","item_observation","map","item","room");
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("select table_name from information_schema.tables where table_schema = 'public' and table_type = 'BASE TABLE' and table_name <> 'spatial_ref_sys';");
			List<String> results = new ArrayList<String>();
			while (rs.next()) {
				if(names.contains(rs.getString(1))) {
					results.add(rs.getString(1));
				}
			}
			rs.close();
			return results;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	
	public ResultSet runQuery(String query) throws SQLException{
		try {
			Statement st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
			ResultSet rs = st.executeQuery(query);
			return rs;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			throw e;
		}
	}
	
	public List<Pair<String,String>> getAttributesOfTables(List<String> tables) {
		try {
			List<Pair<String,String>> attributes = new ArrayList<Pair<String,String>>();
			for(String table : tables) {
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery("SELECT column_name,data_type FROM information_schema.columns WHERE table_name = '"+table+"';");
				while (rs.next()) {
					attributes.add(new Pair<String,String>(rs.getString(1),table+" "+rs.getString(2)));
				}
				rs.close();
			}
			return attributes;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return null;
	}
	
	public boolean registerSimulation(int timeAvailableUser, int delayObservingItem, double userSpeed, double kmToPixel, int timeOnStairs) {
		if(loadedMapName != null) {
			try {
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery("SELECT ID FROM MAP WHERE NAME = '"+loadedMapName+"';");
				Date d = new Date();
				if(rs.next()) {
					PreparedStatement pst = conn.prepareStatement("INSERT INTO SIMULATION(MAP_NAME,MAP_ID,TIME_AVAILABLE_USER,DELAY_OBSERVING_ITEM,USER_SPEED,KM_TO_PIXEL,TIME_ON_STAIRS,BEGIN_TIME) VALUES (?,?,?,?,?,?,?,?);");
					pst.setString(1, loadedMapName);
					pst.setInt(2, rs.getInt(1));
					pst.setInt(3, timeAvailableUser);
					pst.setInt(4, delayObservingItem);
					pst.setDouble(5, userSpeed);
					pst.setDouble(6, kmToPixel);
					pst.setInt(7, timeOnStairs);
					pst.setTimestamp(8, new Timestamp(d.getTime()));
					pst.executeUpdate();
					pst.close();
				}/*else {
					PreparedStatement pst = conn.prepareStatement("INSERT INTO SIMULATION(MAP_NAME,TIMESTAMP) VALUES (?,?);");
					pst.setString(1, loadedMapName);
					pst.setTimestamp(2, new Timestamp(d.getTime()));
					pst.executeUpdate();
					pst.close();
				}*/
				return true;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}else {
			return false;
		}
	}
	
	public boolean registerVisit(int room, int user, boolean userIsSpecial) {
		try {
			if(loadedMapName != null) {
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery("SELECT MAX(ID) FROM SIMULATION WHERE MAP_NAME = '"+loadedMapName+"';");
				if(rs.next()) {
					
					PreparedStatement pst = conn.prepareStatement("INSERT INTO USER_SIM(ID,IS_SPECIAL,SIMULATION) VALUES (?,?,?);");
					try {
						pst.setInt(1, user);
						pst.setBoolean(2, userIsSpecial);
						pst.setInt(3, rs.getInt(1));
						pst.executeUpdate();
					}catch (SQLException e) {
						// TODO Auto-generated catch block
//						e.printStackTrace();
					}
					
					pst = conn.prepareStatement("INSERT INTO VISIT(ID,SIMULATION,ROOM_LABEL,MAP,USER_ID) VALUES ((SELECT COUNT(*) FROM VISIT WHERE SIMULATION = ? AND USER_ID = ?)+1,?,?,?,?);");
					pst.setInt(1, rs.getInt(1));
					pst.setInt(2, user);
					pst.setInt(3, rs.getInt(1));
					pst.setInt(4, room);
					pst.setInt(5, loadedMapId);
					pst.setInt(6, user);
					pst.executeUpdate();
					pst.close();
					return true;
				}else {
					return false;
				}
			}else {
				return false;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public void initUserPath(int user, String position) {
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT MAX(ID) FROM SIMULATION WHERE MAP_NAME = '"+loadedMapName+"';");
			if(rs.next()) {
				PreparedStatement pst = conn.prepareStatement("UPDATE USER_SIM SET PATH = (SELECT ST_MakeLine(ST_GeomFromText(?))) WHERE ID = ? AND SIMULATION = ?;");
				double x = Double.valueOf(position.split(", ")[0]).doubleValue();
				double y = Double.valueOf(position.split(", ")[1]).doubleValue();
				pst.setString(1, "POINT("+x+" "+y+")");
				pst.setInt(2, user);
				pst.setInt(3, rs.getInt(1));
				pst.executeUpdate();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addPositionToPath(int user, String position, int room) {
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT MAX(ID) FROM SIMULATION WHERE MAP_NAME = '"+loadedMapName+"';");
			if(rs.next()) {
				double x = Double.valueOf(position.split(", ")[0]).doubleValue();
				double y = Double.valueOf(position.split(", ")[1]).doubleValue();
				PreparedStatement pst = conn.prepareStatement("DO $$ BEGIN IF EXISTS (SELECT * FROM USER_SIM WHERE ID = "+user+" AND SIMULATION = "+rs.getInt(1)+" AND PATH IS NULL) THEN UPDATE USER_SIM SET PATH = (SELECT ST_MakeLine(ST_GeomFromText('POINT("+x+" "+y+")'))) WHERE ID = "+user+" AND SIMULATION = "+rs.getInt(1)+"; ELSE UPDATE USER_SIM SET PATH = ST_AddPoint(PATH,ST_GeomFromText('POINT("+x+" "+y+")')) WHERE ID = "+user+" AND SIMULATION = "+rs.getInt(1)+"; END IF; END$$;");
				pst.executeUpdate();
				pst = conn.prepareStatement("DO $$ DECLARE VISIT_ID INTEGER; BEGIN SELECT ID INTO VISIT_ID FROM VISIT WHERE SIMULATION = "+rs.getInt(1)+" AND USER_ID = "+user+" AND ROOM_LABEL = "+room+" ORDER BY ID DESC LIMIT 1; IF EXISTS (SELECT * FROM VISIT WHERE ID = VISIT_ID AND SIMULATION = "+rs.getInt(1)+" AND USER_ID = "+user+" AND ROOM_LABEL = "+room+" AND PATH IS NULL) THEN UPDATE VISIT SET PATH = (SELECT ST_MakeLine(ST_GeomFromText('POINT("+x+" "+y+")'))) WHERE ID = VISIT_ID AND USER_ID = "+user+" AND SIMULATION = "+rs.getInt(1)+"; ELSE UPDATE VISIT SET PATH = ST_AddPoint(PATH,ST_GeomFromText('POINT("+x+" "+y+")')) WHERE ID = VISIT_ID AND USER_ID = "+user+" AND SIMULATION = "+rs.getInt(1)+"; END IF; END$$;");
				pst.executeUpdate();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void registerVisitDuration(int room, int user, double duration) {
		try {
			if(loadedMapName != null) {
//				PreparedStatement pst = conn.prepareStatement(
//				ResultSet rs = st.executeQuery("SELECT ID FROM VISIT WHERE SIMULATION = (SELECT MAX(ID) FROM SIMULATION WHERE MAP_NAME = ?) AND USER_ID = ? ORDER BY ID DESC LIMIT 1");
//				if(rs.next()) {										
					PreparedStatement pst = conn.prepareStatement("UPDATE VISIT SET DURATION = ? WHERE SIMULATION = (SELECT MAX(ID) FROM SIMULATION WHERE MAP_NAME = ?) AND ID = (SELECT ID FROM VISIT WHERE SIMULATION = (SELECT MAX(ID) FROM SIMULATION WHERE MAP_NAME = ?) AND USER_ID = ? AND ROOM_LABEL = ? ORDER BY ID DESC LIMIT 1) AND USER_ID = ?;");
					pst.setDouble(1, duration);
					pst.setString(2, loadedMapName);
					pst.setString(3, loadedMapName);
					pst.setInt(4, user);
					pst.setInt(5, room);
					pst.setInt(6, user);
					pst.executeUpdate();					
					pst.close();
//				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean registerItemObservation(int room, int user, int itemID) {
		try {
			if(loadedMapName != null) {
				PreparedStatement pst = conn.prepareStatement("SELECT VISIT.ID,VISIT.SIMULATION,SIMULATION.MAP_ID FROM VISIT,SIMULATION WHERE VISIT.SIMULATION = SIMULATION.ID AND SIMULATION.MAP_NAME = ? AND VISIT.USER_ID = ? AND VISIT.ROOM_LABEL = ? ORDER BY VISIT.ID DESC LIMIT 1;");
				pst.setString(1, loadedMapName);
				pst.setInt(2, user);
				pst.setInt(3, room);
				ResultSet rs = pst.executeQuery();
				if(rs.next()) {
										
					pst = conn.prepareStatement("INSERT INTO ITEM_OBSERVATION(ID,VISIT,USER_ID,SIMULATION,ITEM_ID,MAP) VALUES ((SELECT COUNT(*) FROM ITEM_OBSERVATION WHERE SIMULATION = ? AND VISIT = ?),?,?,?,?,?);");
					pst.setInt(1, rs.getInt(2));
					pst.setInt(2, rs.getInt(1));
					pst.setInt(3, rs.getInt(1));
					pst.setInt(4, user);
					pst.setDouble(5, rs.getInt(2));
					pst.setInt(6, itemID);
					pst.setInt(7, rs.getInt(3));
					pst.executeUpdate();
					pst.close();
					return true;
				}else {
					return false;
				}
			}else {
				return false;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public List<String> getSimulations(){
		try {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery("SELECT ID,MAP_NAME,BEGIN_TIME FROM SIMULATION ORDER BY ID DESC;");
			List<String> simList = new ArrayList<String>();
			while(rs.next()) {
				simList.add(rs.getInt(1)+"Simulation: "+Integer.toString(rs.getInt(1))+" || Map: "+rs.getString(2)+" || Date: "+rs.getTimestamp(3).toString());
			}
			simList.set(0, simList.getFirst()+" (last simulation)");
			return simList;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void saveLoadedMap(String name,int id) {
		loadedMapName = name;
		loadedMapId = id;
	}
	
	public String getLoadedMapName() {
		return loadedMapName;
	}
	
	public int getLoadedMapId() {
		return loadedMapId;
	}
}
