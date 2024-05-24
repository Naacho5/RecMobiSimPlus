/*
 * @(#)Context.java  1.0.0  25/10/16
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
package es.unizar.recommendation.contextaware.trajectory;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

/**
 * Operations with graph.
 * 
 * @author Maria del Carmen Rodriguez-Hernandez
 */
public abstract class AbstractTrajectoryStrategy {

	public UndirectedGraph<Long, DefaultWeightedEdge> graph;
	public SimpleWeightedGraph<Long, DefaultWeightedEdge> subgraph;
	public Map<Long, String> diccionaryItemLocation;
	public Map<String, String> pathsBetweenVertices;

	public AbstractTrajectoryStrategy(UndirectedGraph<Long, DefaultWeightedEdge> graph, Map<Long, String> diccionaryItemLocation) {
		this.graph = graph;
		this.subgraph = new SimpleWeightedGraph<Long, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		this.diccionaryItemLocation = diccionaryItemLocation;
		this.pathsBetweenVertices = new TreeMap<>();
	}

	public abstract List<Long> getOptimalTrajectory(List<Long> vertices, long initialVertex);
}
