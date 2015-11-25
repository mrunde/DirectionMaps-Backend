package tryouts;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.swing.UIManager;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class Sweep {

	// container for the features to display in final shapefile
	ArrayList<MultiLineString> roads = new ArrayList<MultiLineString>();
	
	// the different road features as requested from the database
	private ArrayList<MultiLineString> motorways = new ArrayList<MultiLineString>();
	private ArrayList<MultiLineString> primary = new ArrayList<MultiLineString>();
	private ArrayList<MultiLineString> secondary = new ArrayList<MultiLineString>();
	private ArrayList<MultiLineString> tertiary = new ArrayList<MultiLineString>();
	private ArrayList<MultiLineString> residential = new ArrayList<MultiLineString>();
	private ArrayList<MultiLineString> service = new ArrayList<MultiLineString>();

	public Coordinate source = new Coordinate(7.5956, 51.9695);	//ifgi
//	public Coordinate source = new Coordinate(7.61, 51.96);	//castle
//	public Coordinate source = new Coordinate(7.62, 51.96);	//cathedral

	// used to create multilinesegments
	private GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

	private SimpleFeatureBuilder featureBuilder;
	// information for shapefile creation
	private final SimpleFeatureType TYPE = DataUtilities.createType("Location",
			"the_geom:MultiLineString:srid=4326," + "number:Integer");

	
	public Sweep() throws SchemaException {
		this.featureBuilder = new SimpleFeatureBuilder(TYPE);

		queryData();

		runSweep();
		
		writeToShapefile();	
	}

	// creates a shapefile (method is mostly copied from geotools tutorials)
	private void createShapefile(ArrayList<SimpleFeature> features){
		try{
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			
	        File newFile = new File("output.shp");

	        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

	        Map<String, Serializable> params = new HashMap<String, Serializable>();
	        params.put("url", newFile.toURI().toURL());
	        params.put("create spatial index", Boolean.TRUE);

	        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

	        /*
	         * TYPE is used as a template to describe the file contents
	         */
	        newDataStore.createSchema(TYPE);
	        
	        /*
	         * Write the features to the shapefile
	         */
	        Transaction transaction = new DefaultTransaction("create");

	        String typeName = newDataStore.getTypeNames()[0];
	        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
	        SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
	        /*
	         * The Shapefile format has a couple limitations:
	         * - "the_geom" is always first, and used for the geometry attribute name
	         * - "the_geom" must be of type Point, MultiPoint, MuiltiLineString, MultiPolygon
	         * - Attribute names are limited in length 
	         * - Not all data types are supported (example Timestamp represented as Date)
	         * 
	         * Each data store has different limitations so check the resulting SimpleFeatureType.
	         */
	        System.out.println("SHAPE:"+SHAPE_TYPE);

	        if (featureSource instanceof SimpleFeatureStore) {
	            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
	            /*
	             * SimpleFeatureStore has a method to add features from a
	             * SimpleFeatureCollection object, so we use the ListFeatureCollection
	             * class to wrap our list of features.
	             */
	            SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
	            featureStore.setTransaction(transaction);
	            try {
	                featureStore.addFeatures(collection);
	                transaction.commit();
	            } catch (Exception problem) {
	                problem.printStackTrace();
	                transaction.rollback();
	            } finally {
	                transaction.close();
	            }
	            System.exit(0); // success!
	        } else {
	            System.out.println(typeName + " does not support read/write access");
	            System.exit(1);
	        }
		} catch(Exception ex){
			ex.printStackTrace();
		}
		System.out.println("Done writing!");
	}

	
	public void writeToShapefile(){
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
		for(int i = 0; i < roads.size(); i++){
			featureBuilder.add(roads.get(i));
			featureBuilder.add(i);
			SimpleFeature feature = featureBuilder.buildFeature(null);
			features.add(feature);
		}
		
		createShapefile(features);
	}
	
	
	private void runSweep(){		
		
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
		for(int i = 0; i < roadLayers.size(); i++){
			roads.addAll(roadLayers.get(i));
		}

		for(int i = 1; i < roadLayers.size(); i++){
			newLines.clear();
			// calculate "sight lines" (lines from destination to each line in collection)
			for(MultiLineString mls : roadLayers.get(i)){
				Coordinate mlsCenter = mls.getCoordinates()[(int)(mls.getCoordinates().length/2)];
				Coordinate[] coArr = { source, mlsCenter };
				LineString ls = geometryFactory.createLineString(coArr);
				
				newLines.add(geometryFactory.createMultiLineString(new LineString[] { ls }));
			}			
			
			System.out.println("Calculated Sightlines for Layer " + i + "! Size: " + newLines.size());
			
			
			// for each sight line...
			for(int n = 0; n < newLines.size(); n++){
				// ...iterate through layers of higher hierarchy...
				for(int x = i; x > 0; x--){	
					// ...and check each line segment for intersections
					for(MultiLineString mlsM : roadLayers.get(x-1)){
						if(newLines.get(n).intersects(mlsM)){
							toRemove.add(roadLayers.get(i).get(n));
						}
					}
				}
			}						
		}
		
		System.out.println("Segments removed: " + toRemove.size());
		roads.removeAll(toRemove);	
		
		System.out.println("Roads size: " + + roads.size());
	}

	// query data from database
	private void queryData() {
		try {
			this.motorways = Database.queryRoads("motorway");
			this.primary = Database.queryRoads("primary");
			this.secondary = Database.queryRoads("secondary");
			this.tertiary = Database.queryRoads("tertiary");
			this.residential = Database.queryRoads("residential");
			this.service = Database.queryRoads("service");
		} catch (CQLException cql) {
			cql.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {
			Sweep sweep = new Sweep();
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
