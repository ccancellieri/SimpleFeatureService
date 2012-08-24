package it.geosolutions.sfs.data.postgis;

import it.geosolutions.sfs.data.FeatureFactory;
import it.geosolutions.sfs.data.FeatureFactorySPI;

import java.io.File;
import java.util.Properties;

public class PGisFeatureFactorySPI extends FeatureFactorySPI {

	private final File properties;
	
	private Properties prop;
	
	public PGisFeatureFactorySPI(final File properties, final int priority) throws Exception {
		super(priority);
		this.properties=properties;
	}

	public Properties getProp() {
		return prop;
	}

	/**
	 * 
	 */
	@Override
	public boolean canCreate() throws Exception {
		try {
			prop = DataStoreUtils.loadPropertiesFromURL(properties.toURI().toURL());
			
			// TODO may we want to check the database settings here???
			
		} catch (Exception e){
			return false;
		}
		return true; 
	}
	
	@Override
	public FeatureFactory getFeatureFactory() throws Exception {
		return new PGisFeatureFactory(prop);
	}

}
