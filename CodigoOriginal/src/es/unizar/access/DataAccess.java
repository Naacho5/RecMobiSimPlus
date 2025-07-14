package es.unizar.access;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Properties;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import es.unizar.util.Literals;

/**
 * Access to the values of the properties stored in to file. Getters and setters.
 *
 * @author Maria del Carmen Rodriguez-Hernandez and Alejandro Piedrafita Barrantes
 *
 */
public class DataAccess {

    protected File file;
    protected Properties properties;
    protected Monitor dataAccessMon;

    public DataAccess(File file) {
    	
    	if(file != null && file.exists() && file.getName().endsWith(".svg")) {
    		int i = 1;
    		this.file = new File("resources" + File.separator + "tmp" + File.separator + "file"+ i + ".txt");
    		try {
				while(!this.file.createNewFile()) {
					i++;
					this.file = new File("resources" + File.separator + "tmp" + File.separator + "file"+ i + ".txt");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}else {
    		this.file = file;
    	}
    	
//        this.file = file;
        this.properties = new Properties();
        this.dataAccessMon = null;
        
        if (this.file != null && this.file.exists() && !file.getName().endsWith(".svg")) {
        	loadProperties();
        }
    }
    
    public void clearProperties() {
    	properties.clear();
    }
    
    /**
     * READING OPERATIONS
     */

	/**
     * Gets all the values of the properties of the scheme file.
     *
     */
    public void loadProperties() {
    	
    	InputStream input = null;
        
    	try {
        	//initialTimeInput = System.currentTimeMillis();
            input = new FileInputStream(this.file.getAbsoluteFile());
            //finalTimeInput = System.currentTimeMillis();
            
            clearProperties();
            
            //initialTimeLoad = System.currentTimeMillis();
            properties.load(input);
            //finalTimeLoad = System.currentTimeMillis();
            
            //initialTimeGetProperty = System.currentTimeMillis();
            //Monitor propertyValueMonitor = MonitorFactory.start("propertyValueMonitor");
            //propertyValue = properties.getProperty(nameProperty);
            //propertyValueMonitor.stop();
            //System.out.println(propertyValueMonitor);
            //finalTimeGetProperty = System.currentTimeMillis();
            
            //System.out.println("[***] GET PROPERTY VALUE TIEMPOS: input(" + (finalTimeInput-initialTimeInput) + "); load(" + 
            //		(finalTimeLoad-initialTimeLoad) + "); getProperty(" + (finalTimeGetProperty-initialTimeGetProperty) + ");\n");
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Gets the value of a property of the scheme file.
     *
     * @param nameProperty Name of the property.
     * @return The value of the property specified.
     */
    public String getPropertyValue(String nameProperty) {
    	dataAccessMon = MonitorFactory.start("getPropertyValue");
        String propertyValue = properties.getProperty(nameProperty);
        dataAccessMon.stop();
        
        return propertyValue;
    }

    /**
     * WRITING OPERATIONS
     */
    
    /**
     * Stores all the values of the properties of the scheme file.
     *
     */
    public void storeProperties() {
    	
    	OutputStream output = null;
        
    	try {
    		
            output = new FileOutputStream(this.file.getAbsoluteFile());
            
            properties.store(output, null);
            
            properties.clear();
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Gets the value of a property of the scheme file.
     * 
     * @param keyProperty
     * @param valueProperty
     */
    public void setPropertyValue(String keyProperty, String valueProperty) {
    	// ANOTHER MONITOR COULD BE ADDED TO MONITOR THE STORE OPS
    	//storeDataAccessMon = MonitorFactory.start("getPropertyValue");
        properties.setProperty(keyProperty, valueProperty);
        // ANOTHER MONITOR COULD BE ADDED TO MONITOR THE STORE OPS
        //storeDataAccessMon.stop();
    }
    
    
    /**
     * GETTERS AND SETTERS
     */
    
    /**
     * File getter.
     * 
     * @return File
     */
    public File getFile() {
		return file;
	}

    /**
     * File setter.
     * 
     * @param file
     */
	public void setFile(File file) {
		this.file = file;
		if(file != null && file.exists() && file.getName().endsWith(".svg")) {
    		int i = 1;
    		this.file = new File("resources" + File.separator + "tmp" + File.separator + "file"+ i + ".txt");
    		try {
				while(!this.file.createNewFile()) {
					i++;
					this.file = new File("resources" + File.separator + "tmp" + File.separator + "file"+ i + ".txt");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		new SVGParserSimulation(this,file);
    	}
	}
    
    /**
     * Monitor getter.
     * 
     * @return monitor
     */
	public Monitor getDataAccessMon() {
		return dataAccessMon;
	}

	/**
	 * Monitor setter.
	 * 
	 * @param dataAccessMon
	 */
	public void setDataAccessMon(Monitor dataAccessMon) {
		this.dataAccessMon = dataAccessMon;
	}
	
	/**
	 * Reset monitor (if created).
	 */
	public void reset() {
		if (dataAccessMon != null) {
			dataAccessMon.reset();
		}
	}

    /**
     * Prints all the properties loaded.
     * Añadido por Nacho Palacio 2025-04-13.
     */
    public void printAllProperties() {
        for (String key : properties.stringPropertyNames()) {
            System.out.println(key + " = " + properties.getProperty(key));
        }
    }

}
