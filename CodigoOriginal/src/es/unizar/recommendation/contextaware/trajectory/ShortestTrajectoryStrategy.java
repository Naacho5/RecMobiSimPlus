package es.unizar.recommendation.contextaware.trajectory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

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
		if (vertices == null || vertices.isEmpty()) {
			return new LinkedList<>();
		}

		SimpleWeightedGraph<Long, DefaultWeightedEdge> localSubgraph =
        	new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
    	Map<String, String> localPaths = new TreeMap<>();

		// Clean subgraph
		// subgraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);  // reset
    	// pathsBetweenVertices.clear();

		// Add vertex
		for (int v = 0; v < vertices.size(); v++) {
			// subgraph.addVertex(vertices.get(v));
			localSubgraph.addVertex(vertices.get(v));
		}
		// Add weights
		// addWeights(vertices);
		addWeights(vertices, localSubgraph, localPaths);
		
		// A Hamiltonian path is a path that visits each vertex exactly once.
		// List<Long> pathHamiltonianPath = HamiltonianPath.getApproximateOptimalForCompleteGraph(subgraph, initialVertex);
		List<Long> pathHamiltonianPath = HamiltonianPath.getApproximateOptimalForCompleteGraph(localSubgraph, initialVertex);
		System.out.println("ShortestTrajectoryStrategy: pathHamiltonianPath=" + pathHamiltonianPath); // Debugging line
		//log.debug(pathHamiltonianPath.toString());
		return pathHamiltonianPath;
	}

	/**
	 * Add weights to the subgraph based on the shortest paths in the original graph.
	 * @param vertices the list of vertices to consider for the subgraph
	 * @param localSubgraph the subgraph to which weights will be added
	 * @param localPaths a map to store the paths between vertices for debugging purposes
	 */
	public void addWeights(List<Long> vertices,
						SimpleWeightedGraph<Long, DefaultWeightedEdge> localSubgraph,
						Map<String, String> localPaths) {
		for (int i = 0; i < vertices.size(); i++) {
			long startVertex = vertices.get(i);
			for (int j = i + 1; j < vertices.size(); j++) {
				long endVertex = vertices.get(j);
				List<DefaultWeightedEdge> pathTemp = DijkstraShortestPath.findPathBetween(graph, startVertex, endVertex);

				if (pathTemp == null || pathTemp.isEmpty()) {
					continue;
				}

				String path = preprocessingPath(startVertex, pathTemp.toString());
				localPaths.put(startVertex + ", " + endVertex, path);
				localPaths.put(endVertex + ", " + startVertex, reversePath(path));

				double weight = 0;
				for (int k = 0; k < pathTemp.size(); k++) {
					weight += graph.getEdgeWeight((DefaultWeightedEdge) pathTemp.get(k));
				}

				DefaultWeightedEdge e = localSubgraph.getEdge(startVertex, endVertex);
				if (e == null) {
					e = localSubgraph.addEdge(startVertex, endVertex);
				}
				if (e != null) {
					localSubgraph.setEdgeWeight(e, weight);
				}
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
		String[] arrayEdge = null;
		String[] arrayNextEdge = null;
		// The first edge:
		String edge = arrayPaths[0];
		arrayEdge = cleanEdge(edge);
		edge = "(" + arrayEdge[0] + " : " + arrayEdge[1] + ")";
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
