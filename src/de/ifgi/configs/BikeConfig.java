package de.ifgi.configs;

public class BikeConfig extends Config {


	public BikeConfig() {
			super();
			// road classes loaded for the transportation type car
//			this.roadClasses = new String[] { "motorway", "primary", "secondary", "tertiary", "residential", "service" };
			// 31 = tertiary, 81=cycleway
			this.roadClasses = new String[] {"42", "31", "41", "42", "51", "63"};
			// diameter of the bbox for each road class in m
			this.bboxDiameter = new double[] {5000.00, 3000.00, 2000.00, 1000.00, 500.00, 300};
			this.DBTable = "roads_bike";
		}

}
