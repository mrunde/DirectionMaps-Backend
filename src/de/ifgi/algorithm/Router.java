package de.ifgi.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.DijkstraIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator.EdgeWeighter;
import org.opengis.feature.Feature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

public class Router {

	private Graph graph;
	private GeometryFactory geometryFactory;
	private ArrayList<MultiLineString> roads;

	public Router(Graph graph, ArrayList<MultiLineString> roads) {
		this.graph = graph;
		this.roads = roads;
		this.geometryFactory = JTSFactoryFinder.getGeometryFactory();
	}

	// calculates the shortest path to all geometries of the roads list(result
	// from sweep)
	public ArrayList<MultiLineString> shortestPath(double destLat, double destLng) {
		HashSet<MultiLineString> result = new HashSet<MultiLineString>();
		ArrayList<MultiLineString> r = new ArrayList<MultiLineString>();
		// source node (equal to dijkstra root)
		Node start = findEdgeClosestTo(destLat, destLng, graph.getEdges()).getNodeA(); // ifgi
		System.out.println("Contains Node: " + graph.getNodes().contains(start));

		// list of destinations to calculate shortest path to
		ArrayList<Edge> destinations = new ArrayList<Edge>();
		Iterator<?> it = graph.getEdges().iterator();
		// add all edges, that are also included in the
		// sweep result set, as destination
		while (it.hasNext()) {
			Edge e = (Edge) it.next();
			LineString ls = (LineString) e.getObject();
			if (containsGeometry(findOsmId(ls))) {
				destinations.add(e);
			}
		}

		// use length of line segments as weights for graph
		EdgeWeighter weighter = new DijkstraIterator.EdgeWeighter() {
			public double getWeight(Edge e) {
				return ((LineString) e.getObject()).getLength();
			}
		};

		// create and calculate shortest paths with dijkstra
		DijkstraShortestPathFinder pf = new DijkstraShortestPathFinder(graph, start, weighter);
		pf.calculate();

		// calculate the paths
		for (Iterator<?> d = destinations.iterator(); d.hasNext();) {
			Edge destination = (Edge) d.next();
			Path path = pf.getPath(destination.getNodeA());
			try {
				for (int i = 0; i < path.getEdges().size(); i++) {
					Edge e = (Edge) path.getEdges().get(i);

					// add shortests paths to the result set
					LineString ls = (LineString) e.getObject();
					GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
					MultiLineString mls = geometryFactory.createMultiLineString(new LineString[] { ls });
					mls.setUserData(ls.getUserData());

					result.add(mls);
				}
			} catch (NullPointerException ex) {
				ex.getMessage();
			}
		}

		// write result to disk
		try {
			// also add sweep result to the list
			result.addAll(roads);
			r.addAll(result);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return r;
	}

	// finds the edge in the graph, that is closest to the destination
	// coordinates
	// TODO: replace this method by Marius DB function
	private Edge findEdgeClosestTo(double destLat, double destLong, Collection<?> edges) {
		Edge result = null;
		double minDistance = 100000;

		Point dest = geometryFactory.createPoint(new Coordinate(destLong, destLat));
		Iterator<?> it = edges.iterator();

		while (it.hasNext()) {
			Edge e = (Edge) it.next();
			LineString ls = (LineString) e.getObject();
			if (dest.distance(ls) < minDistance && containsGeometry(findOsmId(ls))) {
				minDistance = dest.distance(ls);
				// System.out.println("Min Distance: " + minDistance);
				result = e;
			}
		}
		return result;
	}

	// returns the osm id of a geometry
	private long findOsmId(Geometry geom) {
		Feature f = (Feature) geom.getUserData();
		Object id = f.getProperty("osm_id").getValue();
		if (id instanceof Long) {
			return (Long) id;
		}
		return Long.parseLong((String) id);
	}

	// uses the osm_id to check if a geometry of
	// the pgr_roads table is also included in
	// the sweep result set
	private boolean containsGeometry(long osmId) {
		for (MultiLineString mls : roads) {
			if (findOsmId(mls) == osmId) {
				return true;
			}
		}
		return false;
	}

}
