package de.ifgi.utils;

import java.util.ArrayList;

import org.geotools.filter.text.cql2.CQLException;
import org.geotools.graph.build.line.LineStringGraphGenerator;
import org.geotools.graph.structure.Graph;
import org.opengis.coverage.processing.OperationNotFoundException;
import org.opengis.feature.Feature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

import de.ifgi.configs.ConfigLoader;
import de.ifgi.db.Database;

public class GraphUtil {

	private ArrayList<LineString> topology;
	private Graph graph;

	public GraphUtil(ArrayList<MultiLineString> roads, Database db, double destLat, double destLng) {
		try {
			queryPGRData(db, destLat, destLng);
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
	private void queryPGRData(Database db, double destLat, double destLng) throws OperationNotFoundException, TransformException, FactoryException {
		//Database db = new Database("server", new ConfigLoader().getConfig("car"));
		try {
			this.topology = db.queryRoads(destLat, destLng);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

}
