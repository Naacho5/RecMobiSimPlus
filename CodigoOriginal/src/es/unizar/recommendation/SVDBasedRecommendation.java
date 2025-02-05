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
package es.unizar.recommendation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import org.apache.mahout.cf.taste.common.NoSuchItemException;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.impl.common.FullRunningAverage;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.common.RefreshHelper;
import org.apache.mahout.cf.taste.impl.common.RunningAverage;
import org.apache.mahout.cf.taste.impl.recommender.AbstractRecommender;
import org.apache.mahout.cf.taste.impl.recommender.TopItems;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.recommender.IDRescorer;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.common.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.unizar.gui.Configuration;

/**
 * Produces user-based collaborative filtering recommendations and preference
 * estimates.
 *
 * @author Maria del Carmen Rodriguez-Hernandez
 */
public final class SVDBasedRecommendation extends AbstractRecommender {

	@SuppressWarnings("all")
	private static final Logger log = LoggerFactory.getLogger(SVDBasedRecommendation.class);
	private static final Random random = RandomUtils.getRandom(Configuration.simulation.getSeed());
	private final RefreshHelper refreshHelper;
	/**
	 * Number of features
	 */
	private final int numFeatures;
	private final FastByIDMap<Integer> userMap;
	private final FastByIDMap<Integer> itemMap;
	private final ExpectationMaximizationSVD emSvd;
	private final List<Preference> cachedPreferences;

	/**
	 * @param numFeatures
	 *            the number of features
	 * @param initialSteps
	 *            number of initial training steps
	 */
	public SVDBasedRecommendation(DataModel dataModel, int numFeatures, int initialSteps) throws TasteException {
		super(dataModel);
		this.numFeatures = numFeatures;
		int numUsers = dataModel.getNumUsers();
		userMap = new FastByIDMap<Integer>(numUsers);
		int idx = 0;
		LongPrimitiveIterator userIterator = dataModel.getUserIDs();
		while (userIterator.hasNext()) {
			userMap.put(userIterator.nextLong(), idx++);
		}
		int numItems = dataModel.getNumItems(); // RETURNS 0 ITEMS
		itemMap = new FastByIDMap<Integer>(numItems);
		idx = 0;
		LongPrimitiveIterator itemIterator = dataModel.getItemIDs();
		while (itemIterator.hasNext()) {
			itemMap.put(itemIterator.nextLong(), idx++);
		}
		double average = getAveragePreference(); // THROWS EXCEPTION
		double defaultValue = Math.sqrt((average - 1.0) / (double) numFeatures);
		emSvd = new ExpectationMaximizationSVD(numUsers, numItems, numFeatures, defaultValue);
		cachedPreferences = new ArrayList<Preference>(numUsers);
		recachePreferences();
		refreshHelper = new RefreshHelper(new Callable<Object>() {
			@Override
			public Object call() throws TasteException {
				recachePreferences();
				// TODO: train again
				return null;
			}
		});
		refreshHelper.addDependency(dataModel);
		train(initialSteps);
	}

	private void recachePreferences() throws TasteException {
		cachedPreferences.clear();
		DataModel dataModel = getDataModel();
		LongPrimitiveIterator it = dataModel.getUserIDs();
		while (it.hasNext()) {
			for (Preference pref : dataModel.getPreferencesFromUser(it.nextLong())) {
				cachedPreferences.add(pref);
			}
		}
	}

	private double getAveragePreference() throws TasteException {
		RunningAverage average = new FullRunningAverage();
		DataModel dataModel = getDataModel();
		LongPrimitiveIterator it = dataModel.getUserIDs();
		while (it.hasNext()) {
			for (Preference pref : dataModel.getPreferencesFromUser(it.nextLong())) {
				average.addDatum(pref.getValue());
			}
		}
		return average.getAverage();
	}

	public void train(int steps) {
		for (int i = 0; i < steps; i++) {
			nextTrainStep();
		}
	}

	private void nextTrainStep() {
		Collections.shuffle(cachedPreferences, random);
		for (int i = 0; i < numFeatures; i++) {
			for (Preference pref : cachedPreferences) {
				Integer useridx = userMap.get(pref.getUserID());
				Integer itemidx = itemMap.get(pref.getItemID());
				if (useridx != null && itemidx != null) {
					emSvd.train(useridx, itemidx, i, pref.getValue());
				}				
			}
		}
	}

	private float predictRating(int user, int item) {
		return (float) emSvd.getDotProduct(user, item);
	}

	@Override
	public float estimatePreference(long userID, long itemID) throws TasteException {
		Integer useridx = userMap.get(userID);
		if (useridx == null) {
			throw new NoSuchUserException();
		}
		Integer itemidx = itemMap.get(itemID);
		if (itemidx == null) {
			throw new NoSuchItemException();
		}
		return predictRating(useridx, itemidx);
	}

	// @Override
	public List<RecommendedItem> recommend(long userID, int howMany, IDRescorer rescorer) throws TasteException {
		if (howMany < 1) {
			throw new IllegalArgumentException("howMany must be at least 1");
		}
		log.debug("Recommending items for user ID '{}'", userID);
		PreferenceArray preferencesFromUser = getDataModel().getPreferencesFromUser(userID);
		FastIDSet possibleItemIDs = getAllOtherItems(userID, preferencesFromUser);
		TopItems.Estimator<Long> estimator = new Estimator(userID);
		List<RecommendedItem> topItems = TopItems.getTopItems(howMany, possibleItemIDs.iterator(), rescorer, estimator);
		log.debug("Recommendations are: {}", topItems);
		return topItems;
	}

	@Override
	public void refresh(Collection<Refreshable> alreadyRefreshed) {
		refreshHelper.refresh(alreadyRefreshed);
	}

	@Override
	public String toString() {
		return "SVDRecommender[numFeatures:" + numFeatures + ']';
	}

	private final class Estimator implements TopItems.Estimator<Long> {

		private final long theUserID;

		private Estimator(long theUserID) {
			this.theUserID = theUserID;
		}

		@Override
		public double estimate(Long itemID) throws TasteException {
			return estimatePreference(theUserID, itemID);
		}
	}

	public ExpectationMaximizationSVD getEmSvd() {
		return emSvd;
	}
}
