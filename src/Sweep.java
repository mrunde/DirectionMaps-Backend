import java.util.ArrayList;
import java.util.HashSet;
import org.geotools.feature.SchemaException;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.JTSFactoryFinder;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

import de.ifgi.utils.Utils;

public class Sweep {

	// container for the features to display in final shapefile
	private ArrayList<MultiLineString> roads = new ArrayList<MultiLineString>();

	// the different road features as requested from the database
	private ArrayList<MultiLineString> motorways = new ArrayList<MultiLineString>();
	private ArrayList<MultiLineString> primary = new ArrayList<MultiLineString>();
	private ArrayList<MultiLineString> secondary = new ArrayList<MultiLineString>();
	private ArrayList<MultiLineString> tertiary = new ArrayList<MultiLineString>();
	private ArrayList<MultiLineString> residential = new ArrayList<MultiLineString>();
	private ArrayList<MultiLineString> service = new ArrayList<MultiLineString>();

	// used to create multilinesegments
	private GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
	private Utils util;
	private Database db;
	private Coordinate dest; // destination coordinate

	/**
	 * 
	 * @param destLat destination latitude 
	 * @param destLng destination longitude
	 * @param bbox bbox radius
	 * @param type transportation type
	 * @throws SchemaException
	 */
	public Sweep(double destLat, double destLng, double bbox, String type) throws SchemaException {
		this.dest = new Coordinate(destLng, destLat); 
		this.util = new Utils();
		this.db = new Database();

		queryData();

		runSweep();

		util.writeToShapefile(roads);
	}

	// query data from database
	private void queryData() {
		try {
			this.motorways = db.queryRoads("motorway");
			this.primary = db.queryRoads("primary");
			this.secondary = db.queryRoads("secondary");
			this.tertiary = db.queryRoads("tertiary");
			this.residential = db.queryRoads("residential");
			this.service = db.queryRoads("service");
		} catch (CQLException cql) {
			cql.printStackTrace();
		}
	}

	private void runSweep() {

		// different road layers from osm, hierarchy is important here
		ArrayList<ArrayList<MultiLineString>> roadLayers = new ArrayList<ArrayList<MultiLineString>>();
		roadLayers.add(motorways);
		roadLayers.add(primary);
		roadLayers.add(secondary);
		roadLayers.add(tertiary);
		roadLayers.add(residential);
		roadLayers.add(service);

		ArrayList<MultiLineString> newLines = new ArrayList<MultiLineString>();
		HashSet<MultiLineString> toRemove = new HashSet<MultiLineString>();
		roads.clear();

		// first, add all roads to the result
		for (int i = 0; i < roadLayers.size(); i++) {
			roads.addAll(roadLayers.get(i));
		}

		for (int i = 1; i < roadLayers.size(); i++) {
			newLines.clear();
			// calculate "sight lines" (lines from destination to each line in
			// collection)
			ArrayList<MultiLineString> layer = roadLayers.get(i);
			for (MultiLineString mls : layer) {
				// System.out.println(mls.toString());
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
					for (MultiLineString mlsM : roadLayers.get(x)) {
						// check intersection, ignore case where sight line
						// corresponds to a certain street segment
						if (newLines.get(n).intersects(mlsM) && !roadLayers.get(i).get(n).equals(mlsM)) {
							// roads.remove(roadLayers.get(i).get(n));
							toRemove.add(roadLayers.get(i).get(n));
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

	
	// Coordinate(7.5956, 51.9695); // ifgi
	// Coordinate(7.61, 51.96); // castle
	// Coordinate(7.62, 51.96); //cathedral
	public static void main(String[] args) {
		try {
			Sweep sweep = new Sweep(51.9695, 7.5956, 10.00, "car"); // ifgi
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
