import java.io.File;
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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import com.vividsolutions.jts.geom.MultiLineString;

public class Utils {
	// information for shapefile creation
	private final SimpleFeatureType TYPE = DataUtilities.createType("Location","the_geom:MultiLineString:srid=4326," + "number:Integer");
	private SimpleFeatureBuilder featureBuilder;

	public Utils() throws SchemaException {
		this.featureBuilder = new SimpleFeatureBuilder(TYPE);
	}

	public void writeToShapefile(ArrayList<MultiLineString> roads) {
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
		for (int i = 0; i < roads.size(); i++) {
			featureBuilder.add(roads.get(i));
			featureBuilder.add(i);
			SimpleFeature feature = featureBuilder.buildFeature(null);
			features.add(feature);
		}

		createShapefile(features);
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
