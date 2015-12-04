package de.ifgi.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
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

public class Utils {
	// information for shapefile creation
	private final SimpleFeatureType TYPE = DataUtilities.createType("Location","the_geom:MultiLineString:srid=4326," + "osm_id:Integer," + "name:String," + "ref:String," + "type:String");
	private SimpleFeatureBuilder featureBuilder;

	public Utils() throws SchemaException {
		this.featureBuilder = new SimpleFeatureBuilder(TYPE);
	}

	public void writeToShapefile(ArrayList<MultiLineString> roads) {
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
		for (int i = 0; i < roads.size(); i++) {
			featureBuilder.add(roads.get(i));
			Feature featureProps = (Feature)roads.get(i).getUserData();
			featureBuilder.add(featureProps.getProperty("osm_id").getValue());
			featureBuilder.add(featureProps.getProperty("name").getValue());
			featureBuilder.add(featureProps.getProperty("ref").getValue());
			featureBuilder.add(featureProps.getProperty("type").getValue());
			SimpleFeature feature = featureBuilder.buildFeature(null);
			features.add(feature);
		}

		createShapefile(features);
		createGeoJSON(features);
		
	}
	
	// creates a geojson file
	private void createGeoJSON(ArrayList<SimpleFeature> features){
		SimpleFeatureCollection collection = new ListFeatureCollection(TYPE, features);
		
		FeatureJSON fjson = new FeatureJSON();		
		try{			
			fjson.writeFeatureCollection(collection, new FileOutputStream(new File("geojson.json")));
		} catch(IOException ex){
			ex.printStackTrace();
		}
	}

	// creates a shapefile (method is mostly copied from geotools tutorials)
	private void createShapefile(ArrayList<SimpleFeature> features) {
		try {
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
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.out.println("Done writing!");
	}

}
