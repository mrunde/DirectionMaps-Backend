package de.ifgi.utils;

import java.util.ArrayList;

import org.geotools.filter.text.cql2.CQLException;
import org.geotools.graph.build.line.LineStringGraphGenerator;
import org.geotools.graph.structure.Graph;
import org.opengis.coverage.processing.OperationNotFoundException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

import de.ifgi.db.PGRDatabase;

public class GraphUtil {

	private ArrayList<LineString> topology;
	private ArrayList<MultiLineString> roads;
	private Graph graph;

	public GraphUtil(ArrayList<MultiLineString> roads) {
		this.roads = roads;

		try {
			queryPGRData();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		this.graph = buildGraph();
	}

	public Graph getGraph() {
		return this.graph;
	}

	// create the graph from the queried roads
	public Graph buildGraph() {
		// create graph generator
		LineStringGraphGenerator graphGen = new LineStringGraphGenerator();

		// add geometries(linestring) to graph
		for (int i = 0; i < topology.size(); i++) {
			graphGen.add(topology.get(i));
		}
		Graph graph = graphGen.getGraph();

		System.out.println("Topo Graph: " + graph.getNodes().size() + "/" + graph.getEdges().size());

		return graph;
	}

	// queries the data from the "roads_pgr" table
	private void queryPGRData() throws OperationNotFoundException, TransformException, FactoryException {
		PGRDatabase tdb = new PGRDatabase();
		try {
			this.topology = tdb.queryRoads(51.9695, 7.5956, 15000);
		} catch (CQLException ceq) {
			ceq.printStackTrace();
		}

	}

}
