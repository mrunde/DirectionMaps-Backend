package tryouts;

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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;

public class Database {
	
	public static Map<String,Object> getConnectionParams(){
		Map<String,Object> params = new HashMap<String, Object>();
		params.put("dbtype", "postgis");
		params.put("host", "localhost");
		params.put("user", "postgres");
		params.put("passwd", "admin");
		params.put("schema", "public");
		params.put("database", "postgres");
		params.put("port", new Integer(5432));
		return params;
	}
	
	
	public static ArrayList<MultiLineString> queryRoads(String type) throws CQLException{
		ArrayList<MultiLineString> lines = new ArrayList<MultiLineString>();
		try{			
			DataStore dataStore = DataStoreFinder.getDataStore(getConnectionParams());
			String[] types = dataStore.getTypeNames();
//			System.out.println(Arrays.asList(types));
			
			FeatureSource fs = dataStore.getFeatureSource("roads");
//			System.out.println("Count: " + fs.getCount(Query.ALL));
			
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
			ReferencedEnvelope bbox = new ReferencedEnvelope(7.526751,7.722273,51.909702,52.014736, fs.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem());
			Filter filter1 = ff.like(ff.property("type"), type);
			Filter filter2 = ff.bbox(ff.property("geom"), bbox);
			Filter filter = ff.and(filter1, filter2);
			
			
			FeatureCollection<SimpleFeatureType, Feature> roads = fs.getFeatures(filter);

			FeatureIterator<Feature> it = roads.features();
			
			while(it.hasNext()){
				Feature feature = it.next(); //simplefeatureimpl

				Geometry g = (Geometry)feature.getDefaultGeometryProperty().getValue();
				MultiLineString mls = (MultiLineString) g;
				lines.add(mls);
			}
			it.close();
			
			dataStore.dispose();
		} catch(IOException ex){
			ex.printStackTrace();
		}
		return lines;
	}
}
