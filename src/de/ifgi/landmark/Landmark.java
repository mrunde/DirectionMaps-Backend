package de.ifgi.landmark;

import com.vividsolutions.jts.geom.Point;

public class Landmark {

	private String name;
	private String category;
	private Point location;
	private double radius;
	
	public Landmark(String name, String category, Point location, double radius) {
		this.name = name;
		this.category = category;
		this.location = location;
		this.radius = radius;
	}
	
	public String toString() {
		return name + "|" + category + "|" + location.toString() + "|" + radius;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Point getLocation() {
		return location;
	}

	public void setLocation(Point location) {
		this.location = location;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}
}
