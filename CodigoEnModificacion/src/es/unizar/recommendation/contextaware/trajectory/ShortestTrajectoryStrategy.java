package es.unizar.recommendation.contextaware.trajectory;

import java.util.List;
import java.util.Map;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;

public class ShortestTrajectoryStrategy extends AbstractTrajectoryStrategy {

	//private static final Logger log = LoggerFactory.getLogger(ShortestTrajectoryStrategy.class);

	public ShortestTrajectoryStrategy(UndirectedGraph<Long, DefaultWeightedEdge> graph, Map<Long, String> diccionaryItemLocation) {
		super(graph, diccionaryItemLocation);
	}

	/**
	 * The most efficient route passing by several nodes. It is Based in the
	 * algorithm KShortestPaths of the API JGraphT.
	 */
	@Override
	public List<Long> getOptimalTrajectory(List<Long> vertices, long initialVertex) {
		// Add vertex
		for (int v = 0; v < vertices.size(); v++) {
			subgraph.addVertex(vertices.get(v));
		}
		// Add weights
		addWeights(vertices);

		// A Hamiltonian path is a path that visits each vertex exactly once.
		List<Long> pathHamiltonianPath = HamiltonianPath.getApproximateOptimalForCompleteGraph(subgraph, initialVertex);

		//log.debug(pathHamiltonianPath.toString());
		return pathHamiltonianPath;
	}

	public void addWeights(List<Long> vertices) {
		// Shortest paths between peers of recommended items
		for (int i = 0; i < vertices.size(); i++) {
			long startVertex = vertices.get(i);
			for (int j = i + 1; j < vertices.size(); j++) {
				long endVertex = vertices.get(j);
				List<DefaultWeightedEdge> pathTemp = DijkstraShortestPath.findPathBetween(graph, startVertex, endVertex);

				String path = preprocessingPath(startVertex, pathTemp.toString());
				pathsBetweenVertices.put(startVertex + ", " + endVertex, path);
				pathsBetweenVertices.put(endVertex + ", " + startVertex, reversePath(path));

				double weight = 0;
				for (int k = 0; k < pathTemp.size(); k++) {
					DefaultWeightedEdge edge = pathTemp.get(k);
					weight += graph.getEdgeWeight(edge);
				}
				subgraph.setEdgeWeight(subgraph.addEdge(startVertex, endVertex), weight);
				//log.debug(startVertex + ", " + endVertex + "-> " + weight);
			}
		}
	}

	public String reversePath(String path) {
		String[] edges = path.split(", ");
		String[] edge = cleanEdge(edges[edges.length - 1]);
		String pathReversed = "(" + edge[1] + " : " + edge[0] + ")";
		for (int i = edges.length - 2; i >= 0; i--) {
			edge = cleanEdge(edges[i]);
			pathReversed += ", " + "(" + edge[1] + " : " + edge[0] + ")";
		}
		return pathReversed;
	}

	public static String preprocessingPath(long startVertex, String pathTemp) {
		String[] arrayPaths = pathTemp.split(", ");
		String path = "";
		// path += "(" + startVertex + " : " + startVertex + ")";
		String[] arrayEdge = null;
		String[] arrayNextEdge = null;
		// The first edge:
		String edge = arrayPaths[0];
		arrayEdge = cleanEdge(edge);
		try {
			edge = "(" + arrayEdge[0] + " : " + arrayEdge[1] + ")";
		}
		catch(Exception e) {
			e.getStackTrace();
			System.out.println("Start vertex -> " + startVertex + " pathTemp -> " + pathTemp);
		}
		if (Long.valueOf(arrayEdge[0]).longValue() != startVertex) {
			edge = "(" + arrayEdge[1] + " : " + arrayEdge[0] + ")";
		}
		path += edge;// + ", ";
		String first = edge;
		// The rest of the edges
		for (int i = 1; i < arrayPaths.length; i++) {
			String[] firstEdge = cleanEdge(first);
			String nextEdge = arrayPaths[i];
			arrayNextEdge = cleanEdge(nextEdge);
			nextEdge = "(" + arrayNextEdge[0] + " : " + arrayNextEdge[1] + ")";
			if (!firstEdge[1].equalsIgnoreCase(arrayNextEdge[0])) {
				nextEdge = "(" + arrayNextEdge[1] + " : " + arrayNextEdge[0] + ")";
			}
			path += ", " + nextEdge;// + ", ";
			first = nextEdge;
		}
		
		System.out.println("Path -> " + path);		
		return path;
	}

	public static String[] cleanEdge(String edge) {
		String cleanEdge = edge.replace("[", "");
		cleanEdge = cleanEdge.replace("]", "");
		cleanEdge = cleanEdge.replace("(", "");
		cleanEdge = cleanEdge.replace(")", "");
		String[] array = cleanEdge.split(" : ");
		return array;
	}
}
