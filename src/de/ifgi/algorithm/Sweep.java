package de.ifgi.algorithm;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

import de.ifgi.configs.Config;


public class Sweep {

	// hashmap of all road classes
	private Map<String, ArrayList<MultiLineString>> roadLayers;
	// used to create multilinesegments
	private GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
	private Coordinate dest;
	private String[] roadClasses; // holds names of all the road classes for the current transportation mode

	/**
	 * 
	 * @param destLat
	 *            destination latitude
	 * @param destLng
	 *            destination longitude
	 * @param config
	 *            transportation type config
	 * @param roadLayers
	 *           
	 * @throws SchemaException
	 * @throws FactoryException
	 * @throws TransformException
	 * @throws OperationNotFoundException
	 */
	public Sweep(double destLat, double destLng, Config config, Map<String, ArrayList<MultiLineString>> roadLayers)
			throws SchemaException, OperationNotFoundException, TransformException, FactoryException {
		
		this.roadClasses = config.roadClasses;
		this.dest = new Coordinate(destLng, destLat);
		this.roadLayers = roadLayers;
		
	}



	public ArrayList<MultiLineString> runSweep() {
		// container for the features to display in final shapefile
		ArrayList<MultiLineString> roads = new ArrayList<MultiLineString>();
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
		return roads;
	}

}
