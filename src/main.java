import java.util.ArrayList;
import java.util.Map;

import org.geotools.feature.SchemaException;
import org.geotools.graph.structure.Graph;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.MultiLineString;

import de.ifgi.algorithm.LandmarksIncluder;
import de.ifgi.algorithm.Router;
import de.ifgi.algorithm.Simplifier;
import de.ifgi.algorithm.Sweep;
import de.ifgi.configs.Config;
import de.ifgi.configs.ConfigLoader;
import de.ifgi.db.Database;
import de.ifgi.landmark.Landmark;
import de.ifgi.utils.GraphUtil;
import de.ifgi.utils.Utils;

public class main {

	// Coordinate(7.61, 51.96); // castle
	// Coordinate(7.62, 51.96); //cathedral
	/**
	 * 
	 * @param args lat lng type
	 * @throws OperationNotFoundException
	 * @throws TransformException
	 * @throws FactoryException
	 * @throws SchemaException
	 */
	public static void main(String[] args) throws OperationNotFoundException, TransformException, FactoryException, SchemaException {
		double destLat = args.length == 0 ? 51.961831 : Double.parseDouble(args[0]);
		double destLng = args.length == 0 ? 7.617630 : Double.parseDouble(args[1]);
		String transportationType = args.length == 0 ? "car" : args[2];
		Config config = new ConfigLoader().getConfig(transportationType);

		ArrayList<MultiLineString> roads;
		Map<String, ArrayList<MultiLineString>> roadLayers;
		Database db = new Database("server", config);
		Utils utils = new Utils();
		Sweep sweeper;
		Graph g;
		
		/*
		 * Algorithm modules 
		 */
		
		// query data and create graph
		roadLayers = db.queryData(destLat, destLng);
		// visibility sweep
		sweeper = new Sweep(destLat, destLng, config, roadLayers);
		roads = sweeper.runSweep();
		// shortest path
		g = new GraphUtil(roads).getGraph();
		roads = new Router(g, roads).shortestPath(destLat, destLng);
		// simplify
		roads = new Simplifier().simplify(roads);
		// landmarks
		ArrayList<Landmark> landmarks = new LandmarksIncluder().includeLandmarks(roads);
		// roads output
		utils.writeRoadsToShapefile(roads);
		// landmarks output
		utils.writeLandmarksToShapefile(landmarks);
	}
}
