import java.util.ArrayList;
import java.util.Map;

import org.geotools.feature.SchemaException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiLineString;

import de.ifgi.algorithm.Sweep;
import de.ifgi.configs.CarConfig;
import de.ifgi.configs.Config;
import de.ifgi.configs.ConfigLoader;
import de.ifgi.db.Database;
import de.ifgi.utils.GraphUtil;
import de.ifgi.utils.Utils;

public class main {

	// Coordinate(7.61, 51.96); // castle
	// Coordinate(7.62, 51.96); //cathedral
	public static void main(String[] args) throws OperationNotFoundException, TransformException, FactoryException, SchemaException {
		double destLat = 51.9695;
		double destLng = 7.5956;
		Config config = new ConfigLoader().getConfig("car");
		ArrayList<MultiLineString> roads;
		Map<String, ArrayList<MultiLineString>> roadLayers;
		Database db = new Database("local", config);
		Utils util = new Utils();
		Sweep sweeper;

		roadLayers = db.queryData(destLat, destLng);
		sweeper = new Sweep(destLat, destLng, config, roadLayers);
		roads = sweeper.runSweep();
		
		GraphUtil gu = new GraphUtil(roads);
		gu.shortestPath();

	}

}
