package de.ifgi.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.MultiLineString;

import de.ifgi.landmark.Landmark;

public class Utils {
	// information for shapefile creation
	private final SimpleFeatureType ROAD_TYPE = DataUtilities.createType("Location","the_geom:MultiLineString:srid=4326," + "osm_id:Integer," + "name:String," + "ref:String," + "type:String");
	private final String ROAD_OUTPUT = "roads";
	private SimpleFeatureBuilder roadFeatureBuilder;
	private final SimpleFeatureType LANDMARK_TYPE = DataUtilities.createType("Landmark", "the_geom:Point:srid=4326," + "name:String," + "ref:String");
	private final String LANDMARK_OUTPUT = "landmarks";
	private SimpleFeatureBuilder landmarkFeatureBuilder;

	public Utils() throws SchemaException {
		this.roadFeatureBuilder = new SimpleFeatureBuilder(ROAD_TYPE);
		this.landmarkFeatureBuilder = new SimpleFeatureBuilder(LANDMARK_TYPE);
	}

	public void writeRoadsToShapefile(ArrayList<MultiLineString> roads) {
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
		for (int i = 0; i < roads.size(); i++) {
			roadFeatureBuilder.add(roads.get(i));
			Feature featureProps = (Feature)roads.get(i).getUserData();
			roadFeatureBuilder.add(featureProps.getProperty("osm_id").getValue());
			roadFeatureBuilder.add(featureProps.getProperty("osm_name").getValue());
//			featureBuilder.add(featureProps.getProperty("ref").getValue());
			roadFeatureBuilder.add(featureProps.getProperty("clazz").getValue());
			SimpleFeature feature = roadFeatureBuilder.buildFeature(null);
			features.add(feature);
		}
		
		createGeoJSON(features, ROAD_OUTPUT);
		createShapefile(features, ROAD_TYPE, ROAD_OUTPUT);
	}
	
	public void writeLandmarksToShapefile(ArrayList<Landmark> landmarks) {
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
		for (int i = 0; i < landmarks.size(); i++) {
			landmarkFeatureBuilder.add(landmarks.get(i));
		}
		
		createGeoJSON(features, LANDMARK_OUTPUT);
		createShapefile(features, LANDMARK_TYPE, LANDMARK_OUTPUT);
	}
	
	// creates a geojson file for the roads
	private void createGeoJSON(ArrayList<SimpleFeature> features, String outputName){
		SimpleFeatureCollection collection = new ListFeatureCollection(ROAD_TYPE, features);
		
		FeatureJSON fjson = new FeatureJSON();		
		try{			
			fjson.writeFeatureCollection(collection, new FileOutputStream(new File(outputName + ".json")));
		} catch(IOException ex){
			ex.printStackTrace();
		}
	}

	// creates a shapefile (method is mostly copied from geotools tutorials)
	private void createShapefile(ArrayList<SimpleFeature> features, SimpleFeatureType type, String outputName) {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

			File newFile = new File(outputName + ".shp");

			ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

			Map<String, Serializable> params = new HashMap<String, Serializable>();
			params.put("url", newFile.toURI().toURL());
			params.put("create spatial index", Boolean.TRUE);

			ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

			/*
			 * TYPE is used as a template to describe the file contents
			 */
			newDataStore.createSchema(type);

			/*
			 * Write the features to the shapefile
			 */
			Transaction transaction = new DefaultTransaction("create");

			String typeName = newDataStore.getTypeNames()[0];
			SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);
			SimpleFeatureType SHAPE_TYPE = featureSource.getSchema();
			/*
			 * The Shapefile format has a couple limitations: - "the_geom" is
			 * always first, and used for the geometry attribute name -
			 * "the_geom" must be of type Point, MultiPoint, MuiltiLineString,
			 * MultiPolygon - Attribute names are limited in length - Not all
			 * data types are supported (example Timestamp represented as Date)
			 * 
			 * Each data store has different limitations so check the resulting
			 * SimpleFeatureType.
			 */
			System.out.println("SHAPE:" + SHAPE_TYPE);

			if (featureSource instanceof SimpleFeatureStore) {
				SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
				/*
				 * SimpleFeatureStore has a method to add features from a
				 * SimpleFeatureCollection object, so we use the
				 * ListFeatureCollection class to wrap our list of features.
				 */
				SimpleFeatureCollection collection = new ListFeatureCollection(type, features);
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
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.out.println("Done writing!");
	}

}
