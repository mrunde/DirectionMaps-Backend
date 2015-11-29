import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import com.vividsolutions.jts.geom.MultiLineString;

public class Database {

	private Map<String, Object> params;

	public Database() {
		this.params = new HashMap<String, Object>();
		params.put("dbtype", "postgis");
		params.put("host", "localhost");
		params.put("user", "postgres");
		params.put("passwd", "0acc1020,");
		params.put("schema", "public");
		params.put("database", "postgis");
		params.put("port", new Integer(5432));
	}
	
	/**
	 * 
	 * @param lat destination lat
	 * @param lng destination lng
	 * @param diameter diameter of the bounding box in m
	 * @param type transportation type
	 * @return
	 * @throws CQLException
	 * @throws OperationNotFoundException
	 * @throws TransformException
	 * @throws FactoryException
	 */
	public ArrayList<MultiLineString> queryRoads(double lat, double lng, double diameter, String type)
			throws CQLException, OperationNotFoundException, TransformException, FactoryException {

		ArrayList<MultiLineString> roads = new ArrayList<MultiLineString>();

		try {
			DataStore dataStore = DataStoreFinder.getDataStore(params);
			FeatureSource fs = dataStore.getFeatureSource("roads");
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
			
			Filter filter1 = ff.like(ff.property("type"), type); // type e.g. motorway														
			Filter filter2 = ff.bbox(ff.property("geom"), bbox);
			Filter filter = ff.and(filter1, filter2);

			FeatureCollection<SimpleFeatureType, Feature> roadFeatures = fs.getFeatures(filter);

			FeatureIterator<Feature> it = roadFeatures.features();

			while (it.hasNext()) {
				Feature feature = it.next(); // simplefeatureimpl
				Geometry g = (Geometry) feature.getDefaultGeometryProperty().getValue();
				MultiLineString mls = (MultiLineString) g;
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
