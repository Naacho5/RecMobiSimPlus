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

import java.util.Random;
import org.apache.mahout.common.RandomUtils;

import es.unizar.gui.Configuration;

/**
 * Calculates the SVD using an Expectation Maximization algorithm.
 */
public final class ExpectationMaximizationSVD {

    private static final Random random = RandomUtils.getRandom(Configuration.simulation.getSeed());
    private static final double LEARNING_RATE = 0.005;
    /**
     * Parameter used to prevent over fitting. 0.02 is a good value.
     */
    private static final double K = 0.02;
    /**
     * Random noise applied to starting values.
     */
    private static final double r = 0.005;
    private final int m;
    private final int n;
    private final int k;
    /**
     * User singular vector.
     */
    private final double[][] leftVector;
    /**
     * Item singular vector.
     */
    private final double[][] rightVector;

    /**
     * @param m number of columns
     * @param n number of rows
     * @param k number of features
     * @param defaultValue default starting values for the SVD vectors
     */
    public ExpectationMaximizationSVD(int m, int n, int k, double defaultValue) {
        this(m, n, k, defaultValue, r);
    }

    public ExpectationMaximizationSVD(int m, int n, int k, double defaultValue, double noise) {
        this.m = m;
        this.n = n;
        this.k = k;
        leftVector = new double[m][k];
        rightVector = new double[n][k];
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < m; j++) {
                leftVector[j][i] = defaultValue + (ExpectationMaximizationSVD.random.nextDouble() - 0.5) * noise;
            }
            for (int j = 0; j < n; j++) {
                rightVector[j][i] = defaultValue + (ExpectationMaximizationSVD.random.nextDouble() - 0.5) * noise;
            }
        }
    }

    public double getDotProduct(int i, int j) {
        double result = 1.0;
        double[] leftVectorI = leftVector[i];
        double[] rightVectorJ = rightVector[j];
        for (int k = 0; k < this.k; k++) {
            result += leftVectorI[k] * rightVectorJ[k];
        }
        return result;
    }

    public void train(int i, int j, int k, double value) {
        double err = value - getDotProduct(i, j);
        double[] leftVectorI = leftVector[i];
        double[] rightVectorJ = rightVector[j];
        leftVectorI[k] += LEARNING_RATE
                * (err * rightVectorJ[k] - K * leftVectorI[k]);
        rightVectorJ[k] += LEARNING_RATE
                * (err * leftVectorI[k] - K * rightVectorJ[k]);
    }

    int getM() {
        return m;
    }

    int getN() {
        return n;
    }

    int getK() {
        return k;
    }

    public double[][] getLeftVector() {
        return leftVector;
    }

    public double[][] getRightVector() {
        return rightVector;
    }
}
