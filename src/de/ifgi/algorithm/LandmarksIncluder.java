package de.ifgi.algorithm;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

import de.ifgi.landmark.Landmark;

public class LandmarksIncluder {

	public LandmarksIncluder() {
		
	}
	
	public ArrayList<Landmark> includeLandmarks(ArrayList<MultiLineString> roads) {
		ArrayList<Landmark> result = new ArrayList<Landmark>();
		ArrayList<Landmark> landmarks = loadLandmarks();
		roads.forEach(road -> {
			for (Iterator<Landmark> iterator = landmarks.iterator(); iterator.hasNext(); ) {
				Landmark landmark = iterator.next();
//				double distance = landmark.getLocation().distance(road);
				double distance = 0;
				try {
					distance = JTS.orthodromicDistance(road.getCoordinates()[road.getNumGeometries() / 2], landmark.getLocation().getCoordinate(), ((SimpleFeature)road.getUserData()).getType().getCoordinateReferenceSystem());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (distance < landmark.getRadius() && distance > 10) {
					result.add(landmark);
					iterator.remove();
				}
			}
		});
		
		System.out.println("Landmarks size: " + result.size());
		return result;
	}
	
	private ArrayList<Landmark> loadLandmarks() {
		ArrayList<Landmark> result = new ArrayList<Landmark>();
		GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();
		
		try {
			File xmlFile = new File("src/res/landmarks.xml");
			
			DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			
			Document doc = (Document) dBuilder.parse(xmlFile);
			
			NodeList nList = doc.getElementsByTagName("landmark");
			
			for (int i = 0; i < nList.getLength(); i++) {
				Node tempNode = nList.item(i);
				if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element) tempNode;
					
					String name = element.getElementsByTagName("name").item(0).getTextContent();
					String category = element.getElementsByTagName("category").item(0).getTextContent();
					double lat = Double.valueOf(element.getElementsByTagName("lat").item(0).getTextContent());
					double lng = Double.valueOf(element.getElementsByTagName("lng").item(0).getTextContent());
//					Coordinate[] coordinates = {new Coordinate(lat, lng)};
//					CoordinateSequence cs = CoordinateArraySequenceFactory.instance().create(coordinates);
//					Point location = new Point(cs, gf);
					Point location = gf.createPoint(new Coordinate(lng, lat));
					double radius = Double.valueOf(element.getElementsByTagName("radius").item(0).getTextContent());
					
					result.add(new Landmark(name, category, location, radius));
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		return result;
	}
}
