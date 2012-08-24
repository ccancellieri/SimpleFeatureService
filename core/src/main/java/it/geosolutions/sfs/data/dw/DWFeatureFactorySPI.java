package it.geosolutions.sfs.data.dw;

import it.geosolutions.sfs.data.FeatureFactory;
import it.geosolutions.sfs.data.postgis.PGisFeatureFactorySPI;

import java.io.File;

public class DWFeatureFactorySPI extends PGisFeatureFactorySPI {

	public DWFeatureFactorySPI(final File properties, final int priority) throws Exception {
		super(properties,priority);
	}

	/**
	 * 
	 */
	@Override
	public boolean canCreate() throws Exception {
		try {
			super.canCreate();
			
			// TODO may we want to check other here???
			
		} catch (Exception e){
			return false;
		}
		return true; 
	}
	
	@Override
	public FeatureFactory getFeatureFactory() throws Exception {
		return new DWFeatureFactory(getProp());
	}

}
