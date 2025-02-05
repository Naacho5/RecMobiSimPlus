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

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.recommender.Recommender;

import es.unizar.database.DBDataModel;
import es.unizar.recommendation.SVDBasedRecommendation;

/**
 * Filtering based recommendations.
 * 
 * @author Maria del Carmen Rodriguez-Hernandez
 *
 */
public abstract class FilteringBasedRecommendation extends ContextAwareRecommendation {

	private double threshold;
	private Recommender recommender;
	private int numFeatures, initialSteps;

	public FilteringBasedRecommendation(DBDataModel dataModel, double threshold) throws TasteException {
		super(dataModel);
		this.threshold = threshold;
		this.numFeatures = 10;
		this.initialSteps = 10;
		// XXX Data model null no hacer
		this.recommender = new SVDBasedRecommendation(dataModel, numFeatures, initialSteps);
	}

	// Gets and sets
	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public Recommender getRecommender() {
		return recommender;
	}

	public void setRecommender(Recommender recommender) {
		this.recommender = recommender;
	}

	public int getNumFeatures() {
		return numFeatures;
	}

	public void setNumFeatures(int numFeatures) {
		this.numFeatures = numFeatures;
	}

	public int getInitialSteps() {
		return initialSteps;
	}

	public void setInitialSteps(int initialSteps) {
		this.initialSteps = initialSteps;
	}
}
