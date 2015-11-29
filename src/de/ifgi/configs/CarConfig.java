package de.ifgi.configs;

public class CarConfig extends Config{
	
	public CarConfig() {
		super();
		// road classes loaded for the transportation type car
		this.roadClasses = new String[] { "motorway", "primary", "secondary", "tertiary", "residential", "service" };
		// diameter of the bbox for each road class in m
		this.bboxDiameter = new double[] {15000.00, 7500.00, 5000.00, 3000.00, 1000.00, 500.00};
	}

}
