package es.unizar.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import es.unizar.access.DataAccessRecommendersFile;
import es.unizar.access.DataAccessRoomFile;
import es.unizar.epidemic.EpidemicConfiguration;
import es.unizar.gui.simulation.Simulation;
import es.unizar.recommendation.path.ExhaustivePath;
import es.unizar.recommendation.path.NearestPath;
import es.unizar.recommendation.path.Path;
import es.unizar.recommendation.path.RandomPath;
import es.unizar.util.EditorLiterals;
import es.unizar.util.Literals;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

/**
 * Parameter settings.
 * 
 * @author Maria del Carmen Rodriguez-Hernandez and Alejandro Piedrafita Barrantes
 */
public class Configuration extends javax.swing.JDialog {

	private static final long serialVersionUID = 1L;
	public static Simulation simulation;
	
	public static final int X_CONFIG = 1080; /* silarri, 2022-07-13. */
	//public static final int X_CONFIG = 1070;
	public static final int Y_CONFIG = 850; /* silarri, 2022-07-13. */
	//public static final int Y_CONFIG = 830;
	
	DataAccessRecommendersFile recommenders;

	public JTextField simulationDurationTextField;
	public JTextField immunePopulationTextField;
	public JTextField superSpreaderProbabilityTextField;	

	/**
	 * Creates new form Configuration
	 * @wbp.parser.constructor
	 */
	public Configuration(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
		setAlwaysOnTop(true);
		// Parameter configuration form.
		setConfigurationPreferredSize();
		
		/* Modified by silarri (2022-07-15). */
		ImageIcon appImg = new ImageIcon(Literals.IMAGES_PATH + "configuration.png");
		Image img = appImg.getImage();
		setIconImage(img);
		//setIconImage(Toolkit.getDefaultToolkit().getImage(Configuration.class.getResource("/es/unizar/images/configuration.png")));
		setTitle("Configuration");

		initComponents();

		pack();

		// Center in the parent
		/*Dimension size = getSize();
		Rectangle parentBounds = parent.getBounds();
		int x = Math.max(0, parentBounds.x + (parentBounds.width - size.width) / 2);
		int y = Math.max(0, parentBounds.y + (parentBounds.height - size.height) / 2);
		setLocation(new Point(x, y));*/
		
		/*GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
        int x = (int) rect.getMaxX() - this.getWidth();
        int y = 0;
        setLocation(x,y);*/
		setDialogLocation();

		// The simulation is activate (start, stop and pause options).
		MainSimulator.startMenuItem.setEnabled(true);
		//MainMuseumSimulator.stopMenuItem.setEnabled(true);
	}
	
	public Configuration(int timeAvailableUser, int delayObservingPainting, double timeForIteration, double screenRefreshTime, double timeForThePaths, double userVelocity, 
			double kmToPixel, int ttl, int timeOnStairs, int minimumTimeToUpdateRecommendation, int communicationRange, int maxKnowledgeBaseSize, int communicationBandwidth, 
			int latencyOfTransmission, int timeToChangeMood, boolean useFixedSeed, long seed,
			int numberOfSpecialUser, int numberOfNonSpecialUser, String nonSpecialUserPaths, String pathStrategy, String recommendationAlgorithm, float thresholdRecommendation, 
			double thresholdSimilarity, int howMany, String networkType, String propagationStrategy, double probabilityUserDisobedience, int numberVoteReceived, boolean generateUserPaths,
			double simulationDurationMinutes) {

		try {
			
			recommenders = new DataAccessRecommendersFile(new File(Literals.RECOMMENDERS_FILE));
			
			// Validation of fields:
			if ( checkParametersForSimulation(timeAvailableUser, delayObservingPainting, timeForIteration, screenRefreshTime, timeForThePaths, 
					userVelocity, kmToPixel, ttl, timeOnStairs, minimumTimeToUpdateRecommendation, communicationRange, maxKnowledgeBaseSize,
					communicationBandwidth, latencyOfTransmission, timeToChangeMood, simulationDurationMinutes)
					&& 
					checkUsersInfo(numberOfSpecialUser, numberOfNonSpecialUser, nonSpecialUserPaths, pathStrategy)
					&& 
					checkAlgorithmAndNetworkParams(recommendationAlgorithm, thresholdRecommendation, thresholdSimilarity, howMany, networkType, 
							propagationStrategy, probabilityUserDisobedience, numberVoteReceived)) {
				
				es.unizar.epidemic.EpidemicConfiguration epidemicConfig = es.unizar.epidemic.EpidemicConfiguration.getInstance();
            	epidemicConfig.setSimulationDuration((int) simulationDurationMinutes);

				// Build a floor panel but including the users.
				MainSimulator.floorPanelCombined = new FloorPanelCombined(MainSimulator.DRAWING_WIDTH, MainSimulator.DRAWING_HEIGHT);
				MainSimulator.frmSimulator.getContentPane().add(MainSimulator.floorPanelCombined);
	
				// Load combined floors (4 and 5):
				MainSimulator.loadFloorCombined();
				
//				MainSimulator.startMenuItem.setEnabled(true);
//				
//				// IF ideal algorithms
//				if (!recommenders.checkRecommenderUsesP2P(recommendationAlgorithm)) {
//					networkType = "Centralized (Centralized)";
//					propagationStrategy = "";
//					
//				}
//	
//				// Build a simulation object:
//				simulation = new Simulation(timeAvailableUser, delayObservingPainting, timeForIteration, screenRefreshTime, timeForThePaths, userVelocity, kmToPixel, ttl, timeOnStairs,
//						minimumTimeToUpdateRecommendation, communicationRange, maxKnowledgeBaseSize, communicationBandwidth, latencyOfTransmission, numberOfSpecialUser, numberOfNonSpecialUser,
//						nonSpecialUserPaths, pathStrategy, recommendationAlgorithm, thresholdRecommendation, howMany, propagationStrategy, probabilityUserDisobedience, numberVoteReceived,
//						thresholdSimilarity, networkType, timeToChangeMood, useFixedSeed, seed);
//				
//	
//				// Generate a path for each non-RS user.
//				if (generateUserPaths) {
//					Path strategy = null;
//					// Apply the specified path strategy in the Configuration form.
//					if (pathStrategy.equalsIgnoreCase("Near POI (NPOI)")) {
//						strategy = new NearestPath();
//					} else if (pathStrategy.equalsIgnoreCase("Exhaustive visit (ALL)")) {
//						strategy = new ExhaustivePath();
//					} else if (pathStrategy.equalsIgnoreCase("Completely-random (FULLY-RAND)")) {
//						strategy = new RandomPath();
//					}
//					simulation.generate_non_special_user_path(strategy);
//				}
	
				//this.dispose();
			} else {
				JOptionPane.showMessageDialog(this, "You will need to fill in all the fields, except: user disobedience.", "Field validation problems", JOptionPane.ERROR_MESSAGE);
			}
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(this, "You will need to fill in all the fields, except: user disobedience.", "Field validation problems", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	

	/**
	 * Saves the changes.
	 * 
	 * @param evt
	 * @throws IOException
	 */
	private void saveActionPerformed(java.awt.event.ActionEvent evt) throws IOException {
		
		try {
			
			int i = 1;
			File f = new File("resources" + File.separator + "tmp" + File.separator + "file"+ i + ".txt");
			while(f.delete()) {
				i++;
				f = new File("resources" + File.separator + "tmp" + File.separator + "file"+ i + ".txt");
			}
			
			// Parameters for the simulation:
			// Time available for the user [hour].
			int timeAvailableUser = Integer.valueOf(timeAvailableForUserTextField.getText()).intValue();
			// Delay observing item [seconds].
			int delayObservingItem = Integer.valueOf(delayObservingItemTextField.getText()).intValue();
			// Real time per iteration [seconds].
			double timeForIteration = Double.valueOf(timeForIterationTextField.getText()).doubleValue();
			// Iteration time/Screen refresh time [seconds].
			double screenRefreshTime = Double.valueOf(screenRefreshTimeTextField.getText()).doubleValue();
			// Time for the paths [hour].
			double timeForThePaths = Double.valueOf(timeForThePathsTextField.getText()).doubleValue();
			// User speed [km/h].
			double userSpeed = Double.valueOf(userSpeedTextField.getText()).doubleValue();
			// 1 km represents in pixels [pixel].
			double kmToPixel = Double.valueOf(kmToPixelTextField.getText()).doubleValue();
			// TTL of the items to propagate [seconds].
			int ttl = Integer.valueOf(ttlTextField.getText()).intValue(); // Propagation of items
			// Time on the stairs [seconds].
			int timeOnStairs = Integer.valueOf(timeOnStairsTextField.getText()).intValue();
			// Minimum time to update recommendation [seconds].
			int minimumTimeToUpdateRecommendation = Integer.valueOf(minimumTimeToUpdateRecommendationTextField.getText()).intValue();
			// Communication range [m].
			int communicationRange = Integer.valueOf(communicationRangeTextField.getText()).intValue();
			// Maximum knowledge base size [Mb].
			int maxKnowledgeBaseSize = Integer.valueOf(maximumKnowledgeBaseSizeTextField.getText()).intValue();
			// Communication bandwidth [Mbps].
			int communicationBandwidth = Integer.valueOf(communicationBandwidthTextField.getText()).intValue();
			// Latency of transmission [s].
			int latencyOfTransmission = Integer.valueOf(latencyOfTransmissionTextField.getText()).intValue();
			// Time to change the mood [seconds].
			int timeToChangeMood = Integer.valueOf(timeChangeMoodTextField.getText()).intValue();
			// Is seed selected
			boolean useFixedSeed = chckbxUseFixedSeed.isSelected();
			// Seed value for random numbers
			long seed = Long.valueOf(seedNumber.getText()).longValue();
	
			// Parameters for the experimentation:
			// Number of RS users.
			int numberOfSpecialUser = Integer.valueOf(numberOfSpecialUsersTextField.getText()).intValue();
			// Number of non-RS users.
			int numberOfNonSpecialUser = Integer.valueOf(numberOfNonSpecialUsersTextField.getText()).intValue();
			// File name of non-RS users.
			String nonSpecialUserPaths = Literals.PATH_MAPS + nonSpecialUserPathsJTextField.getText();
			// Path strategy of non-RS users.
			String pathStrategy = (String) pathStrategyComboBox.getSelectedItem();
			// Recommendation algorithm.
			String recommendationAlgorithm = (String) recommendationAlgorithmComboBox.getSelectedItem();
			// Threshold of recommendation.
			float thresholdRecommendation = Float.valueOf(thresholdRecommendationTextField.getText()).floatValue();
			// Threshold of similarity.
			double thresholdSimilarity = Double.valueOf(thresholdSimilarityTextField.getText()).doubleValue();
			// How many items to recommend.
			int howMany = Integer.valueOf(howManyTextField.getText()).intValue();
			// Type of network.
			String networkType = (String) typeNetworkComboBox.getSelectedItem();
			// Propagation strategy:
			String propagationStrategy = (String) propagationStrategyComboBox.getSelectedItem();
			// Probability of user disobedience.
			double probabilityUserDisobedience = Double.valueOf(probabilityUserDisobedienceTextField.getText()).doubleValue();
			// Number of votes received.
			int numberVoteReceived = Integer.valueOf(numberVoteReceivedTextField.getText()).intValue(); // Propagation of
																										// items

			// Añadido por Nacho Palacio 2025-07-16
			String epidemicModel = (String) epidemicModelComboBox.getSelectedItem();
			int initialInfected = Integer.valueOf(initialInfectedTextField.getText()).intValue();
			double maskCompliance = Double.valueOf(maskComplianceTextField.getText()).doubleValue();

			double simulationDurationMinutes = Double.valueOf(simulationDurationTextField.getText()).doubleValue();

			es.unizar.epidemic.EpidemicConfiguration epidemicConfig = es.unizar.epidemic.EpidemicConfiguration.getInstance();
			epidemicConfig.setSelectedModel(epidemicModel);
			epidemicConfig.setInitialInfectedUsers(initialInfected);
			epidemicConfig.setMaskComplianceRate(maskCompliance);

			epidemicConfig.setSimulationDuration((int) simulationDurationMinutes);

			// Añadido por Nacho Palacio 2025-09-14
			try {
				double defaultVentRate = Double.parseDouble(defaultVentilationRateTextField.getText());
				double virusDecay = Double.parseDouble(virusDecayRateTextField.getText());
				double maskExhEff = Double.parseDouble(maskExhalationEffTextField.getText());
				double maskInhEff = Double.parseDouble(maskInhalationEffTextField.getText());
				
				epidemicConfig.setDefaultVentilationRate(defaultVentRate);
				epidemicConfig.setVirusDecayRate(virusDecay);
				epidemicConfig.setMaskExhalationEfficiency(maskExhEff);
				epidemicConfig.setMaskInhalationEfficiency(maskInhEff);

				try {
					double immunePopulationFraction = Double.parseDouble(immunePopulationTextField.getText());
					double superSpreaderProbability = Double.parseDouble(superSpreaderProbabilityTextField.getText());
					
					epidemicConfig.setImmunePopulationFraction(immunePopulationFraction);
					epidemicConfig.setSuperSpreaderProbability(superSpreaderProbability);

					System.out.printf("🔧 Configuration.java: Parámetros super-spreader configurados:\n");
					System.out.printf("   - Probabilidad super-spreader enviada: %.1f%%\n", superSpreaderProbability * 100);
					
					EpidemicConfiguration verify = EpidemicConfiguration.getInstance();
					System.out.printf("   - Probabilidad almacenada en EpidemicConfiguration: %.1f%%\n", 
                     verify.getSuperSpreaderProbability() * 100);
					 
				} catch (NumberFormatException e) {
					System.err.println("Error parsing immunity/superspreader parameters, using defaults: " + e.getMessage());
				}

				String selectedModel = (String) epidemicModelComboBox.getSelectedItem();

				switch (selectedModel) {
					case "SIMPLE_PROXIMITY":
						if (maxTransmissionDistanceTextField != null && 
							baseTransmissionProbTextField != null && 
							minContactDurationTextField != null) {
							
							double maxDist = Double.parseDouble(maxTransmissionDistanceTextField.getText());
							double baseProb = Double.parseDouble(baseTransmissionProbTextField.getText());
							int minDuration = Integer.parseInt(minContactDurationTextField.getText());
							
							epidemicConfig.setMaxTransmissionDistance(maxDist);
							epidemicConfig.setBaseTransmissionProbability(baseProb);
							epidemicConfig.setMinContactDuration(minDuration);
						}
						break;
					case "AEROSOL_PENG":
						if (quantaEmissionRateTextField != null && 
							breathingRateTextField != null && 
							depositionRateTextField != null) {
							
							double quantaRate = Double.parseDouble(quantaEmissionRateTextField.getText());
							double breathRate = Double.parseDouble(breathingRateTextField.getText());
							double depRate = Double.parseDouble(depositionRateTextField.getText());
							
							epidemicConfig.setQuantaEmissionRate(quantaRate);
							epidemicConfig.setBreathingRate(breathRate);
							epidemicConfig.setDepositionRate(depRate);
						}
						break;
					case "AEROSOL_LELIEVELD":
						System.out.println("Selected model: AEROSOL_LELIEVELD");
						if (viralLoadHighTextField != null && 
							viralLoadSuperTextField != null && 
							infectiousDoseTextField != null && 
							depositionProbabilityTextField != null) {
							
							double viralHigh = Double.parseDouble(viralLoadHighTextField.getText());
							double viralSuper = Double.parseDouble(viralLoadSuperTextField.getText());
							double infDose = Double.parseDouble(infectiousDoseTextField.getText());
							double depProb = Double.parseDouble(depositionProbabilityTextField.getText());
							
							epidemicConfig.setViralLoadHigh(viralHigh);
							epidemicConfig.setViralLoadSuper(viralSuper);
							epidemicConfig.setInfectiousDose(infDose);
							epidemicConfig.setDepositionProbability(depProb);
						}
						break;
				}
			} catch (NumberFormatException e) {
				System.err.println("Error parsing epidemic parameters, using defaults: " + e.getMessage());
			}
	
			// Validation of fields:
			if ( checkParametersForSimulation(timeAvailableUser, delayObservingItem, timeForIteration, screenRefreshTime, timeForThePaths, 
					userSpeed, kmToPixel, ttl, timeOnStairs, minimumTimeToUpdateRecommendation, communicationRange, maxKnowledgeBaseSize,
					communicationBandwidth, latencyOfTransmission, timeToChangeMood, simulationDurationMinutes)
					&& 
					checkUsersInfo(numberOfSpecialUser, numberOfNonSpecialUser, nonSpecialUserPaths, pathStrategy)
					&& 
					checkAlgorithmAndNetworkParams(recommendationAlgorithm, thresholdRecommendation, thresholdSimilarity, howMany, networkType, 
							propagationStrategy, probabilityUserDisobedience, numberVoteReceived)
					&& // Añadido por Nacho Palacio 2025-07-16
        			checkEpidemicParameters(epidemicModel, initialInfected, maskCompliance, 
						Double.parseDouble(immunePopulationTextField.getText()), 
						Double.parseDouble(superSpreaderProbabilityTextField.getText()))) {
				
				MainSimulator.configureElementIdMapperStatically(); // Añadido por Nacho Palacio 2025-07-10

				// DEBUG
				// System.out.println("\n=== VERIFICANDO CONFIGURACIÓN GUARDADA ===");
    			// epidemicConfig.printCurrentConfiguration();
				
				// Build a floor panel but including the users.
//				MainSimulator.floorPanelCombined = new FloorPanelCombined(MainSimulator.DRAWING_WIDTH, MainSimulator.DRAWING_HEIGHT);				
//				MainSimulator.frmSimulator.getContentPane().add(MainSimulator.floorPanelCombined);
//				System.out.println(MainSimulator.frmSimulator.getContentPane().getComponentCount());
				if(MainSimulator.frmSimulator.getContentPane().getComponentCount() > 2) {
					i = 0;
					boolean end = false;
					while(!end && i < MainSimulator.frmSimulator.getContentPane().getComponentCount()) {
						if(MainSimulator.frmSimulator.getContentPane().getComponent(i) == MainSimulator.floorPanelCombined) {
							MainSimulator.floorPanelCombined = new FloorPanelCombined(MainSimulator.DRAWING_WIDTH, MainSimulator.DRAWING_HEIGHT);
							MainSimulator.frmSimulator.getContentPane().remove(i);
							MainSimulator.frmSimulator.getContentPane().add(MainSimulator.floorPanelCombined,i);
							end = true;
						}else {
							i++;
						}
					}
				}else {
					MainSimulator.floorPanelCombined = new FloorPanelCombined(MainSimulator.DRAWING_WIDTH, MainSimulator.DRAWING_HEIGHT);
					MainSimulator.frmSimulator.getContentPane().add(MainSimulator.floorPanelCombined);
				}
				System.out.println("load floor combined");
				// Load combined floors (4 and 5):
				MainSimulator.loadFloorCombined();
				
				// IF ideal algorithms
				if (!recommenders.checkRecommenderUsesP2P(recommendationAlgorithm)) {
					networkType = "Centralized (Centralized)";
					propagationStrategy = "";
					
				}
				System.out.println("simulation");
				// Build a simulation object:
				simulation = new Simulation(timeAvailableUser, delayObservingItem, timeForIteration, screenRefreshTime, timeForThePaths, userSpeed, kmToPixel, ttl, timeOnStairs,
						minimumTimeToUpdateRecommendation, communicationRange, maxKnowledgeBaseSize, communicationBandwidth, latencyOfTransmission, numberOfSpecialUser, numberOfNonSpecialUser,
						nonSpecialUserPaths, pathStrategy, recommendationAlgorithm, thresholdRecommendation, howMany, propagationStrategy, probabilityUserDisobedience, numberVoteReceived,
						thresholdSimilarity, networkType, timeToChangeMood, useFixedSeed, seed, true);
	
				/*
				 *  FROM PREVIOUS VERSION -> We think the message printed isn't necessary (or compulsory)
				 */
				// Calculate the simulation resolution. Check that the resolution is lower than the latency.
				//verifyResolution(timeForIteration, screenRefreshTime, latencyOfTransmission);
				System.out.println("end simulation");
				// Generate a path for each non-RS user.

				/* Añadido por Nacho Palacio 2025-04-13. */
				if (ifGenerateuserPathCheckBox.isSelected()) {
					Path strategy = null;
					// Apply the specified path strategy in the Configuration form.
					if (pathStrategy.equalsIgnoreCase("Near POI (NPOI)")) {
						strategy = new NearestPath();
					} else if (pathStrategy.equalsIgnoreCase("Exhaustive visit (ALL)")) {
						strategy = new ExhaustivePath();
					} else if (pathStrategy.equalsIgnoreCase("Completely-random (FULLY-RAND)")) {
						strategy = new RandomPath();
					}
					simulation.generate_non_special_user_path(strategy);
				}
				
				MainSimulator.floorPanelCombined.modifySize(simulation.graphSpecialUser.accessRoomFile.getMapWidth(), simulation.graphSpecialUser.accessRoomFile.getMapHeight());
				
				simulation.currentTime();
				
				MainSimulator.reloadInfoTables();
				
				if(MainSimulator.db.isConnected()) {
					DataAccessRoomFile dataAccessRoomFile = new DataAccessRoomFile(new File(Literals.ROOM_FLOOR_COMBINED));
					String mapName = dataAccessRoomFile.getName();
					ResultSet rs = MainSimulator.db.getMap(mapName);
					if(rs.next()) {							
						int mapId = rs.getInt("id");
						MainSimulator.db.saveLoadedMap(mapName, mapId);
					}
				}
				
				System.out.println("end");
				this.dispose();
			} else {
				JOptionPane.showMessageDialog(this, "You will need to fill in all the fields, except: user disobedience.", "Field validation problems", JOptionPane.ERROR_MESSAGE);
			}
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(this, "You will need to fill in all the fields, except: user disobedience. EXC", "Field validation problems", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
	 * Check that the resolution is lower than the latency.
	 */
	public void verifyResolution(double timeForIteration, double screenRefreshTime, int latencyOfTransmission) {
		// Calculate the simulation resolution:
		double simulationResolution = timeForIteration / (double) screenRefreshTime; //Cada segundo de simulaci�n, cu�nto tiempo real representa
		//System.out.println("Time for iteration: " + timeForIteration + "; Screen Refresh time: " + screenRefreshTime + "; Latency of Transmission: " + latencyOfTransmission);
		//System.out.println("Simulation resolution: " + simulationResolution);
		if (simulationResolution > latencyOfTransmission) {
			JOptionPane.showMessageDialog(this, "The resolution is greater than latency. Change the values: Real time per iteration and Iteration time/Screen refresh time.");
		} else {
			this.dispose();
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initComponents() {
		
		// recommenders/RecommenderDefinition.txt access file
		recommenders = new DataAccessRecommendersFile(new File(Literals.RECOMMENDERS_FILE));

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

		// In order to change the value in real time.
		DeferredDocumentChangedListener listenerSpecialUsers = new DeferredDocumentChangedListener(), listenerNonSpecialUsers = new DeferredDocumentChangedListener();
		
		listenerSpecialUsers.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				
				// Number of RS users - NEW VALUE PROVIDED BY THE USER
				int numberOfSpecialUser = Integer.valueOf(numberOfSpecialUsersTextField.getText()).intValue();
				// Number of non-RS users - PREVIOUS VALUE
				int numberOfNonSpecialUser = Integer.valueOf(numberOfNonSpecialUsersTextField.getText()).intValue();
				
				// If number introduced exceeds maximum
				if (numberOfSpecialUser >= Literals.TOTAL_USERS) {
					numberOfSpecialUser = Literals.TOTAL_USERS - 1;
					numberOfNonSpecialUser = 1;
					
					numberOfSpecialUsersTextField.setText(Integer.toString(numberOfSpecialUser));
					numberOfNonSpecialUsersTextField.setText(Integer.toString(numberOfNonSpecialUser));
					numberOfSpecialUsersTextField.selectAll();
				}
				// If number introduced is lower than minimum
				else if (numberOfSpecialUser <= 0) {
					numberOfSpecialUser = 1;
					numberOfNonSpecialUser = Literals.TOTAL_USERS - 1;
					
					numberOfSpecialUsersTextField.setText(Integer.toString(numberOfSpecialUser));
					numberOfNonSpecialUsersTextField.setText(Integer.toString(numberOfNonSpecialUser));
					numberOfSpecialUsersTextField.selectAll();
				}
				// If new specialusers + old non-specialusers exceeds TOTAL_USERS -> Decrease
				else if (numberOfSpecialUser + numberOfNonSpecialUser > Literals.TOTAL_USERS) {
					numberOfNonSpecialUser = Literals.TOTAL_USERS - numberOfSpecialUser;
					
					numberOfNonSpecialUsersTextField.setText(Integer.toString(numberOfNonSpecialUser));
					numberOfSpecialUsersTextField.selectAll();
				}
			}
		});
		
		listenerNonSpecialUsers.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				
				// Number of RS users - NEW VALUE PROVIDED BY THE USER
				int numberOfSpecialUser = Integer.valueOf(numberOfSpecialUsersTextField.getText()).intValue();
				// Number of non-RS users - PREVIOUS VALUE
				int numberOfNonSpecialUser = Integer.valueOf(numberOfNonSpecialUsersTextField.getText()).intValue();
				
				// If number introduced exceeds maximum
				if (numberOfNonSpecialUser >= Literals.TOTAL_USERS) {
					numberOfNonSpecialUser = Literals.TOTAL_USERS - 1;
					numberOfSpecialUser = 1;
					
					numberOfSpecialUsersTextField.setText(Integer.toString(numberOfSpecialUser));
					numberOfNonSpecialUsersTextField.setText(Integer.toString(numberOfNonSpecialUser));
					numberOfNonSpecialUsersTextField.selectAll();
				}
				// If number introduced is lower than minimum
				else if (numberOfNonSpecialUser <= 0) {
					numberOfNonSpecialUser = 1;
					numberOfSpecialUser = Literals.TOTAL_USERS - 1;
					
					numberOfSpecialUsersTextField.setText(Integer.toString(numberOfSpecialUser));
					numberOfNonSpecialUsersTextField.setText(Integer.toString(numberOfNonSpecialUser));
					numberOfNonSpecialUsersTextField.selectAll();
				}
				// If new non-specialusers + old specialusers exceeds TOTAL_USERS -> Decrease
				else if (numberOfSpecialUser + numberOfNonSpecialUser > Literals.TOTAL_USERS) {
					numberOfSpecialUser = Literals.TOTAL_USERS - numberOfSpecialUser;
					
					numberOfSpecialUsersTextField.setText(Integer.toString(numberOfSpecialUser));
					numberOfNonSpecialUsersTextField.selectAll();
				}
			}
		});
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		JPanel panel = new JPanel();
		scrollPane.setViewportView(panel);
		simulationLabel = new javax.swing.JLabel();
		simulationLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

		simulationLabel.setText("Parameters for the simulation:");
		simulationPanel = new javax.swing.JPanel();
		jLabel3 = new javax.swing.JLabel();
		jLabel3.setFont(new Font("SansSerif", Font.PLAIN, 14));
		communicationRangeTextField = new javax.swing.JTextField();
		communicationRangeTextField.setColumns(10);
		communicationRangeTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		jLabel4 = new javax.swing.JLabel();
		jLabel4.setFont(new Font("SansSerif", Font.PLAIN, 14));
		maximumKnowledgeBaseSizeTextField = new javax.swing.JTextField();
		maximumKnowledgeBaseSizeTextField.setColumns(10);
		maximumKnowledgeBaseSizeTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		jLabel5 = new javax.swing.JLabel();
		jLabel5.setFont(new Font("SansSerif", Font.PLAIN, 14));
		communicationBandwidthTextField = new javax.swing.JTextField();
		communicationBandwidthTextField.setColumns(10);
		communicationBandwidthTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		jLabel6 = new javax.swing.JLabel();
		jLabel6.setFont(new Font("SansSerif", Font.PLAIN, 14));
		latencyOfTransmissionTextField = new javax.swing.JTextField();
		latencyOfTransmissionTextField.setColumns(10);
		latencyOfTransmissionTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		JLabel9 = new javax.swing.JLabel();
		JLabel9.setFont(new Font("SansSerif", Font.PLAIN, 14));
		timeAvailableForUserTextField = new javax.swing.JTextField();
		timeAvailableForUserTextField.setColumns(10);
		timeAvailableForUserTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		jSeparator1 = new javax.swing.JSeparator();
		jLabel8 = new javax.swing.JLabel();
		jLabel8.setFont(new Font("SansSerif", Font.PLAIN, 14));
		delayObservingItemTextField = new javax.swing.JTextField();
		delayObservingItemTextField.setColumns(10);
		delayObservingItemTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		timeForIterationTextField = new javax.swing.JTextField();
		timeForIterationTextField.setColumns(10);
		timeForIterationTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		jLabel9 = new javax.swing.JLabel();
		jLabel9.setFont(new Font("SansSerif", Font.PLAIN, 14));
		jLabel10 = new javax.swing.JLabel();
		jLabel10.setFont(new Font("SansSerif", Font.PLAIN, 14));
		screenRefreshTimeTextField = new javax.swing.JTextField();
		screenRefreshTimeTextField.setColumns(10);
		screenRefreshTimeTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		jLabel11 = new javax.swing.JLabel();
		jLabel11.setFont(new Font("SansSerif", Font.PLAIN, 14));
		timeForThePathsTextField = new javax.swing.JTextField();
		timeForThePathsTextField.setColumns(10);
		timeForThePathsTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		jLabel15 = new javax.swing.JLabel();
		jLabel15.setFont(new Font("SansSerif", Font.PLAIN, 14));
		userSpeedTextField = new javax.swing.JTextField();
		userSpeedTextField.setColumns(10);
		userSpeedTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		jLabel16 = new javax.swing.JLabel();
		jLabel16.setFont(new Font("SansSerif", Font.PLAIN, 14));
		kmToPixelTextField = new javax.swing.JTextField();
		kmToPixelTextField.setColumns(10);
		kmToPixelTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		jLabel17 = new javax.swing.JLabel();
		jLabel17.setFont(new Font("SansSerif", Font.PLAIN, 14));
		ttlTextField = new javax.swing.JTextField();
		ttlTextField.setColumns(10);
		ttlTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		simulationPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
				
		jLabel3.setText("Communication range [m]");
						
		communicationRangeTextField.setText("250");
								
		jLabel4.setText("Maximum knowledge base size [Mb]");
										
		maximumKnowledgeBaseSizeTextField.setText("1");
												
		jLabel5.setText("Communication bandwidth [Mbps]");
														
		communicationBandwidthTextField.setText("54");
																
		jLabel6.setText("Latency of transmission [s]");
																		
		latencyOfTransmissionTextField.setText("1");
																				
		JLabel9.setText("Time available for the user [hour]");
																						
		timeAvailableForUserTextField.setText("1");
																								
		jLabel8.setText("Delay observing item [seconds]");
																										
		delayObservingItemTextField.setText("30");
																												
		timeForIterationTextField.setText("1");
																														
		jLabel9.setText("Real time per iteration [seconds]");
																																
		jLabel10.setText("Iteration time/Screen refresh time [seconds]");
																																		
		screenRefreshTimeTextField.setText("1");
																																				
		jLabel11.setText("Time for the paths [hour]");
																																						
		timeForThePathsTextField.setText("1");
																																								
		jLabel15.setText("User speed [km/h]");
																																										
		userSpeedTextField.setText("3");
																																												
		jLabel16.setText("1 km represents in pixels [pixel]");
																																														
		kmToPixelTextField.setText("6597");
																																																
		jLabel17.setText("TTL of the items to propagate [seconds]");

		ttlTextField.setText("180");
		
		timeOnStairsTextField = new JTextField();
		timeOnStairsTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		timeOnStairsTextField.setText("60");
		timeOnStairsTextField.setColumns(10);
				
		JLabel lblTimeOnThe = new JLabel("Time on the stairs [seconds]");
		lblTimeOnThe.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		JLabel lblMinimumTimeBetween = new JLabel("Minimum time to update recommendation [seconds]");
		lblMinimumTimeBetween.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		minimumTimeToUpdateRecommendationTextField = new JTextField();
		minimumTimeToUpdateRecommendationTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		minimumTimeToUpdateRecommendationTextField.setText("30");
		minimumTimeToUpdateRecommendationTextField.setColumns(10);
		
		JLabel lblNewLabel_7 = new JLabel("Time to change the mood [seconds]");
		lblNewLabel_7.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		timeChangeMoodTextField = new JTextField();
		timeChangeMoodTextField.setText("1800");
		timeChangeMoodTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		timeChangeMoodTextField.setColumns(10);
		
		jLabelSeed = new JLabel("Seed number");
		jLabelSeed.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		seedNumber = new JTextField();
		seedNumber.setEnabled(false);
		seedNumber.setFont(new Font("SansSerif", Font.PLAIN, 14));
		seedNumber.setText("0");
		seedNumber.setEditable(false);
		seedNumber.setColumns(10);
		
		chckbxUseFixedSeed = new JCheckBox("Use fixed seed for randomness");
		chckbxUseFixedSeed.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		chckbxUseFixedSeed.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (chckbxUseFixedSeed.isSelected()) {
					// Seed fields enabled and visible
					seedNumber.setEnabled(true);
					seedNumber.setEditable(true);
				} else {
					// Seed fields not enabled nor visible
					seedNumber.setEnabled(false);
					seedNumber.setEditable(true);
				}
			}
		});

		chooseMap = new JButton("Choose map");
		JDialog conf = this;
		chooseMap.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("*.svg", "svg"));
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.setCurrentDirectory(new File(Literals.PATH_MAPS));
				fileChooser.setDialogTitle("Open map (select SVG file or directory with text files)");
				
				switch(fileChooser.showOpenDialog(conf)) {
				
					case JFileChooser.APPROVE_OPTION:
						
						File f = fileChooser.getSelectedFile();
						//model.setFiles(f.getAbsolutePath());
						//System.out.println("1: "+Literals.GRAPH_FLOOR_COMBINED);
						if(f.isDirectory()) {
							Literals.GRAPH_FLOOR_COMBINED = f.getAbsolutePath() + File.separator + Literals.GRAPH_FILE_NAME;
							Literals.ROOM_FLOOR_COMBINED = f.getAbsolutePath() + File.separator + Literals.ROOM_FILE_NAME;
							Literals.ITEM_FLOOR_COMBINED = f.getAbsolutePath() + File.separator + Literals.ITEM_FILE_NAME;
						}else if(f.isFile() && f.getName().endsWith(".svg")){
							Literals.GRAPH_FLOOR_COMBINED = f.getAbsolutePath();
							Literals.ROOM_FLOOR_COMBINED = f.getAbsolutePath();
							Literals.ITEM_FLOOR_COMBINED = f.getAbsolutePath();
						}
						
						MainSimulator.configureElementIdMapperStatically(); // Añadido por Nacho Palacio 2025-07-10


						//System.out.println("2: "+Literals.GRAPH_FLOOR_COMBINED);
						
					default:
						break;
						
				}
			}
		});

		// Añadido por Nacho Palacio 2025-07-16
		JLabel epidemicLabel = new JLabel("Epidemic simulation settings:");
		epidemicLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

		JPanel epidemicPanel = new JPanel();
		epidemicPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

		JLabel lblEpidemicModel = new JLabel("Epidemic model");
		lblEpidemicModel.setFont(new Font("SansSerif", Font.PLAIN, 14));

		epidemicModelComboBox = new JComboBox();
		epidemicModelComboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
		epidemicModelComboBox.setModel(new DefaultComboBoxModel(new String[] { 
			"SIMPLE_PROXIMITY", 
			"AEROSOL_PENG",
			"AEROSOL_LELIEVELD"
		}));

		JLabel lblInitialInfected = new JLabel("Initial infected users");
		lblInitialInfected.setFont(new Font("SansSerif", Font.PLAIN, 14));

		initialInfectedTextField = new JTextField();
		initialInfectedTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		initialInfectedTextField.setText("2");
		initialInfectedTextField.setColumns(10);

		JLabel lblMaskCompliance = new JLabel("Mask compliance rate [0-1]");
		lblMaskCompliance.setFont(new Font("SansSerif", Font.PLAIN, 14));

		maskComplianceTextField = new JTextField();
		maskComplianceTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		maskComplianceTextField.setText("0.3");
		maskComplianceTextField.setColumns(10);

		// Layout del panel epidemiológico
		GroupLayout gl_epidemicPanel = new GroupLayout(epidemicPanel);

		JLabel lblDefaultVentRate = new JLabel("Default ventilation rate [h⁻¹]");
		lblDefaultVentRate.setFont(new Font("SansSerif", Font.PLAIN, 14));

		defaultVentilationRateTextField = new JTextField();
		defaultVentilationRateTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		defaultVentilationRateTextField.setText("3.0");
		defaultVentilationRateTextField.setColumns(10);

		JLabel lblVirusDecayRate = new JLabel("Virus decay rate [h⁻¹]");
		lblVirusDecayRate.setFont(new Font("SansSerif", Font.PLAIN, 14));

		virusDecayRateTextField = new JTextField();
		virusDecayRateTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		virusDecayRateTextField.setText("0.62");
		virusDecayRateTextField.setColumns(10);

		JLabel lblMaskExhEff = new JLabel("Mask exhalation efficiency [0-1]");
		lblMaskExhEff.setFont(new Font("SansSerif", Font.PLAIN, 14));

		maskExhalationEffTextField = new JTextField();
		maskExhalationEffTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		maskExhalationEffTextField.setText("0.5");
		maskExhalationEffTextField.setColumns(10);

		JLabel lblMaskInhEff = new JLabel("Mask inhalation efficiency [0-1]");
		lblMaskInhEff.setFont(new Font("SansSerif", Font.PLAIN, 14));

		maskInhalationEffTextField = new JTextField();
		maskInhalationEffTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		maskInhalationEffTextField.setText("0.3");
		maskInhalationEffTextField.setColumns(10);

		JLabel lblSimulationDuration = new JLabel("Simulation duration [minutes]");
		lblSimulationDuration.setFont(new Font("SansSerif", Font.PLAIN, 14));

		simulationDurationTextField = new JTextField();
		simulationDurationTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		simulationDurationTextField.setText("7"); // 7 minutos por defecto
		simulationDurationTextField.setColumns(10);

		JLabel lblImmunePopulation = new JLabel("Immune population fraction [0-1]");
		lblImmunePopulation.setFont(new Font("SansSerif", Font.PLAIN, 14));

		immunePopulationTextField = new JTextField();
		immunePopulationTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		immunePopulationTextField.setText("0.0");
		immunePopulationTextField.setColumns(10);

		JLabel lblSuperSpreaderProb = new JLabel("Super-spreader probability [0-1]");
		lblSuperSpreaderProb.setFont(new Font("SansSerif", Font.PLAIN, 14));

		superSpreaderProbabilityTextField = new JTextField();
		superSpreaderProbabilityTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		superSpreaderProbabilityTextField.setText("0.05");
		superSpreaderProbabilityTextField.setColumns(10);

		// Panel para parámetros específicos del modelo
		modelSpecificPanel = new JPanel();
		modelSpecificPanel.setBorder(BorderFactory.createTitledBorder("Model-specific parameters"));

		gl_epidemicPanel.setHorizontalGroup(
			gl_epidemicPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_epidemicPanel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_epidemicPanel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_epidemicPanel.createSequentialGroup()
							.addComponent(lblEpidemicModel)
							.addGap(18)
							.addComponent(epidemicModelComboBox, 0, 200, Short.MAX_VALUE))
						.addGroup(gl_epidemicPanel.createSequentialGroup()
							.addComponent(lblInitialInfected)
							.addGap(18)
							.addComponent(initialInfectedTextField, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_epidemicPanel.createSequentialGroup()
							.addComponent(lblMaskCompliance)
							.addGap(18)
							.addComponent(maskComplianceTextField, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_epidemicPanel.createSequentialGroup()
							.addComponent(lblDefaultVentRate)
							.addGap(18)
							.addComponent(defaultVentilationRateTextField, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_epidemicPanel.createSequentialGroup()
							.addComponent(lblVirusDecayRate)
							.addGap(18)
							.addComponent(virusDecayRateTextField, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_epidemicPanel.createSequentialGroup()
							.addComponent(lblMaskExhEff)
							.addGap(18)
							.addComponent(maskExhalationEffTextField, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_epidemicPanel.createSequentialGroup()
							.addComponent(lblMaskInhEff)
							.addGap(18)
							.addComponent(maskInhalationEffTextField, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_epidemicPanel.createSequentialGroup()
							.addComponent(lblSimulationDuration)
							.addGap(18)
							.addComponent(simulationDurationTextField, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_epidemicPanel.createSequentialGroup()
							.addComponent(lblImmunePopulation)
							.addGap(18)
							.addComponent(immunePopulationTextField, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_epidemicPanel.createSequentialGroup()
							.addComponent(lblSuperSpreaderProb)
							.addGap(18)
							.addComponent(superSpreaderProbabilityTextField, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE))
						.addComponent(modelSpecificPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					.addContainerGap())
		);

		gl_epidemicPanel.setVerticalGroup(
			gl_epidemicPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_epidemicPanel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_epidemicPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblEpidemicModel)
						.addComponent(epidemicModelComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(10)
					.addGroup(gl_epidemicPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblInitialInfected)
						.addComponent(initialInfectedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(10)
					.addGroup(gl_epidemicPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblMaskCompliance)
						.addComponent(maskComplianceTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(10)
					.addGroup(gl_epidemicPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblDefaultVentRate)
						.addComponent(defaultVentilationRateTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(10)
					.addGroup(gl_epidemicPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblVirusDecayRate)
						.addComponent(virusDecayRateTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(10)
					.addGroup(gl_epidemicPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblMaskExhEff)
						.addComponent(maskExhalationEffTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(10)
					.addGroup(gl_epidemicPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblMaskInhEff)
						.addComponent(maskInhalationEffTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(10)
					.addGroup(gl_epidemicPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblSimulationDuration)
						.addComponent(simulationDurationTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(10)
					.addGroup(gl_epidemicPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblImmunePopulation)
						.addComponent(immunePopulationTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(10)
					.addGroup(gl_epidemicPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblSuperSpreaderProb)
						.addComponent(superSpreaderProbabilityTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(10)
					.addComponent(modelSpecificPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
		);
		epidemicPanel.setLayout(gl_epidemicPanel);

		// Añadido por Nacho Palacio 2025-09-14
		epidemicModelComboBox.addActionListener(e -> updateModelSpecificParameters());
		
		javax.swing.GroupLayout gl_simulationPanel = new javax.swing.GroupLayout(simulationPanel);
		gl_simulationPanel.setHorizontalGroup(
			gl_simulationPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_simulationPanel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.LEADING)
						.addComponent(chooseMap)
						.addComponent(lblMinimumTimeBetween)
						.addComponent(jLabel3)
						.addComponent(jLabel4)
						.addComponent(jLabel5)
						.addComponent(jLabel6)
						.addComponent(lblNewLabel_7)
						.addComponent(jLabel8)
						.addComponent(jLabel9)
						.addComponent(lblTimeOnThe)
						.addComponent(JLabel9)
						.addComponent(jLabel10)
						.addComponent(jLabel11)
						.addComponent(jLabel15)
						.addComponent(jLabel16)
						.addComponent(jLabel17)
						.addComponent(chckbxUseFixedSeed)
						.addGroup(gl_simulationPanel.createSequentialGroup()
							.addGap(21)
							.addComponent(jLabelSeed)
							.addGap(18)
							.addComponent(seedNumber, GroupLayout.DEFAULT_SIZE, 116, Short.MAX_VALUE)))
					.addGap(7)
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_simulationPanel.createSequentialGroup()
							.addGroup(gl_simulationPanel.createParallelGroup(Alignment.LEADING)
								.addComponent(jSeparator1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addGroup(gl_simulationPanel.createSequentialGroup()
									.addPreferredGap(ComponentPlacement.RELATED)
									.addGroup(gl_simulationPanel.createParallelGroup(Alignment.LEADING, false)
										.addComponent(timeForIterationTextField, Alignment.TRAILING)
										.addComponent(delayObservingItemTextField, Alignment.TRAILING)
										.addComponent(timeAvailableForUserTextField)
										.addComponent(screenRefreshTimeTextField)
										.addComponent(timeForThePathsTextField)
										.addComponent(userSpeedTextField)
										.addComponent(kmToPixelTextField)
										.addComponent(ttlTextField))))
							.addContainerGap())
						.addGroup(Alignment.TRAILING, gl_simulationPanel.createSequentialGroup()
							.addGroup(gl_simulationPanel.createParallelGroup(Alignment.TRAILING)
								.addComponent(communicationRangeTextField, Alignment.LEADING, 116, 116, Short.MAX_VALUE)
								.addComponent(maximumKnowledgeBaseSizeTextField, Alignment.LEADING, 116, 116, Short.MAX_VALUE)
								.addComponent(communicationBandwidthTextField, Alignment.LEADING, 116, 116, Short.MAX_VALUE)
								.addComponent(latencyOfTransmissionTextField)
								.addComponent(timeChangeMoodTextField)
								.addComponent(minimumTimeToUpdateRecommendationTextField)
								.addComponent(timeOnStairsTextField, Alignment.LEADING))
							.addGap(307))))
		);
		gl_simulationPanel.setVerticalGroup(
			gl_simulationPanel.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_simulationPanel.createSequentialGroup()
					.addComponent(chooseMap)
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(JLabel9)
						.addComponent(timeAvailableForUserTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(delayObservingItemTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(jLabel8))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(timeForIterationTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(jLabel9))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(screenRefreshTimeTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(jLabel10))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(timeForThePathsTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(jLabel11))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(userSpeedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(jLabel15))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(kmToPixelTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(jLabel16))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(ttlTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(jLabel17))
					.addGap(21)
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblTimeOnThe)
						.addComponent(timeOnStairsTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(18)
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblMinimumTimeBetween)
						.addComponent(minimumTimeToUpdateRecommendationTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(18)
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(jLabel3)
						.addComponent(communicationRangeTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(18)
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(jLabel4)
						.addComponent(maximumKnowledgeBaseSizeTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(18)
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(jLabel5)
						.addComponent(communicationBandwidthTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(18)
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(jLabel6)
						.addComponent(latencyOfTransmissionTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGroup(gl_simulationPanel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_simulationPanel.createSequentialGroup()
							.addPreferredGap(ComponentPlacement.RELATED, 60, Short.MAX_VALUE)
							.addComponent(jSeparator1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addGap(42))
						.addGroup(gl_simulationPanel.createSequentialGroup()
							.addGap(18)
							.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
								.addComponent(timeChangeMoodTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblNewLabel_7))
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(chckbxUseFixedSeed)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addGroup(gl_simulationPanel.createParallelGroup(Alignment.BASELINE)
								.addComponent(jLabelSeed)
								.addComponent(seedNumber, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))))
					.addGap(69))
		);
		simulationPanel.setLayout(gl_simulationPanel);
		
		experimentLabel = new javax.swing.JLabel();
		experimentLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
		experimentLabel.setText("Parameters for the experimentation:");
		
		experimentPanel = new javax.swing.JPanel();
		
		jLabel14 = new javax.swing.JLabel();
		jLabel14.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		howManyTextField = new javax.swing.JTextField();
		howManyTextField.setEnabled(false);
		howManyTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		jLabel18 = new javax.swing.JLabel();
		jLabel18.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		probabilityUserDisobedienceTextField = new javax.swing.JTextField();
		probabilityUserDisobedienceTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		jLabel19 = new javax.swing.JLabel();
		jLabel19.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		numberVoteReceivedTextField = new javax.swing.JTextField();
		numberVoteReceivedTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));

		experimentPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

		jLabel14.setText("How many items to recommend");

		howManyTextField.setText("10");
	
		jLabel18.setText("Probability of user disobedience");
		
		probabilityUserDisobedienceTextField.setText("0.4");
		
		jLabel19.setText("Number of votes received");
		
		numberVoteReceivedTextField.setText("40");
	
		JLabel lblNewLabel = new JLabel("Threshold of recommendation [1-5]");
		lblNewLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

		thresholdRecommendationTextField = new JTextField();
		thresholdRecommendationTextField.setEnabled(false);
		thresholdRecommendationTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		thresholdRecommendationTextField.setText("2.5");
		thresholdRecommendationTextField.setColumns(10);
		
		jLabel7 = new javax.swing.JLabel();
		jLabel7.setFont(new Font("SansSerif", Font.PLAIN, 14));

		jLabel7.setText("Number of RS users [1-" + Literals.TOTAL_USERS + "]");
		numberOfSpecialUsersTextField = new javax.swing.JTextField();
		numberOfSpecialUsersTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		numberOfSpecialUsersTextField.setText("1");
	
		JLabel lblExperimentType = new JLabel("Recommender"); /* silarri, 2022-07-13. */
		//JLabel lblExperimentType = new JLabel("Recommendation algorithm");
		lblExperimentType.setFont(new Font("SansSerif", Font.PLAIN, 14));

		recommendationAlgorithmComboBox = new JComboBox();
		recommendationAlgorithmComboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
		recommendationAlgorithmComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String selectedStrategy = (String) recommendationAlgorithmComboBox.getSelectedItem();
				String algorithmDescription = null;
				
				boolean enableNetwork = recommenders.checkRecommenderUsesP2P(selectedStrategy);
				
				switch (selectedStrategy) {
					
					case "User-Based Collaborative Filtering (UBCF)":
						thresholdRecommendationTextField.setEnabled(true);
						thresholdSimilarityTextField.setEnabled(true);
						howManyTextField.setEnabled(true);
						typeNetworkComboBox.setEnabled(enableNetwork);
						propagationStrategyComboBox.setEnabled(enableNetwork);
						algorithmDescription = "UBCF: the RS user is recommended works according to their friends and tastes in the past. It applies the K-Nearst Neighbors (K-NN) algorithm.";
						break;
						
					case "Completely-random (FULLY-RAND)":
						thresholdRecommendationTextField.setEnabled(false);
						thresholdSimilarityTextField.setEnabled(false);
						howManyTextField.setEnabled(true);
						typeNetworkComboBox.setEnabled(enableNetwork);
						propagationStrategyComboBox.setEnabled(enableNetwork);
						algorithmDescription = "FULLY-RAND: the RS user visits works of art recommended in a completely random manner, even if this means changing from one room to another that may be located far away. This is expected to be the worst approach possible, as the user could potentially have to traverse very large distances between observations.";
						break;
						
					case "Exhaustive visit (ALL)":
						thresholdRecommendationTextField.setEnabled(false);
						thresholdSimilarityTextField.setEnabled(false);
						howManyTextField.setEnabled(true);
						typeNetworkComboBox.setEnabled(enableNetwork);
						propagationStrategyComboBox.setEnabled(enableNetwork);
						algorithmDescription = "ALL: the RS user is recommended to visit all the works of art in his/her current room, then the user is suggested to move to a different connected room (in the same or in a different floor, if there are stairs nearby), and so on.";
						break;
						
					case "Near POI (NPOI)":
						thresholdRecommendationTextField.setEnabled(false);
						thresholdSimilarityTextField.setEnabled(false);
						howManyTextField.setEnabled(true);
						typeNetworkComboBox.setEnabled(enableNetwork);
						propagationStrategyComboBox.setEnabled(enableNetwork);
						algorithmDescription = "NPOI: the RS user is recommended to go to the nearest point of interest, which may be a work of art or an exit (a door connecting to another room, or the stairs leading to a different floor).";
						break;
	
					case "Know-It-All (Know-It-All)":
						thresholdRecommendationTextField.setEnabled(true);
						thresholdSimilarityTextField.setEnabled(true);
						howManyTextField.setEnabled(true);
						typeNetworkComboBox.setEnabled(enableNetwork);
						propagationStrategyComboBox.setEnabled(enableNetwork);
						algorithmDescription = "Know-It-All: it is like the centralized approach but assuming that the centralized service stores all the rating information that the other visitors could eventually provide for each work of art in the museum (even if they actually never released that information or observed those items). This is obviously an unrealistic assumption, and therefore this approach is a baseline operating under ideal conditions.";
						break;
						
					case "K-Ideal (K-Ideal)":
						thresholdRecommendationTextField.setEnabled(true);
						thresholdSimilarityTextField.setEnabled(true);
						howManyTextField.setEnabled(true);
						typeNetworkComboBox.setEnabled(enableNetwork);
						propagationStrategyComboBox.setEnabled(enableNetwork);
						algorithmDescription = "K-Ideal: it is similar to Know-It-All but, rather than applying a user-based collaborative filtering, all the ratings about items unseen by the user are estimated and the k items with the best predicted ratings are recommended, independently of whether they exceed or not the recommendation threshold. The k-items are re-ordered, if needed, to minimize the total distance traversed by the user.";
						break;
						
					case "Select an algorithm":
						thresholdRecommendationTextField.setEnabled(false);
						thresholdSimilarityTextField.setEnabled(false);
						howManyTextField.setEnabled(false);
						typeNetworkComboBox.setEnabled(enableNetwork);
						propagationStrategyComboBox.setEnabled(enableNetwork);
						break;
						
					default:
						break;
				}
				MainSimulator.printConsole("Recommendation algorithm chosen for RS users:", Level.WARNING);
				MainSimulator.printConsole(algorithmDescription, Level.WARNING);
			}
		});
		recommendationAlgorithmComboBox.setModel(new DefaultComboBoxModel(new String[] { "Select an algorithm", "User-Based Collaborative Filtering (UBCF)", "Completely-random (FULLY-RAND)",
				"Exhaustive visit (ALL)", "Near POI (NPOI)", "Know-It-All (Know-It-All)", "K-Ideal (K-Ideal)" }));

		nonSpecialUserPathsJTextField = new JTextField();
		nonSpecialUserPathsJTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		nonSpecialUserPathsJTextField.setColumns(100);

		JSeparator separator = new JSeparator();
		separator_1 = new JSeparator();
		
		/*
		entranceDoorComboBox = new JComboBox();
		entranceDoorComboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
		entranceDoorComboBox.setEnabled(false);
		*/

		JSeparator separator_2 = new JSeparator();

		JLabel lblNewLabel_1 = new JLabel("Number of non-RS users [1-" + Literals.TOTAL_USERS + "]");
		lblNewLabel_1.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
		numberOfNonSpecialUsersTextField = new JTextField();
		//numberOfNonSpecialUsersTextField.setEditable(false);
		numberOfNonSpecialUsersTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		numberOfNonSpecialUsersTextField.setText(String.valueOf(Literals.TOTAL_USERS - 1));
		numberOfNonSpecialUsersTextField.setColumns(100);
		
		numberOfSpecialUsersTextField.getDocument().addDocumentListener(listenerSpecialUsers);
		numberOfNonSpecialUsersTextField.getDocument().addDocumentListener(listenerNonSpecialUsers);

		lblNewLabel_2 = new JLabel("File name");
		lblNewLabel_2.setFont(new Font("SansSerif", Font.PLAIN, 14));

		JLabel lblNewLabel_3 = new JLabel("Path strategy");
		lblNewLabel_3.setFont(new Font("SansSerif", Font.PLAIN, 14));

		pathStrategyComboBox = new JComboBox();
		pathStrategyComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				/* Añadido por Nacho Palacio 2025-04-12. */
				String selectedStrategy = (String) pathStrategyComboBox.getSelectedItem();
				String fileName = "";
				int numberOfNonSpecialUsers = Integer.parseInt(numberOfNonSpecialUsersTextField.getText());
				// Final añadido

				if (pathStrategyComboBox.getSelectedItem().equals("Completely-random (FULLY-RAND)")) {
					fileName = "rand_non_special_user_paths_" + numberOfNonSpecialUsers + ".txt";
					nonSpecialUserPathsJTextField.setText("rand_non_special_user_paths_" + numberOfNonSpecialUsersTextField.getText() + ".txt");
				} else if (pathStrategyComboBox.getSelectedItem().equals("Exhaustive visit (ALL)")) {
					fileName = "all_non_special_user_paths_" + numberOfNonSpecialUsers + ".txt";
					nonSpecialUserPathsJTextField.setText("all_non_special_user_paths_" + numberOfNonSpecialUsersTextField.getText() + ".txt");
				} else if (pathStrategyComboBox.getSelectedItem().equals("Near POI (NPOI)")) {
					fileName = "npoi_non_special_user_paths_" + numberOfNonSpecialUsers + ".txt";
					nonSpecialUserPathsJTextField.setText("npoi_non_special_user_paths_" + numberOfNonSpecialUsersTextField.getText() + ".txt");
				} else if (pathStrategyComboBox.getSelectedItem().equals("Select a strategy")) {
					fileName = "";
					nonSpecialUserPathsJTextField.setText("");
				}
			}
		});
		pathStrategyComboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
		pathStrategyComboBox.setEnabled(true);
		pathStrategyComboBox.setModel(new DefaultComboBoxModel(new String[] { "Select a strategy", "Completely-random (FULLY-RAND)", "Exhaustive visit (ALL)", "Near POI (NPOI)" }));

		ifGenerateuserPathCheckBox = new JCheckBox("Generation of dynamic non-RS user paths");
		ifGenerateuserPathCheckBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
		/*ifGenerateuserPathCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (ifGenerateuserPathCheckBox.isSelected()) {
					pathStrategyComboBox.setEnabled(true);
				} else {
					pathStrategyComboBox.setEnabled(false);
				}
			}
		});*/
	
		JLabel lblNewLabel_4 = new JLabel("Propagation Strategy");
		lblNewLabel_4.setFont(new Font("SansSerif", Font.PLAIN, 14));

		propagationStrategyComboBox = new JComboBox();
		propagationStrategyComboBox.setEnabled(false);
		propagationStrategyComboBox.setModel(new DefaultComboBoxModel(new String[] { "Select a strategy", "Opportunistic", "Flooding" }));
		propagationStrategyComboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));

		JLabel lblNewLabel_5 = new JLabel("Type of network");
		lblNewLabel_5.setFont(new Font("SansSerif", Font.PLAIN, 14));

		typeNetworkComboBox = new JComboBox();
		typeNetworkComboBox.setEnabled(false);
		typeNetworkComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String selectedNetwork = (String) typeNetworkComboBox.getSelectedItem();
				switch (selectedNetwork) {
					case "Peer To Peer (P2P)":
						propagationStrategyComboBox.setEnabled(true);
						break;
						
					case "Centralized (Centralized)":
						propagationStrategyComboBox.setEnabled(false);
						break;
						
					default:
						break;
				}
			}
		});
		typeNetworkComboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
		typeNetworkComboBox.setModel(new DefaultComboBoxModel(new String[] { "Select a type of network", "Peer To Peer (P2P)", "Centralized (Centralized)" }));

		JLabel lblNewLabel_6 = new JLabel("Threshold of similarity [0-1]");
		lblNewLabel_6.setFont(new Font("SansSerif", Font.PLAIN, 14));

		thresholdSimilarityTextField = new JTextField();
		thresholdSimilarityTextField.setFont(new Font("SansSerif", Font.PLAIN, 14));
		thresholdSimilarityTextField.setEnabled(false);
		thresholdSimilarityTextField.setText("0.5");
		thresholdSimilarityTextField.setColumns(10);
		
		JSeparator separator_4 = new JSeparator();
		
		JSeparator separator_3 = new JSeparator();

		javax.swing.GroupLayout gl_experimentPanel = new javax.swing.GroupLayout(experimentPanel);
		gl_experimentPanel.setHorizontalGroup(
			gl_experimentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_experimentPanel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_experimentPanel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_experimentPanel.createSequentialGroup()
							.addGroup(gl_experimentPanel.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_experimentPanel.createSequentialGroup()
									.addComponent(lblExperimentType)
									.addPreferredGap(ComponentPlacement.UNRELATED)
									.addComponent(recommendationAlgorithmComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
								.addGroup(gl_experimentPanel.createSequentialGroup()
									.addGroup(gl_experimentPanel.createParallelGroup(Alignment.LEADING)
										.addComponent(lblNewLabel)
										.addComponent(lblNewLabel_6)
										.addComponent(jLabel14))
									.addPreferredGap(ComponentPlacement.UNRELATED)
									.addGroup(gl_experimentPanel.createParallelGroup(Alignment.LEADING, false)
										.addComponent(howManyTextField)
										.addComponent(thresholdSimilarityTextField)
										.addComponent(thresholdRecommendationTextField))))
							.addContainerGap(20, Short.MAX_VALUE))
						.addGroup(gl_experimentPanel.createSequentialGroup()
							.addComponent(jLabel7)
							.addGap(48)
							.addComponent(numberOfSpecialUsersTextField, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE)
							.addContainerGap())
						.addGroup(gl_experimentPanel.createSequentialGroup()
							.addGroup(gl_experimentPanel.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_experimentPanel.createSequentialGroup()
									.addComponent(lblNewLabel_2)
									.addGap(18)
									.addComponent(nonSpecialUserPathsJTextField, 380, 380, 380))
								.addComponent(separator_3, GroupLayout.PREFERRED_SIZE, 489, GroupLayout.PREFERRED_SIZE)
								.addGroup(gl_experimentPanel.createSequentialGroup()
									.addGap(24)
									.addComponent(lblNewLabel_3)
									.addGap(18)
									.addComponent(pathStrategyComboBox, GroupLayout.PREFERRED_SIZE, 333, GroupLayout.PREFERRED_SIZE)))
							.addGap(469))
						.addGroup(gl_experimentPanel.createSequentialGroup()
							.addGroup(gl_experimentPanel.createParallelGroup(Alignment.LEADING)
								.addComponent(lblNewLabel_5)
								.addComponent(lblNewLabel_4))
							.addGap(53)
							.addGroup(gl_experimentPanel.createParallelGroup(Alignment.LEADING, false)
								.addComponent(propagationStrategyComboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(typeNetworkComboBox, 0, 211, Short.MAX_VALUE))
								//.addComponent(entranceDoorComboBox, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 65, GroupLayout.PREFERRED_SIZE))
							.addContainerGap(101, Short.MAX_VALUE))
						.addGroup(gl_experimentPanel.createSequentialGroup()
							.addGroup(gl_experimentPanel.createParallelGroup(Alignment.TRAILING, false)
								.addComponent(separator_4, Alignment.LEADING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(separator_1, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 489, Short.MAX_VALUE))
							.addContainerGap())
						.addGroup(gl_experimentPanel.createSequentialGroup()
							.addComponent(separator, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addGap(117))
						.addGroup(gl_experimentPanel.createSequentialGroup()
							.addGroup(gl_experimentPanel.createParallelGroup(Alignment.LEADING)
								.addComponent(jLabel18)
								.addComponent(jLabel19))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_experimentPanel.createParallelGroup(Alignment.LEADING)
								.addComponent(numberVoteReceivedTextField, 80, 80, 80)
								.addComponent(probabilityUserDisobedienceTextField, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE))
							.addContainerGap(213, Short.MAX_VALUE))
						.addGroup(gl_experimentPanel.createSequentialGroup()
							.addComponent(separator_2, GroupLayout.PREFERRED_SIZE, 496, GroupLayout.PREFERRED_SIZE)
							.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addGroup(gl_experimentPanel.createSequentialGroup()
							.addComponent(ifGenerateuserPathCheckBox)
							.addContainerGap(182, Short.MAX_VALUE))
						.addGroup(gl_experimentPanel.createSequentialGroup()
							.addComponent(lblNewLabel_1)
							.addGap(18)
							.addComponent(numberOfNonSpecialUsersTextField, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE)
							.addContainerGap(617, Short.MAX_VALUE))))
		);
		gl_experimentPanel.setVerticalGroup(
			gl_experimentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_experimentPanel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_experimentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(jLabel7)
						.addComponent(numberOfSpecialUsersTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_experimentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblNewLabel_1)
						.addComponent(numberOfNonSpecialUsersTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(17)
					.addComponent(separator_3, GroupLayout.PREFERRED_SIZE, 11, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_experimentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblNewLabel_2)
						.addComponent(nonSpecialUserPathsJTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(18)
					.addComponent(ifGenerateuserPathCheckBox)
					.addGap(15)
					.addGroup(gl_experimentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblNewLabel_3)
						.addComponent(pathStrategyComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(18)
					.addComponent(separator_1, GroupLayout.PREFERRED_SIZE, 11, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_experimentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblExperimentType)
						.addComponent(recommendationAlgorithmComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(18)
					.addGroup(gl_experimentPanel.createParallelGroup(Alignment.LEADING)
						.addComponent(lblNewLabel)
						.addComponent(thresholdRecommendationTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGroup(gl_experimentPanel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_experimentPanel.createSequentialGroup()
							.addGap(18)
							.addComponent(lblNewLabel_6))
						.addGroup(gl_experimentPanel.createSequentialGroup()
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(thresholdSimilarityTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
					.addGap(18)
					.addGroup(gl_experimentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(howManyTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(jLabel14))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(separator_4, GroupLayout.PREFERRED_SIZE, 7, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_experimentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblNewLabel_5)
						.addComponent(typeNetworkComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(18)
					.addGroup(gl_experimentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(propagationStrategyComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblNewLabel_4))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(separator, GroupLayout.PREFERRED_SIZE, 8, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					//.addComponent(entranceDoorComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(18)
					.addComponent(separator_2, GroupLayout.PREFERRED_SIZE, 2, GroupLayout.PREFERRED_SIZE)
					.addGap(15)
					.addGroup(gl_experimentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(jLabel18)
						.addComponent(probabilityUserDisobedienceTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(18)
					.addGroup(gl_experimentPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(jLabel19)
						.addComponent(numberVoteReceivedTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(162))
		);
		experimentPanel.setLayout(gl_experimentPanel);
		saveButton = new javax.swing.JButton();
		saveButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
		
				saveButton.setText("Save and load combined floors");
				saveButton.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						try {
							saveActionPerformed(evt);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
		
		JButton btnFasttestusersrandcent = new JButton("FT,RAND,C");
		btnFasttestusersrandcent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					
					timeForIterationTextField.setText("60"); // Fast test -> Simulate 1 hour in 1 minute
					
					numberOfNonSpecialUsersTextField.setText("175");
					
					ifGenerateuserPathCheckBox.setSelected(true);
					pathStrategyComboBox.setSelectedItem("Completely-random (FULLY-RAND)");
					
					recommendationAlgorithmComboBox.setSelectedItem("Completely-random (FULLY-RAND)");
					
					typeNetworkComboBox.setSelectedItem("Centralized (Centralized)");
					
					saveActionPerformed(e);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		});
		
		JButton btnFasttestusersrandP2P = new JButton("FT,NPOI,P2P");
		btnFasttestusersrandP2P.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					
					timeForIterationTextField.setText("60"); // Fast test -> Simulate 1 hour in 1 minute
					
					numberOfNonSpecialUsersTextField.setText("175");
					
					ifGenerateuserPathCheckBox.setSelected(true);
					pathStrategyComboBox.setSelectedItem("Near POI (NPOI)");
					
					recommendationAlgorithmComboBox.setSelectedItem("Near POI (NPOI)");
					
					typeNetworkComboBox.setSelectedItem("Peer To Peer (P2P)");
					
					propagationStrategyComboBox.setEnabled(true);
					propagationStrategyComboBox.setSelectedItem("Opportunistic");
					
					saveActionPerformed(e);
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		});
		
		GroupLayout gl_panel = new GroupLayout(panel);
		// gl_panel.setHorizontalGroup(
		// 	gl_panel.createParallelGroup(Alignment.LEADING)
		// 		.addGroup(gl_panel.createSequentialGroup()
		// 			.addGap(10)
		// 			.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
		// 				.addGroup(gl_panel.createSequentialGroup()
		// 					.addComponent(simulationLabel)
		// 					.addPreferredGap(ComponentPlacement.RELATED, 64, Short.MAX_VALUE)
		// 					.addComponent(btnFasttestusersrandcent)
		// 					.addPreferredGap(ComponentPlacement.RELATED)
		// 					.addComponent(btnFasttestusersrandP2P)
		// 					.addGap(30))
		// 				.addGroup(gl_panel.createSequentialGroup()
		// 					.addComponent(simulationPanel, GroupLayout.PREFERRED_SIZE, 480, GroupLayout.PREFERRED_SIZE)
		// 					// // Añadido por Nacho Palacio 2022-07-16
		// 					// .addGap(18)
		// 					// .addComponent(epidemicLabel)
		// 					// .addGap(11)
		// 					// .addComponent(epidemicPanel, GroupLayout.PREFERRED_SIZE, 480, GroupLayout.PREFERRED_SIZE)
		// 					.addPreferredGap(ComponentPlacement.RELATED)))
		// 			.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
		// 				.addComponent(experimentLabel)
		// 				.addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
		// 					.addComponent(saveButton)
		// 					.addComponent(experimentPanel, GroupLayout.PREFERRED_SIZE, 513, GroupLayout.PREFERRED_SIZE)))
		// 			.addContainerGap(225, Short.MAX_VALUE))
		// );

		// Modificado por Nacho Palacio 2022-07-16
		gl_panel.setHorizontalGroup(
		gl_panel.createParallelGroup(Alignment.LEADING)
			.addGroup(gl_panel.createSequentialGroup()
				.addGap(10)
				.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_panel.createSequentialGroup()
						.addComponent(simulationLabel)
						.addPreferredGap(ComponentPlacement.RELATED, 64, Short.MAX_VALUE)
						.addComponent(btnFasttestusersrandcent)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(btnFasttestusersrandP2P)
						.addGap(30))
					.addGroup(gl_panel.createSequentialGroup()
						.addComponent(simulationPanel, GroupLayout.PREFERRED_SIZE, 480, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)))
				.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
					.addComponent(experimentLabel)
					.addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
						.addComponent(saveButton)
						.addComponent(experimentPanel, GroupLayout.PREFERRED_SIZE, 513, GroupLayout.PREFERRED_SIZE)))
				.addGap(18)
				.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
					.addComponent(epidemicLabel)
					.addComponent(epidemicPanel, GroupLayout.PREFERRED_SIZE, 450, GroupLayout.PREFERRED_SIZE))
				.addContainerGap(225, Short.MAX_VALUE))
	);
		gl_panel.setVerticalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
							.addComponent(simulationLabel)
							.addComponent(experimentLabel)
							.addComponent(epidemicLabel))  // AÑADIR epidemicLabel aquí
						.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
							.addComponent(btnFasttestusersrandcent)
							.addComponent(btnFasttestusersrandP2P)))
					.addGap(11)
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addComponent(simulationPanel, GroupLayout.PREFERRED_SIZE, 751, GroupLayout.PREFERRED_SIZE)
						.addGroup(gl_panel.createSequentialGroup()
							.addComponent(experimentPanel, GroupLayout.PREFERRED_SIZE, 715, GroupLayout.PREFERRED_SIZE)
							.addGap(18)
							.addComponent(saveButton))
						.addComponent(epidemicPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					.addGap(25))
		);
		panel.setLayout(gl_panel);
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 1000, Short.MAX_VALUE)
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 830, Short.MAX_VALUE)
		);
		getContentPane().setLayout(groupLayout);

		pack();

		epidemicModelComboBox.setSelectedItem("SIMPLE_PROXIMITY");
		updateModelSpecificParameters();
	}
	
	/**
	 * Set Configuration window's dimensions taking into account the screen size
	 */
	private void setConfigurationPreferredSize() {
		
		// Get screen size
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		
		// Get taskbar's size
		Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
		int taskBarSize = scnMax.bottom;
		
		// Leave y position gap (Title bar)
		int titleBar = 30;
		
		// Set minimum size between screen and constants
		getContentPane().setPreferredSize(
				new Dimension(
						Math.min(X_CONFIG, screenSize.width),
						Math.min(Y_CONFIG, (screenSize.height - taskBarSize - titleBar))
				)
		);
	}
	
	/**
	 * Set dialog's location centering its x position and placing it on top (y position = 0).
	 */
	public void setDialogLocation() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
        int x = (int) rect.getMaxX()/2 - this.getWidth()/2;
        int y = 0;
        this.setLocation(x, y); // x centered, y top
	}

	public static void setPixelsPerMeter(double pixelsPerMeter) {
		if (simulation != null) {
			simulation.kmToPixel = pixelsPerMeter * 1000.0;
		}
	}
	
	/**
	 * Checks if the simulation parameters are valid.
	 * 
	 * @param timeAvailableUser
	 * @param delayObservingPainting
	 * @param timeForIteration
	 * @param screenRefreshTime
	 * @param timeForThePaths
	 * @param userVelocity
	 * @param kmToPixel
	 * @param ttl
	 * @param timeOnStairs
	 * @param minimumTimeToUpdateRecommendation
	 * @param communicationRange
	 * @param maxKnowledgeBaseSize
	 * @param communicationBandwidth
	 * @param latencyOfTransmission
	 * @param timeToChangeMood
	 * @return T/F
	 */
	private boolean checkParametersForSimulation(int timeAvailableUser, int delayObservingPainting, double timeForIteration,
			double screenRefreshTime, double timeForThePaths, double userVelocity, double kmToPixel, int ttl, int timeOnStairs,
			int minimumTimeToUpdateRecommendation, int communicationRange, int maxKnowledgeBaseSize, int communicationBandwidth,
			int latencyOfTransmission, int timeToChangeMood, double simulationDuration) { // ← NUEVO PARÁMETRO
		
		return (timeAvailableUser != 0 && delayObservingPainting != 0 && timeForIteration != 0 && screenRefreshTime != 0
				&& timeForThePaths != 0 && userVelocity != 0 && kmToPixel != 0 && ttl != 0 & timeOnStairs != 0 
				&& minimumTimeToUpdateRecommendation != 0 && communicationRange != 0 && maxKnowledgeBaseSize != 0 
				&& communicationBandwidth != 0 && latencyOfTransmission != 0 && timeToChangeMood != 0
				&& simulationDuration > 0);
	}

	/**
	 * Checks if parameters related to users are correct.
	 * 
	 * @param numberOfSpecialUser
	 * @param numberOfNonSpecialUser
	 * @param nonSpecialUserPaths
	 * @param pathStrategy
	 * @return T/F
	 */
	private boolean checkUsersInfo(int numberOfSpecialUser, int numberOfNonSpecialUser, String nonSpecialUserPaths, String pathStrategy) {
		
		return (numberOfSpecialUser != 0 && numberOfNonSpecialUser != 0 && !nonSpecialUserPaths.isEmpty() 
				&& !pathStrategy.equalsIgnoreCase("Select a strategy")); // PathStrategy really not needed if nonSpecialUserPaths is correct
	}
	
	
	private boolean checkAlgorithmAndNetworkParams(String recommendationAlgorithm, float thresholdRecommendation, double thresholdSimilarity,
			int howMany, String networkType, String propagationStrategy, double probabilityUserDisobedience, int numberVoteReceived) {
		
		// PREVIOUS:
		/*return (!recommendationAlgorithm.equalsIgnoreCase("Select an algorithm") && thresholdRecommendation != 0 && howMany != 0 && probabilityUserDisobedience != 0 && numberVoteReceived != 0
				&& thresholdSimilarity != 0 && (!propagationStrategy.equalsIgnoreCase("Select a strategy") && networkType.equalsIgnoreCase("Peer To Peer (P2P)"))
				|| (propagationStrategy.equalsIgnoreCase("Select a strategy") && networkType.equalsIgnoreCase("Centralized (Centralized)")));
		 */
		
		// To allow Know-It-All and K-Ideal
		boolean notNullParameters = 
				(!recommendationAlgorithm.equalsIgnoreCase("Select an algorithm") && thresholdRecommendation != 0 && howMany != 0
				&& probabilityUserDisobedience != 0 && numberVoteReceived != 0 && thresholdSimilarity != 0);
		
		boolean correctAlgorithmNetworkCorrelation = /*(
				(!propagationStrategy.equalsIgnoreCase("Select a strategy") && networkType.equalsIgnoreCase("Peer To Peer (P2P)"))
				|| (propagationStrategy.equalsIgnoreCase("Select a strategy") && networkType.equalsIgnoreCase("Centralized (Centralized)"))
				|| ((recommendationAlgorithm.equalsIgnoreCase("K-Ideal (K-Ideal)") || recommendationAlgorithm.equalsIgnoreCase("Know-It-All (Know-It-All)")) && networkType.equalsIgnoreCase("Select a type of network")));*/
				
				(!recommenders.checkRecommenderUsesP2P(recommendationAlgorithm))
				|| (recommenders.checkRecommenderUsesP2P(recommendationAlgorithm) && networkType.equalsIgnoreCase("Centralized (Centralized)")) 
				|| (recommenders.checkRecommenderUsesP2P(recommendationAlgorithm) && networkType.equalsIgnoreCase("Peer To Peer (P2P)") && !propagationStrategy.equalsIgnoreCase("Select a strategy"));
		
		return (notNullParameters && correctAlgorithmNetworkCorrelation);
	}


	/**
	 * Añadido por Nacho Palacio 2025-07-15.
	 * Gets the number of pixels that represent one kilometer
	 * @return pixels per kilometer, or default value if simulation not configured
	 */
	public static double getPixelsPerKilometer() {
		if (simulation != null) {
			return simulation.kmToPixel;
		}
		return 6597.0; // Valor por defecto del campo de texto
	}

	/**
	 * Añadido por Nacho Palacio 2025-07-15.
	 * Gets the number of pixels that represent one meter
	 * @return pixels per meter, or default value if simulation not configured
	 */
	public static double getPixelsPerMeter() {
		if (simulation != null) {
			return simulation.kmToPixel / 1000.0;
		}
		return 6.597; // Valor por defecto: 6597 píxeles/km ÷ 1000 = 6.597 píxeles/metro
	}

	/**
	 * Añadido por Nacho Palacio 2022-07-16.
	 * Validates epidemic parameters
	 */
	private boolean checkEpidemicParameters(String epidemicModel, int initialInfected, double maskCompliance, 
                                      double immunePopulation, double superSpreaderProb) {
		return epidemicModel != null && 
			initialInfected > 0 && 
			maskCompliance >= 0.0 && maskCompliance <= 1.0 &&
			immunePopulation >= 0.0 && immunePopulation <= 1.0 &&
			superSpreaderProb >= 0.0 && superSpreaderProb <= 1.0;
	}
	

	/**
	 * Añadido por Nacho Palacio 2025-09-14
	 * Updates the model-specific parameters panel based on the selected epidemic model.
	 */
	private void updateModelSpecificParameters() {
		modelSpecificPanel.removeAll();
		
		String selectedModel = (String) epidemicModelComboBox.getSelectedItem();
		
		switch (selectedModel) {
			case "SIMPLE_PROXIMITY":
				addSimpleProximityParameters();
				break;
			case "AEROSOL_PENG":
				addPengParameters();
				break;
			case "AEROSOL_LELIEVELD":
				addLelieveldParameters();
				break;
		}
    		
		modelSpecificPanel.revalidate();
		modelSpecificPanel.repaint();
		
		if (modelSpecificPanel.getParent() != null) {
			modelSpecificPanel.getParent().revalidate();
			modelSpecificPanel.getParent().repaint();
		}
	}

	private void addSimpleProximityParameters() {
		JLabel lblMaxDistance = new JLabel("Max transmission distance [m]");
		lblMaxDistance.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		maxTransmissionDistanceTextField = new JTextField("1.5");
		maxTransmissionDistanceTextField.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		JLabel lblBaseProb = new JLabel("Base transmission probability [0-1]");
		lblBaseProb.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		baseTransmissionProbTextField = new JTextField("0.01");
		baseTransmissionProbTextField.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		JLabel lblMinDuration = new JLabel("Min contact duration [s]");
		lblMinDuration.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		minContactDurationTextField = new JTextField("200");
		minContactDurationTextField.setFont(new Font("SansSerif", Font.PLAIN, 12));
    
		GroupLayout layout = new GroupLayout(modelSpecificPanel);
		layout.setHorizontalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
							.addComponent(lblMaxDistance)
							.addGap(10)
							.addComponent(maxTransmissionDistanceTextField, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE))
						.addGroup(layout.createSequentialGroup()
							.addComponent(lblBaseProb)
							.addGap(10)
							.addComponent(baseTransmissionProbTextField, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE))
						.addGroup(layout.createSequentialGroup()
							.addComponent(lblMinDuration)
							.addGap(10)
							.addComponent(minContactDurationTextField, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)))
					.addContainerGap())
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblMaxDistance)
						.addComponent(maxTransmissionDistanceTextField))
					.addGap(10)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblBaseProb)
						.addComponent(baseTransmissionProbTextField))
					.addGap(10)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblMinDuration)
						.addComponent(minContactDurationTextField))
					.addContainerGap())
		);
		modelSpecificPanel.setLayout(layout);
		modelSpecificPanel.setPreferredSize(new Dimension(400, 200));
    	modelSpecificPanel.setMinimumSize(new Dimension(400, 150));
	}

	private void addPengParameters() {
		JLabel lblQuantaEmission = new JLabel("Quanta emission rate [quanta/h]");
		lblQuantaEmission.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		quantaEmissionRateTextField = new JTextField("232.5");
		quantaEmissionRateTextField.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		JLabel lblBreathingRate = new JLabel("Breathing rate [m³/h]");
		lblBreathingRate.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		breathingRateTextField = new JTextField("0.72");
		breathingRateTextField.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		JLabel lblDepositionRate = new JLabel("Deposition rate [h⁻¹]");
		lblDepositionRate.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		depositionRateTextField = new JTextField("0.3");
		depositionRateTextField.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		GroupLayout layout = new GroupLayout(modelSpecificPanel);
		layout.setHorizontalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
							.addComponent(lblQuantaEmission)
							.addGap(10)
							.addComponent(quantaEmissionRateTextField, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE))
						.addGroup(layout.createSequentialGroup()
							.addComponent(lblBreathingRate)
							.addGap(10)
							.addComponent(breathingRateTextField, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE))
						.addGroup(layout.createSequentialGroup()
							.addComponent(lblDepositionRate)
							.addGap(10)
							.addComponent(depositionRateTextField, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)))
					.addContainerGap())
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblQuantaEmission)
						.addComponent(quantaEmissionRateTextField))
					.addGap(10)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblBreathingRate)
						.addComponent(breathingRateTextField))
					.addGap(10)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblDepositionRate)
						.addComponent(depositionRateTextField))
					.addContainerGap())
		);
		modelSpecificPanel.setLayout(layout);
		modelSpecificPanel.setPreferredSize(new Dimension(400, 200));
		modelSpecificPanel.setMinimumSize(new Dimension(400, 150));
	}

	private void addLelieveldParameters() {
		JLabel lblViralLoadHigh = new JLabel("Viral load high [copies/cm³]");
		lblViralLoadHigh.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		viralLoadHighTextField = new JTextField("1.5E7");
		viralLoadHighTextField.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		JLabel lblViralLoadSuper = new JLabel("Viral load superspreader [copies/cm³]");
		lblViralLoadSuper.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		viralLoadSuperTextField = new JTextField("5E9");
		viralLoadSuperTextField.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		JLabel lblInfectiousDose = new JLabel("Infectious dose D50 [copies]");
		lblInfectiousDose.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		infectiousDoseTextField = new JTextField("100");
		infectiousDoseTextField.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		JLabel lblDepositionProb = new JLabel("Deposition probability [0-1]");
		lblDepositionProb.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		depositionProbabilityTextField = new JTextField("0.5");
		depositionProbabilityTextField.setFont(new Font("SansSerif", Font.PLAIN, 12));
		
		GroupLayout layout = new GroupLayout(modelSpecificPanel);
		layout.setHorizontalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
							.addComponent(lblViralLoadHigh)
							.addGap(10)
							.addComponent(viralLoadHighTextField, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE))
						.addGroup(layout.createSequentialGroup()
							.addComponent(lblViralLoadSuper)
							.addGap(10)
							.addComponent(viralLoadSuperTextField, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE))
						.addGroup(layout.createSequentialGroup()
							.addComponent(lblInfectiousDose)
							.addGap(10)
							.addComponent(infectiousDoseTextField, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE))
						.addGroup(layout.createSequentialGroup()
							.addComponent(lblDepositionProb)
							.addGap(10)
							.addComponent(depositionProbabilityTextField, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)))
					.addContainerGap())
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblViralLoadHigh)
						.addComponent(viralLoadHighTextField))
					.addGap(8)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblViralLoadSuper)
						.addComponent(viralLoadSuperTextField))
					.addGap(8)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblInfectiousDose)
						.addComponent(infectiousDoseTextField))
					.addGap(8)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(lblDepositionProb)
						.addComponent(depositionProbabilityTextField))
					.addContainerGap())
		);
		modelSpecificPanel.setLayout(layout);
		modelSpecificPanel.setPreferredSize(new Dimension(400, 200));
    	modelSpecificPanel.setMinimumSize(new Dimension(400, 150));
	}
	
	
	/**
	 * All Configuration window elements.
	 */
	private JLabel JLabel9;
	private JLabel experimentLabel;
	private JLabel jLabel10;
	private JLabel jLabel11;
	private JLabel jLabel14;
	private JLabel jLabel15;
	private JLabel jLabel16;
	private JLabel jLabel17;
	private JLabel jLabel18;
	private JLabel jLabel19;
	private JLabel simulationLabel;
	private JLabel jLabel3;
	private JLabel jLabel4;
	private JLabel jLabel5;
	private JLabel jLabel6;
	private JLabel jLabel7;
	private JLabel jLabel8;
	private JLabel jLabel9;
	private JLabel lblNewLabel_2;
	private JPanel experimentPanel;
	private JPanel simulationPanel;
	private JSeparator jSeparator1;
	private JSeparator separator_1;

	public JTextField communicationBandwidthTextField;
	public JTextField communicationRangeTextField;
	public JTextField delayObservingItemTextField;
	public JTextField howManyTextField;
	public JTextField kmToPixelTextField;
	public JTextField latencyOfTransmissionTextField;
	public JTextField maximumKnowledgeBaseSizeTextField;
	public JTextField numberOfSpecialUsersTextField;
	public JTextField numberVoteReceivedTextField;
	@SuppressWarnings("rawtypes")
	public JComboBox pathStrategyComboBox;
	public JTextField probabilityUserDisobedienceTextField;
	@SuppressWarnings("rawtypes")
	public JComboBox recommendationAlgorithmComboBox;
	@SuppressWarnings("rawtypes")
	public JComboBox propagationStrategyComboBox;
	public JButton saveButton;
	public JTextField screenRefreshTimeTextField;
	public JTextField timeAvailableForUserTextField;
	public JTextField timeForIterationTextField;
	public JTextField timeForThePathsTextField;
	public JTextField ttlTextField;
	public JTextField userSpeedTextField;
	public JTextField thresholdRecommendationTextField;
	public JTextField timeOnStairsTextField;
	public JTextField minimumTimeToUpdateRecommendationTextField;
	public JTextField nonSpecialUserPathsJTextField;
	//@SuppressWarnings("rawtypes")
	//public JComboBox entranceDoorComboBox;
	public JTextField numberOfNonSpecialUsersTextField;
	private JCheckBox ifGenerateuserPathCheckBox;
	private JTextField thresholdSimilarityTextField;
	@SuppressWarnings("rawtypes")
	public JComboBox typeNetworkComboBox;
	public JTextField timeChangeMoodTextField;
	public JCheckBox chckbxUseFixedSeed;
	private JLabel jLabelSeed;
	private JTextField seedNumber;
	
	private JButton chooseMap;
	private String pathMap;

	// Añadido por Nacho Palacio 2022-07-16
	@SuppressWarnings("rawtypes")
	public JComboBox epidemicModelComboBox;
	public JTextField initialInfectedTextField;
	public JTextField maskComplianceTextField;

	// Añadido por Nacho Palacio 2025-09-14
	public JTextField defaultVentilationRateTextField;
	public JTextField virusDecayRateTextField;
	public JTextField maskExhalationEffTextField;
	public JTextField maskInhalationEffTextField;

	public JTextField maxTransmissionDistanceTextField;
	public JTextField baseTransmissionProbTextField;
	public JTextField minContactDurationTextField;

	public JTextField quantaEmissionRateTextField;
	public JTextField breathingRateTextField;
	public JTextField depositionRateTextField;

	public JTextField viralLoadHighTextField;
	public JTextField viralLoadSuperTextField;
	public JTextField infectiousDoseTextField;
	public JTextField depositionProbabilityTextField;

	private JPanel modelSpecificPanel;
}
