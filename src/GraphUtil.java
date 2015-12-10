import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.graph.build.line.LineStringGraphGenerator;
import org.geotools.graph.path.DijkstraShortestPathFinder;
import org.geotools.graph.path.Path;
import org.geotools.graph.structure.Edge;
import org.geotools.graph.structure.Graph;
import org.geotools.graph.structure.Node;
import org.geotools.graph.traverse.standard.DijkstraIterator;
import org.geotools.graph.traverse.standard.DijkstraIterator.EdgeWeighter;
import org.opengis.coverage.processing.OperationNotFoundException;
import org.opengis.feature.Feature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

import de.ifgi.db.PGRDatabase;
import de.ifgi.utils.Utils;

public class GraphUtil {


	
	private ArrayList<LineString> topology;
	private ArrayList<MultiLineString> roads;

	private GeometryFactory geometryFactory;
	
	private Graph graph;
	
	public GraphUtil(ArrayList<MultiLineString> roads){
		this.roads = roads;
		this.geometryFactory = JTSFactoryFinder.getGeometryFactory();
		
		try {
			queryPGRData();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		this.graph = buildGraph();
	}
	
	// returns the osm id of a geometry
	private long findOsmId(Geometry geom){
		Feature f = (Feature)geom.getUserData();
		Object id = f.getProperty("osm_id").getValue();
		if(id instanceof Long){
			return (Long)id;
		}
		return Long.parseLong((String)id);
	}
	
	// uses the osm_id to check if a geometry of 
	// the pgr_roads table is also included in
	// the sweep result set
	private boolean containsGeometry(long osmId){
		for(MultiLineString mls : roads){
			if(findOsmId(mls) == osmId){
				return true;
			}
		}
		return false;
	}

	// create the graph from the queried roads
	public Graph buildGraph() {
		// create graph generator
		LineStringGraphGenerator graphGen = new LineStringGraphGenerator();
		
		// add geometries(linestring) to graph 
		for(int i = 0; i < topology.size(); i++){
			graphGen.add(topology.get(i));
		}
		Graph graph = graphGen.getGraph();
		
		System.out.println("Topo Graph: " + graph.getNodes().size() + "/" + graph.getEdges().size());

		return graph;
	}
	
	// finds the edge in the graph, that is closest to the destination coordinates
	//TODO: replace this method by Marius DB function
	private Edge findEdgeClosestTo(double destLat, double destLong, Collection<?> edges){
		Edge result = null;
		double minDistance = 100000;
		
		Point dest = geometryFactory.createPoint(new Coordinate(destLong, destLat));		
		Iterator<?> it = edges.iterator();
		
		while(it.hasNext()){
			Edge e = (Edge)it.next();
			LineString ls = (LineString)e.getObject();
			if(dest.distance(ls) < minDistance && containsGeometry(findOsmId(ls))){
				minDistance = dest.distance(ls);
//				System.out.println("Min Distance: " + minDistance);
				result = e;
			}
		}		
		return result;
	}

	// calculates the shortest path to all geometries of the roads list(result from sweep)
	public void shortestPath() {
		HashSet<MultiLineString> result = new HashSet<MultiLineString>();
		
		// source node (equal to dijkstra root)
//		Node start = findEdgeClosestTo(51.969976, 7.596633, graph.getEdges()).getNodeA(); //ifgi
		Node start = findEdgeClosestTo(51.956667, 7.635, graph.getEdges()).getNodeA();	//train station
//		Node start = findEdgeClosestTo(51.963503, 7.615644, graph.getEdges()).getNodeA();	//castle
		System.out.println("Contains Node: " + graph.getNodes().contains(start));

		// list of destinations to calculate shortest path to
		ArrayList<Edge> destinations = new ArrayList<Edge>();
		Iterator<?> it = graph.getEdges().iterator();
		// add all edges, that are also included in the 
		// sweep result set, as destination
		while(it.hasNext()){
			Edge e = (Edge)it.next();
			LineString ls = (LineString) e.getObject();
			if(containsGeometry(findOsmId(ls))){
				destinations.add(e);
			}
		}

		// use length of line segments as weights for graph
		EdgeWeighter weighter = new DijkstraIterator.EdgeWeighter() {
			public double getWeight(Edge e) {				
				return ((LineString)e.getObject()).getLength();
			}
		};

		// create and calculate shortest paths with dijkstra
		DijkstraShortestPathFinder pf = new DijkstraShortestPathFinder(graph,
				start, weighter);
		pf.calculate();

		// calculate the paths
		for (Iterator<?> d = destinations.iterator(); d.hasNext();) {
			Edge destination = (Edge) d.next();
			Path path = pf.getPath(destination.getNodeA());
			try{
				for(int i = 0; i < path.getEdges().size(); i++){
					Edge e = (Edge)path.getEdges().get(i);
					
					// add shortests paths to the result set
					LineString ls = (LineString)e.getObject();
					GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
					MultiLineString mls = geometryFactory.createMultiLineString(new LineString[]{ls});
					mls.setUserData(ls.getUserData());
					
					result.add(mls);
				}
			} catch(NullPointerException ex){
				ex.getMessage();
			}			
		}
		
		// write result to disk
		try{
			Utils utils = new Utils();
			// also add sweep result to the list
			result.addAll(roads);
			ArrayList<MultiLineString> r = new ArrayList<MultiLineString>();
			r.addAll(result);
			utils.writeToShapefile(r);
		} catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	// queries the data from the "roads_pgr" table
	private void queryPGRData() throws OperationNotFoundException, 
			TransformException, FactoryException{
		PGRDatabase tdb = new PGRDatabase();
		try{
			this.topology = tdb.queryRoads(51.9695, 7.5956, 15000);
		} catch(CQLException ceq){
			ceq.printStackTrace();
		}
		
	}

}
