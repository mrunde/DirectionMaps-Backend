package de.ifgi.configs;
import de.ifgi.configs.CarConfig;
import de.ifgi.configs.Config;

public class ConfigLoader {
	
	public Config getConfig(String type) {
		Config config = null;
		// get appropriate config
		switch (type) {
		case "car":
			config = new CarConfig();
			break;

		default:
			break;
		}
		
		return config;		
	}

}
