import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.geotools.feature.SchemaException;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

import de.ifgi.configs.CarConfig;
import de.ifgi.configs.Config;
import de.ifgi.db.Database;
import de.ifgi.utils.Utils;

public class Sweep {

	// container for the features to display in final shapefile
	private ArrayList<MultiLineString> roads = new ArrayList<MultiLineString>();
	// hashmap of all road classes
	private Map<String, ArrayList<MultiLineString>> roadLayers = new HashMap<String, ArrayList<MultiLineString>>();
	// used to create multilinesegments
	private GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
	private Utils util;
	private Database db;
	private Config config;
	private double destLat, destLng;
	private Coordinate dest;
	private String[] roadClasses; // holds names of all the road classes for the current transportation mode

	/**
	 * 
	 * @param destLat
	 *            destination latitude
	 * @param destLng
	 *            destination longitude
	 * @param type
	 *            transportation type
	 * @throws SchemaException
	 * @throws FactoryException
	 * @throws TransformException
	 * @throws OperationNotFoundException
	 */
	public Sweep(double destLat, double destLng, String type)
			throws SchemaException, OperationNotFoundException, TransformException, FactoryException {

		this.destLat = destLat;
		this.destLng = destLng;
		this.dest = new Coordinate(destLng, destLat);
		
		// get appropriate config
		switch (type) {
		case "car":
			this.config = new CarConfig();
			break;

		default:
			break;
		}

		this.util = new Utils();
		this.db = new Database("server");

		queryData();

		runSweep();

		util.writeToShapefile(roads);
	}

	// query data from database
	private void queryData() throws OperationNotFoundException, TransformException, FactoryException {
		roadClasses = config.roadClasses;
		for (int i = 0; i < roadClasses.length; i++) {
			// bbox diameters for each road class
			double diameter = config.bboxDiameter[i];
			try {
				roadLayers.put(roadClasses[i], db.queryRoads(destLat, destLng, diameter, roadClasses[i]));
			} catch (CQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void runSweep() {

		ArrayList<MultiLineString> newLines = new ArrayList<MultiLineString>();
		HashSet<MultiLineString> toRemove = new HashSet<MultiLineString>();
		roads.clear();

		// first, add all roads to the result
		this.roadLayers.forEach((k, layer) -> {
			roads.addAll(layer);
		});

		for (int i = 1; i < roadClasses.length; i++) {
			newLines.clear();
			// calculate "sight lines" (lines from destination to each line in collection
			ArrayList<MultiLineString> layer = roadLayers.get(roadClasses[i]);
			for (MultiLineString mls : layer) {
				Coordinate mlsCenter = mls.getCoordinates()[(int) (mls.getCoordinates().length / 2)];
				Coordinate[] coArr = { dest, mlsCenter };
				LineString ls = geometryFactory.createLineString(coArr);

				newLines.add(geometryFactory.createMultiLineString(new LineString[] { ls }));
			}

			System.out.println("Calculated Sightlines for Layer " + i + "! Size: " + newLines.size());

			// for each sight line...
			for (int n = 0; n < newLines.size(); n++) {
				// ...iterate through layers of higher hierarchy...
				hierarchyLoop: for (int x = 0; x <= i; x++) {
					// ...and check each line segment for intersections
					// also against its own hierarchy type
					for (MultiLineString mlsM : roadLayers.get(roadClasses[x])) {
						// check intersection, ignore case where sight line
						// corresponds to a certain street segment
						if (newLines.get(n).intersects(mlsM) && !roadLayers.get(roadClasses[i]).get(n).equals(mlsM)) {
							// roads.remove(roadLayers.get(i).get(n));
							toRemove.add(roadLayers.get(roadClasses[i]).get(n));
							// break for performance
							// once roadLayers.get(i).get(n) gets removed
							// there's no need to continue
							break hierarchyLoop;
						}
					}
				}
			}
		}

		System.out.println("Segments removed: " + toRemove.size());
		roads.removeAll(toRemove);

		System.out.println("Roads size: " + roads.size());
	}


	// Coordinate(7.61, 51.96); // castle
	// Coordinate(7.62, 51.96); //cathedral
	public static void main(String[] args) throws OperationNotFoundException, TransformException, FactoryException {
		try {
			Sweep sweep = new Sweep(51.9695, 7.5956, "car"); // ifgi
			//Sweep sweep = new Sweep(51.956667, 7.635, "car"); // train station
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
