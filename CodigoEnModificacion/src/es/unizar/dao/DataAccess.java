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
package es.unizar.dao;

import java.sql.ResultSet;
import java.util.List;

/**
 * Interface that allows to access to several useful data.
 *
 * @author Maria del Carmen Rodriguez-Hernandez and Alejandro Piedrafita Barrantes
 */
public interface DataAccess {

	/**
	 * Gets the users, items and ratings.
	 *
	 * @return ResultSet.
	 */
	public List<String> getUserItemRating();

	/**
	 * Gets the users, items, contexts and ratings.
	 *
	 * @return ResultSet.
	 */
	public ResultSet getUserItemContextRating();

	List<String> getUserItemRatingFrom(long userID);

	float getPreferenceFor(long userId, long itemId, long contextId);

	long getContextIDFor(List<Integer> currentContextValues);

	List<String> getUserItemContextRatingFor(long userID);

	List<String> getUserItemContextRatingRandomFor(long userID);
}
