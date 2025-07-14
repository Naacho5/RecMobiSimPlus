package es.unizar.recommendation.contextaware.trajectory;

import java.util.LinkedList;
import java.util.List;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

public class HamiltonianPath {
	public static <V, E> List<Long> getApproximateOptimalForCompleteGraph(SimpleWeightedGraph<Long, DefaultWeightedEdge> g, long initialVertex) {
		/* Añadido por Nacho Palacio 2025-04-14. */
		if (g == null || g.vertexSet().isEmpty()) {
			System.out.println("Warning: Empty graph received, unable to calculate Hamiltonian path");
			return new LinkedList<>();
		}

		List<Long> vertices = new LinkedList<Long>(g.vertexSet());

		if (vertices.size() * (vertices.size() - 1) / 2 != g.edgeSet().size()) {
			return null;
		}

		List<Long> tour = new LinkedList<>();
		//Fix the initial vertex
		tour.add(initialVertex);
		vertices.remove(initialVertex);

		while (tour.size() != g.vertexSet().size()) {
			boolean firstEdge = true;
			double minEdgeValue = 0.0D;
			int minVertexFound = 0;

			for (int i = 0; i < tour.size(); ++i) {
				Long v = tour.get(i);
				for (int j = 0; j < vertices.size(); ++j) {
					double weight = g.getEdgeWeight(g.getEdge(v, vertices.get(j)));

					if ((firstEdge) || (weight < minEdgeValue)) {
						firstEdge = false;
						minEdgeValue = weight;
						minVertexFound = j;
					}
				}
			}
			tour.add(vertices.get(minVertexFound)); 
			vertices.remove(minVertexFound);
		}
		return tour;
	}

}
