package de.ifgi.db;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

import de.ifgi.configs.Config;


public class Database {
	// DB config
	private Properties DBParams = new Properties();
	private InputStream input = null;
	private Config config;

	public Database(String source, Config config) {
		this.config = config;
		// get db params
		try {
			String paramsSource = source == "local" ? "localDB.properties" : "serverDB.properties";
			input = Database.class.getResourceAsStream(paramsSource);
			DBParams.load(input);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}


	}
	
	
	// query data from database
	public Map<String, ArrayList<MultiLineString>> queryData(double destLat, double destLng) 
			throws OperationNotFoundException, TransformException, FactoryException {
		Map<String, ArrayList<MultiLineString>> roadLayers = new HashMap<String, ArrayList<MultiLineString>>();
		String[] roadClasses = config.roadClasses;
		for (int i = 0; i < roadClasses.length; i++) {
			// bbox diameters for each road class
			double diameter = config.bboxDiameter[i];
			roadLayers.put(roadClasses[i], queryRoads(destLat, destLng, diameter, roadClasses[i]));
		}
		return roadLayers;
	}

	/**
	 * 
	 * @param lat
	 *            destination lat
	 * @param lng
	 *            destination lng
	 * @param diameter
	 *            diameter of the bounding box in m
	 * @param type
	 *            transportation type
	 * @return
	 * @throws NoSuchAuthorityCodeException 
	 * @throws CQLException
	 * @throws OperationNotFoundException
	 * @throws TransformException
	 * @throws FactoryException
	 * @throws MismatchedDimensionException 
	 */
	public ArrayList<MultiLineString> queryRoads(double lat, double lng, double diameter, String type) 
			throws NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException, TransformException {

		ArrayList<MultiLineString> roads = new ArrayList<MultiLineString>();

		try {
			DataStore dataStore = DataStoreFinder.getDataStore(DBParams);
			FeatureSource fs = dataStore.getFeatureSource("roads_pgr");
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
			
			// create bbox dynamicaly
			// define reference systems 
			CoordinateReferenceSystem WGS84 = CRS.decode("EPSG:4326");
			// DHDN zone 3
			CoordinateReferenceSystem DHDN = CRS.decode("EPSG:31467"); 
			MathTransform mathTransform = CRS.findMathTransform(WGS84, DHDN, true);
			// destination
			DirectPosition2D dest = new DirectPosition2D(WGS84, lat, lng);
			// transform into DHDN
			mathTransform.transform(dest, dest);
			// construct bbox according to the definition in the config
			Envelope2D rec = new Envelope2D(DHDN, dest.x - (diameter/2), dest.y - (diameter/2)  , diameter, diameter);
			ReferencedEnvelope bbox = new ReferencedEnvelope(rec, DHDN);
			// transform back to WGS84
			bbox = bbox.transform(WGS84, true, 100);
			// geotools somehow messed up xy order, so init again with the right order
			bbox.init(bbox.getMinY(), bbox.getMaxY(), bbox.getMinX(), bbox.getMaxX());
			
			
			Filter filter1 = ff.equals(ff.property("clazz"), ff.literal(Integer.parseInt(type)));													
			Filter filter2 = ff.bbox(ff.property("geom_way"), bbox);
			Filter filter = ff.and(filter1, filter2);

			FeatureCollection<SimpleFeatureType, Feature> roadFeatures = fs.getFeatures(filter);
			
			FeatureIterator<Feature> it = roadFeatures.features();

			GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
			
			while (it.hasNext()) {
				Feature feature = it.next(); // simplefeatureimpl
				Geometry g = (Geometry) feature.getDefaultGeometryProperty().getValue();
				LineString ls = (LineString) g;
				
				MultiLineString mls = geometryFactory.createMultiLineString(new LineString[]{ls});
				mls.setUserData(feature);
				roads.add(mls);
			}

			it.close();
			dataStore.dispose();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return roads;
	}
	
	/**
	 * 
	 * @param lat
	 *            destination lat
	 * @param lng
	 *            destination lng
	 * @param diameter
	 *            diameter of the bounding box in m
	 * @param type
	 *            transportation type
	 * @return
	 * @throws NoSuchAuthorityCodeException 
	 * @throws CQLException
	 * @throws OperationNotFoundException
	 * @throws TransformException
	 * @throws FactoryException
	 * @throws MismatchedDimensionException 
	 */
	public ArrayList<LineString> queryRoads(double lat, double lng) 
			throws NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException, TransformException {

		ArrayList<LineString> roads = new ArrayList<LineString>();

		try {
			DataStore dataStore = DataStoreFinder.getDataStore(DBParams);
			FeatureSource fs = dataStore.getFeatureSource("roads_pgr");
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
			
			// create bbox dynamicaly
			// define reference systems 
			CoordinateReferenceSystem WGS84 = CRS.decode("EPSG:4326");
			// DHDN zone 3
			CoordinateReferenceSystem DHDN = CRS.decode("EPSG:31467"); 
			MathTransform mathTransform = CRS.findMathTransform(WGS84, DHDN, true);
			// destination
			DirectPosition2D dest = new DirectPosition2D(WGS84, lat, lng);
			// transform into DHDN
			mathTransform.transform(dest, dest);
			// construct bbox according to the definition in the config
			double diameter = config.bboxDiameter[0];
			Envelope2D rec = new Envelope2D(DHDN, dest.x - (diameter/2), dest.y - (diameter/2)  , diameter, diameter);
			ReferencedEnvelope bbox = new ReferencedEnvelope(rec, DHDN);
			// transform back to WGS84
			bbox = bbox.transform(WGS84, true, 100);
			// geotools somehow messed up xy order, so init again with the right order
			bbox.init(bbox.getMinY(), bbox.getMaxY(), bbox.getMinX(), bbox.getMaxX());			
												
			Filter filter = ff.bbox(ff.property("geom_way"), bbox);

			FeatureCollection<SimpleFeatureType, Feature> roadFeatures = fs.getFeatures(filter);
			
			FeatureIterator<Feature> it = roadFeatures.features();
			
			while (it.hasNext()) {
				Feature feature = it.next(); // simplefeatureimpl
				Geometry g = (Geometry) feature.getDefaultGeometryProperty().getValue();
				LineString ls = (LineString) g;
				ls.setUserData(feature);
				
				roads.add(ls);
			}

			it.close();
			dataStore.dispose();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return roads;
	}
}
