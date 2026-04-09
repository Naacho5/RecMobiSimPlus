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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.mahout.cf.taste.common.NoSuchItemException;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveArrayIterator;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.AbstractDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericItemPreferenceArray;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;

import com.google.common.collect.Lists;

import es.unizar.dao.DataAccessLayer;
import es.unizar.util.ElementIdMapper;

/**
 * Data model extracted from a database. File taken from MOONRISE.jar (and optimized).
 *
 * @author Maria del Carmen Rodriguez-Hernandez and Alejandro Piedrafita Barrantes
 */
public class DBDataModel extends AbstractDataModel {

	private static final long serialVersionUID = 1L;

	// Added by Nacho Palacio 2025-12-08
	private static final int MAX_POOL_SIZE = 10;
    private static final ConcurrentHashMap<String, Queue<DBDataModel>> connectionPools = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Object> poolLocks = new ConcurrentHashMap<>();
	private boolean fromPool = false;

	//private static final Logger log = LoggerFactory.getLogger(DBDataModel.class);

	private final long[] userIDs;
	private final long[] itemIDs;
	private final FastByIDMap<PreferenceArray> preferenceFromUsers;
	private final FastByIDMap<PreferenceArray> preferenceForItems;
	private final DataAccessLayer dataAccess;
	public String dbURL;

	/**
	 *
	 * @param dbURL
	 * @throws SQLException
	 */
	public DBDataModel(String dbURL, Database db, int numUsers) throws SQLException {
		dataAccess = new DataAccessLayer(dbURL, db);		

		this.dbURL = dbURL;
		//this.userIDs = dataAccess.getUserIDs();

		boolean usersAdded = dataAccess.ensureRequiredUsers(numUsers); // Added by Nacho Palacio 2025-04-14.

		/* Added by Nacho Palacio 2025-04-15. */
		if (usersAdded) {
			try {
				Thread.sleep(1000); // Esperar 1 segundo
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		long[] correctUserIDs = dataAccess.getUserIDs();

		/* Added by Nacho Palacio 2025-04-14. */
		if (numUsers > correctUserIDs.length) {
			numUsers = correctUserIDs.length; // Limita a los usuarios disponibles
		}

		this.userIDs = Arrays.copyOfRange(correctUserIDs, 0, numUsers);
		
		this.preferenceFromUsers = new FastByIDMap<PreferenceArray>();
		PreferenceArray preferenceArray = null;
		Map<Long, Integer> hashWithNumberItemsByUser = dataAccess.getHashWithNumberItemsByUser();
		
		for (Map.Entry<Long, Integer> entry : hashWithNumberItemsByUser.entrySet()) {
			long userIDKey = entry.getKey();
			int numberOfItems = entry.getValue(); // Added by Nacho Palacio 2025-04-15.

			if (numberOfItems > 0) { // Added by Nacho Palacio 2025-04-15. Modified by Nacho Palacio 2025-05-08, antes == 0
				// System.out.println("DBDataModel: Loading preferences for user ID " + userIDKey + " with " + numberOfItems + " items.");
				List<String> listByUser = dataAccess.getUserItemRatingFrom(userIDKey);
				// System.out.println("DBDataModel: Retrieved " + listByUser.size() + " preferences for user ID " + userIDKey + ".");
				// System.out.println("DBDataModel: listByUser: " + listByUser);
				// int numberOfItems = hashWithNumberItemsByUser.get(userIDKey);
				preferenceArray = new GenericUserPreferenceArray(numberOfItems);

				// Modified by Nacho Palacio 2025-05-08.
				for (int k = 0; k < listByUser.size(); k++) {
					String user_item_rating = listByUser.get(k);
					String array[] = user_item_rating.split(";");
					long userID = Long.valueOf(array[0]).longValue();
					long itemID = Long.valueOf(array[1]).longValue();
					float rating = Float.valueOf(array[2]).floatValue();
					// long itemExternalID = Long.valueOf(array[3]).longValue();
					
					// Convertir los IDs externos a internos
					long internalUserID = userID; // Los IDs de usuario ya están en el formato correcto
					// long internalItemID = ElementIdMapper.convertToRangeId(itemID, ElementIdMapper.CATEGORY_ITEM);
					long internalItemID = ElementIdMapper.convertToRangeId(itemID, ElementIdMapper.CATEGORY_ITEM);

					
					preferenceArray.setUserID(k, internalUserID);
					// preferenceArray.setItemID(k, internalItemID);
					preferenceArray.setItemID(k, itemID); // Modified by Nacho Palacio 2025-10-26
					preferenceArray.setValue(k, rating);
					// System.out.println("DBDataModel:   Preference " + k + ": userID=" + internalUserID + ", itemID=" + itemID + " (internal: " + internalItemID + "), rating=" + rating);
				}
			} else { 
				preferenceArray = new GenericUserPreferenceArray(0);
			}
			this.preferenceFromUsers.put(userIDKey, preferenceArray);
		}
		// System.out.println("DBDataModel: Loaded preferences from " + hashWithNumberItemsByUser.size() + " users.");

		FastByIDMap<Collection<Preference>> prefsForItems = new FastByIDMap<Collection<Preference>>();
		FastIDSet itemIDSet = new FastIDSet();
		int currentCount = 0;
		float maxPrefValue = Float.NEGATIVE_INFINITY;
		float minPrefValue = Float.POSITIVE_INFINITY;
		for (Map.Entry<Long, PreferenceArray> entry : preferenceFromUsers.entrySet()) {
			PreferenceArray prefs = entry.getValue();

			if (prefs.length() > 0) {
				prefs.sortByItem();
				for (Preference preference : prefs) {
					long itemID = preference.getItemID();
					itemIDSet.add(itemID);
					Collection<Preference> prefsForItem = prefsForItems.get(itemID);
					if (prefsForItem == null) {
						prefsForItem = Lists.newArrayListWithCapacity(2);
						prefsForItems.put(itemID, prefsForItem);
					}
					prefsForItem.add(preference);
					float value = preference.getValue();
					if (value > maxPrefValue) {
						maxPrefValue = value;
					}
					if (value < minPrefValue) {
						minPrefValue = value;
					}
				}
			}
			if (++currentCount % 10000 == 0) {
				// log.info("Processed {} users", currentCount);
			}
		}
		// log.info("Processed {} users", currentCount);
		setMinPreference(minPrefValue);
		setMaxPreference(maxPrefValue);
		this.itemIDs = itemIDSet.toArray();
		itemIDSet = null; // Might help GC -- this is big
		Arrays.sort(itemIDs);
		this.preferenceForItems = toDataMap(prefsForItems, false);
		for (Map.Entry<Long, PreferenceArray> entry : preferenceForItems.entrySet()) {
			entry.getValue().sortByUser();
		}
		Arrays.sort(userIDs);
	}

	/**
	 * Swaps, in-place, {@link List}s for arrays in {@link Map} values .
	 *
	 * @param data
	 * @param byUser
	 * @return input value
	 */
	@SuppressWarnings("unchecked")
	public static FastByIDMap<PreferenceArray> toDataMap(FastByIDMap<Collection<Preference>> data, boolean byUser) {
		for (Map.Entry<Long, Object> entry : ((FastByIDMap<Object>) (FastByIDMap<?>) data).entrySet()) {
			List<Preference> prefList = (List<Preference>) entry.getValue();
			entry.setValue(byUser ? new GenericUserPreferenceArray(prefList) : new GenericItemPreferenceArray(prefList));
		}
		return (FastByIDMap<PreferenceArray>) (FastByIDMap<?>) data;
	}

	/**
	 * Gets the user identifiers.
	 *
	 * @return The user identifiers.
	 * @throws TasteException
	 */
	@Override
	public LongPrimitiveIterator getUserIDs() throws TasteException {
		return new LongPrimitiveArrayIterator(userIDs);
	}

	/**
	 * Gets the preferences from user.
	 *
	 * @param userID
	 *            The user identifier.
	 * @return The preferences from user.
	 * @throws TasteException
	 */
	@Override
	public PreferenceArray getPreferencesFromUser(long userID) throws TasteException { // THROWS EXCEPTION WHEN NO PREFERENCES
		PreferenceArray prefs = preferenceFromUsers.get(userID);
		if (prefs == null) {
			throw new NoSuchUserException(userID);
		}
		return prefs;
	}

	/**
	 * Gets the preferences from user sorted by rating in descending order.
	 * @param userID The user identifier.
	 * @return PreferenceArray sorted by rating.
	 * @throws TasteException
	 */
	public PreferenceArray getPreferencesFromUserSortedByRating(long userID) throws TasteException {
		PreferenceArray prefs = getPreferencesFromUser(userID);
		
		// Convertir a lista para ordenar
		List<Preference> prefList = new ArrayList<>();
		for (int i = 0; i < prefs.length(); i++) {
			prefList.add(prefs.get(i));
		}
		
		// Ordenar por rating descendente
		prefList.sort((p1, p2) -> Float.compare(p2.getValue(), p1.getValue()));
		
		// Reconstruir PreferenceArray
		GenericUserPreferenceArray sortedPrefs = new GenericUserPreferenceArray(prefList.size());
		for (int i = 0; i < prefList.size(); i++) {
			Preference p = prefList.get(i);
			sortedPrefs.setUserID(i, p.getUserID());
			sortedPrefs.setItemID(i, p.getItemID());
			sortedPrefs.setValue(i, p.getValue());
		}
		
		return sortedPrefs;
	}

	/**
	 * Gets the item identifier from user identifier.
	 *
	 * @param userID
	 *            The user identifier.
	 * @return The item identifier from user identifier.
	 * @throws TasteException
	 */
	@Override
	public FastIDSet getItemIDsFromUser(long userID) throws TasteException {
		PreferenceArray prefs = getPreferencesFromUser(userID);
		int size = prefs.length();
		FastIDSet result = new FastIDSet(size);
		for (int i = 0; i < size; i++) {
			result.add(prefs.getItemID(i));
		}

		return result;
	}

	/**
	 * Gets the item identifiers.
	 *
	 * @return The item identifiers.
	 * @throws TasteException
	 */
	@Override
	public LongPrimitiveIterator getItemIDs() throws TasteException {
		return new LongPrimitiveArrayIterator(itemIDs);
	}

	/**
	 * Gets the preferences from item.
	 *
	 * @param itemID
	 *            The item identifier.
	 * @return The preferences from item.
	 * @throws TasteException
	 */
	@Override
	public PreferenceArray getPreferencesForItem(long itemID) throws TasteException {
		PreferenceArray prefs = preferenceForItems.get(itemID);
		if (prefs == null) {
			throw new NoSuchItemException(itemID);
		}
		return prefs;
	}

	/**
	 * Gets the user preference value of an item.
	 *
	 * @param userID
	 *            The user identifier.
	 * @param itemID
	 *            The item identifier.
	 * @return The user preference value of an item.
	 * @throws TasteException
	 */
	@Override
	public Float getPreferenceValue(long userID, long itemID) throws TasteException {
		// Modified by Nacho Palacio 2025-05-08.
		long internalItemID = ElementIdMapper.isInCorrectRange(itemID, ElementIdMapper.CATEGORY_ITEM) 
                         ? itemID 
                         : ElementIdMapper.convertToRangeId(itemID, ElementIdMapper.CATEGORY_ITEM);
    

		PreferenceArray prefs = getPreferencesFromUser(userID);
		int size = prefs.length();
		for (int i = 0; i < size; i++) {
			if (prefs.getItemID(i) == internalItemID) {
				return prefs.getValue(i);
			}
		}

		
		return null;
	}

	// SELECT rating FROM user_item_context WHERE id_user==1 and id_item==1 and
	// id_context==1
	/**
	 * Gets the user preference value of an item.
	 *
	 * @param userID
	 *            The user identifier.
	 * @param itemID
	 *            The item identifier.
	 * @return The user preference value of an item.
	 * @throws TasteException
	 */
	public float getPreferenceValue(long userID, long itemID, long contextID) throws TasteException {
		return dataAccess.getPreferenceFor(userID, itemID, contextID);
	}

	/**
	 *
	 * @param userID
	 * @param itemID
	 * @return
	 * @throws TasteException
	 */
	@Override
	public Long getPreferenceTime(long userID, long itemID) throws TasteException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Gets the number of items.
	 *
	 * @return The number of items.
	 * @throws TasteException
	 */
	@Override
	public int getNumItems() throws TasteException {
		return itemIDs.length;
	}

	/**
	 * Gets the number of users.
	 *
	 * @return The number of users.
	 * @throws TasteException
	 */
	@Override
	public int getNumUsers() throws TasteException {
		return userIDs.length;
	}

	/**
	 * Gets the number of users with preference by an item.
	 *
	 * @param itemID
	 *            The item identifier.
	 * @return The number of users with preference by an item.
	 * @throws TasteException
	 */
	@Override
	public int getNumUsersWithPreferenceFor(long itemID) throws TasteException {
		PreferenceArray prefs1 = preferenceForItems.get(itemID);
		return prefs1 == null ? 0 : prefs1.length();
	}

	/**
	 * Gets the number of users with preference by two items.
	 *
	 * @param itemID1
	 *            The item identifier 1.
	 * @param itemID2
	 *            The item identifier 2.
	 * @return The number of users with preference by two items.
	 * @throws TasteException
	 */
	@Override
	public int getNumUsersWithPreferenceFor(long itemID1, long itemID2) throws TasteException {
		PreferenceArray prefs1 = preferenceForItems.get(itemID1);
		if (prefs1 == null) {
			return 0;
		}
		PreferenceArray prefs2 = preferenceForItems.get(itemID2);
		if (prefs2 == null) {
			return 0;
		}

		int size1 = prefs1.length();
		int size2 = prefs2.length();
		int count = 0;
		int i = 0;
		int j = 0;
		long userID1 = prefs1.getUserID(0);
		long userID2 = prefs2.getUserID(0);
		while (true) {
			if (userID1 < userID2) {
				if (++i == size1) {
					break;
				}
				userID1 = prefs1.getUserID(i);
			} else if (userID1 > userID2) {
				if (++j == size2) {
					break;
				}
				userID2 = prefs2.getUserID(j);
			} else {
				count++;
				if (++i == size1 || ++j == size2) {
					break;
				}
				userID1 = prefs1.getUserID(i);
				userID2 = prefs2.getUserID(j);
			}
		}
		return count;
	}

	/**
	 * Sets the preference value of the user about an item.
	 *
	 * @param userID
	 *            The user identifier.
	 * @param itemID
	 *            The item identifier.
	 * @param value
	 *            The preference value.
	 * @throws TasteException
	 */
	@Override
	public void setPreference(long userID, long itemID, float value) throws TasteException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Removes the preference value of the user about an item.
	 *
	 * @param userID
	 *            The user identifier.
	 * @param itemID
	 *            The item identifier.
	 * @throws TasteException
	 */
	@Override
	public void removePreference(long userID, long itemID) throws TasteException {
		throw new UnsupportedOperationException();
	}

	/**
	 * If has preference values.
	 *
	 * @return True if has preference values and false in the other case.
	 */
	@Override
	public boolean hasPreferenceValues() {
		return true;
	}

	/**
	 * Refresh.
	 *
	 * @param alreadyRefreshed
	 */
	@Override
	public void refresh(Collection<Refreshable> alreadyRefreshed) {
		// Does nothing
	}

	/**
	 * Gets a list with the context variable names.
	 *
	 * @return A list with the context variable names.
	 * @throws TasteException
	 * @throws SQLException
	 */
	public List<String> getVariableNames() throws TasteException, SQLException {
		return dataAccess.getVariableNames();
	}

	/**
	 * Gets the variable name from a variable value.
	 *
	 * @param variableValue
	 *            The variable value.
	 * @return The variable name from a variable value.
	 */
	public List<String> getVariableNames(String variableValue) {
		return dataAccess.getVariableNameFromVariableValue(variableValue);
	}

	/**
	 * Gets a list with the possible values of a context variable.
	 *
	 * @param variableName
	 *            The name of the a variable.
	 * @return A list with the possible values of a context variable.
	 */
	public List<String> getPossibleVariableValues(String variableName) {
		return dataAccess.getPossibleVariableValues(variableName);
	}

	/**
	 * Gets a list with the names and values of context variables from userID
	 * and itemID.
	 *
	 * @param userID
	 *            The user identifier.
	 * @param itemID
	 *            The item identifier.
	 * @return A list with the names and values of context variables from userID
	 *         and itemID.
	 */
	public List<String> getVariableNameAndValue(long userID, long itemID) {
		return dataAccess.getVariableNameAndValue(userID, itemID);
	}

	/**
	 * Gets a list with the names and weights of context variables.
	 *
	 * @return A list with the names and weights of context variables.
	 */
	/*public List<String> getVariableNameAndWeight(long userID) {
		return dataAccess.getVariableNameAndWeight(userID);
	}*/

	/**
	 * Determine the distance between two variable values that are similars
	 *
	 * @param valueX
	 * @param valueY
	 * @return
	 */
	public double distanceSoftVariableValues(String variableValueX, String variableValueY) {
		return dataAccess.distanceSoftVariableValues(variableValueX, variableValueY);
	}

	/**
	 * Gets the number of item features.
	 *
	 * @return The number of item features.
	 */
	public int getNumberItemFeatures() {
		return dataAccess.getNumberItemFeatures();
	}

	/**
	 * Gets the feature names and values of an item.
	 *
	 * @param itemId
	 *            Item identifier.
	 * @return The features of an item.
	 * @throws TasteException
	 */
	/*public List<String> getNamesAndValuesOfFeaturesFromItem(long itemID) throws TasteException {
		return dataAccess.getNamesAndValuesOfFeaturesFromItem(itemID);
	}*/

	/**
	 * Gets a list with all the feature names.
	 *
	 * @return List with all the feature names.
	 */
	/*public List<String> getItemFeatureNames() {
		return dataAccess.getItemFeatureNames();
	}*/
	
	/**
	 * Gets the context ID from a list of context values.
	 *
	 * @param currentContextValues
	 *            A list of current context values.
	 * @return long.
	 */
	public long getContextIDFor(List<Integer> currentContextValues) {
		return dataAccess.getContextIDFor(currentContextValues);
	}
	
	/**
	 * Disconnects from database.
	 */
	public void disconnect() throws SQLException{
		dataAccess.disconnect();
	}
	
	/**
	 * Returns dataAccessLayer. To optimize PostfilteringBasedRecommendation's FilterCandidateItemsByLocation
	 */
	public DataAccessLayer getDataAccessLayer(){
		return dataAccess;
	}

	// Added by Nacho Palacio 2025-05-08.
	/**
	 * Convierte un ID externo de ítem a su formato interno.
	 * @param externalItemId El ID externo del ítem
	 * @return El ID interno correspondiente
	 */
	public static long convertExternalToInternalItemId(long externalItemId) {
		return ElementIdMapper.convertToRangeId(externalItemId, ElementIdMapper.CATEGORY_ITEM);
	}

	/**
	 * Convierte un ID interno de ítem a su formato externo.
	 * @param internalItemId El ID interno del ítem
	 * @return El ID externo correspondiente
	 */
	public static long convertInternalToExternalItemId(long internalItemId) {
		return ElementIdMapper.getBaseId(internalItemId);
	}

	/**
     * Obtains an instance from the pool or creates a new one.
     */
    public static DBDataModel getFromPool(String dbURL, Database db, int numUsers) throws SQLException {
        poolLocks.putIfAbsent(dbURL, new Object());
        
        synchronized (poolLocks.get(dbURL)) {
            Queue<DBDataModel> pool = connectionPools.get(dbURL);
            
            if (pool != null && !pool.isEmpty()) {
                DBDataModel model = pool.poll();
                if (model != null) {
                    model.fromPool = true;
                    return model;
                }
            }
        }
        
        // No hay conexiones disponibles, crear una nueva
        DBDataModel newModel = new DBDataModel(dbURL, db, numUsers);
        newModel.fromPool = false;
        return newModel;
    }

    /**
     * Returns this instance to the pool.
     */
    public void returnToPool() {
        poolLocks.putIfAbsent(dbURL, new Object());
        
        synchronized (poolLocks.get(dbURL)) {
            Queue<DBDataModel> pool = connectionPools.computeIfAbsent(dbURL, k -> new ConcurrentLinkedQueue<>());
            
            if (pool.size() < MAX_POOL_SIZE) {
                pool.offer(this);
            } else {
                // Pool lleno, cerrar conexión
                try {
                    this.disconnect();
                } catch (SQLException e) {
                    System.err.println("Error cerrando conexión excedente: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Clears all connection pools, closing all connections.
     */
    public static void clearAllPools() {
        for (Map.Entry<String, Queue<DBDataModel>> entry : connectionPools.entrySet()) {
            Queue<DBDataModel> pool = entry.getValue();
            DBDataModel model;
            while ((model = pool.poll()) != null) {
                try {
                    model.disconnect();
                } catch (SQLException e) {
                    System.err.println("Error limpiando pool: " + e.getMessage());
                }
            }
        }
        connectionPools.clear();
        System.out.println("✅ Todos los pools de conexiones limpiados");
    }
}
