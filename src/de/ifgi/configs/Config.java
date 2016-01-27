package de.ifgi.configs;

public abstract class Config {
	// road classes loaded for the transportation type car
	public String[] roadClasses;
	// diameter of the bbox for each road class
	public double[] bboxDiameter;
	// database table name
	public String DBTable;
	
	public Config() {
		
	}
}
