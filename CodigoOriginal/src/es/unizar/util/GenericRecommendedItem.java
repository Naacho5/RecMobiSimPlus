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
package es.unizar.util;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

/**
 * Was created to sort a list of recommended items.
 *
 * @author Maria del Carmen Rodriguez-Hernandez
 */
public final class GenericRecommendedItem implements Comparable<RecommendedItem>, RecommendedItem, Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final long itemID;
    private final float value;

    /**
     * @param itemID
     * @param value
     * @throws IllegalArgumentException if item is null or value is NaN
     */
    public GenericRecommendedItem(long itemID, float value) {
        Preconditions.checkArgument(!Float.isNaN(value), "value is NaN");
        this.itemID = itemID;
        this.value = value;
    }

    /**
     * Gets the item recommended.
     *
     * @return The item recommended.
     */
    @Override
    public long getItemID() {
        return itemID;
    }

    /**
     * Gets the value of the item recommended.
     *
     * @return The value of the item recommended.
     */
    @Override
    public float getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "RecommendedItem[item:" + itemID + ", value:" + value + ']';
    }

    @Override
    public int compareTo(RecommendedItem item) {
        return Float.compare(item.getValue(), this.getValue());
    }
}