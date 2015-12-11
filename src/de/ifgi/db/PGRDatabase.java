package de.ifgi.db;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class PGRDatabase {

	// DB config
	private Properties config = new Properties();
	InputStream input = null;

	/**
	 * This class is only required as long as the
	 * new database does not contain the type
	 * attribute.
	 */
	public PGRDatabase() {
		// get db config
		try {
			String configFile = "serverDB.properties";
			input = Database.class.getResourceAsStream(configFile);
			config.load(input);
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
	 * @throws CQLException
	 * @throws OperationNotFoundException
	 * @throws TransformException
	 * @throws FactoryException
	 */
	public ArrayList<LineString> queryRoads(double lat, double lng, double diameter)
			throws CQLException, OperationNotFoundException, TransformException, FactoryException {

		ArrayList<LineString> roads = new ArrayList<LineString>();

		try {
			DataStore dataStore = DataStoreFinder.getDataStore(config);
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
			Envelope2D rec = new Envelope2D(DHDN, dest.x - (diameter / 2), dest.y - (diameter / 2), diameter, diameter);
			ReferencedEnvelope bbox = new ReferencedEnvelope(rec, DHDN);
			// transform back to WGS84
			bbox = bbox.transform(WGS84, true, 100);
			// geotools somehow messed up xy order, so init again with the right
			// order
			bbox.init(bbox.getMinY(), bbox.getMaxY(), bbox.getMinX(), bbox.getMaxX());

			// Filter filter1 = ff.like(ff.property("type"), type); // type e.g.
			// motorway
			Filter filter2 = ff.bbox(ff.property("geom_way"), bbox);
			// Filter filter = ff.and(filter1, filter2);

			FeatureCollection<SimpleFeatureType, Feature> roadFeatures = fs.getFeatures(filter2);

			FeatureIterator<Feature> it = roadFeatures.features();

			while (it.hasNext()) {
				Feature feature = it.next(); // simplefeatureimpl
				Geometry g = (Geometry) feature.getDefaultGeometryProperty().getValue();
				LineString mls = (LineString) g;
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
}
