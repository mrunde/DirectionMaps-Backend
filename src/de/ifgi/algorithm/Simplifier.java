package de.ifgi.algorithm;


import java.util.ArrayList;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

public class Simplifier {
	
	
	public Simplifier(){
		
	}
	
	public ArrayList<MultiLineString> simplify(ArrayList<MultiLineString> roads) {
		ArrayList<MultiLineString> result = new ArrayList<MultiLineString>();
		GeometryFactory f = new GeometryFactory();
		roads.forEach(road -> {
			Object userData = road.getUserData();
			LineString l = (LineString)TopologyPreservingSimplifier.simplify((Geometry)road, 0.01);
			MultiLineString ml = new MultiLineString(new LineString[]{l}, f);
			ml.setUserData(userData);
			result.add(ml);
		});
		return result;
	}

}
