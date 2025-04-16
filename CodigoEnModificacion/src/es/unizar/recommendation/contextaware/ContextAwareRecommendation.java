/*
 * @(#)Context.java  1.0.0  13/10/14
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
package es.unizar.recommendation.contextaware;

import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import es.unizar.database.DBDataModel;

/**
 * Context-Aware recommendations.
 * 
 * @author Maria del Carmen Rodriguez-Hernandez
 *
 */
public abstract class ContextAwareRecommendation {

	protected DBDataModel dataModel;

	public ContextAwareRecommendation(DBDataModel dataModel) {
		this.setDataModel(dataModel);
	}

	public abstract List<RecommendedItem> recommend(long userID, int howMany) throws TasteException;

	public abstract float estimatePreference(long userID, long itemID) throws TasteException;

	// Gets and sets:
	public DBDataModel getDataModel() {
		return dataModel;
	}

	public void setDataModel(DBDataModel dataModel) {
		this.dataModel = dataModel;
	}
}
