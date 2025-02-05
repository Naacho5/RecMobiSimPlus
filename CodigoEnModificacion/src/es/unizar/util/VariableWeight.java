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
 * CAnMORE_LICENSE file included in this distribution; you may not use this
 * file except in compliance with the License.
 *
 * Contributor(s):
 *  RODRIGUEZ-HERNANDEZ, MARIA DEL CARMEN <692383[3]unizar.es>
 *  ILARRI, SERGIO <silarri[3]unizar.es>
 */
package es.unizar.util;

/**
 *
 * @author Maria del Carmen Rodriguez-Hernandez
 */
public class VariableWeight {

    /**
     * Variable name.
     */
    public String variableName;
    /**
     * Weight.
     */
    public double weight;

    /**
     * Constructor.
     *
     * @param variableName Variable name.
     * @param weight Weight.
     */
    public VariableWeight(String variableName, double weight) {
        this.variableName = variableName;
        this.weight = weight;
    }
}
