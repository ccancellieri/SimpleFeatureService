package it.geosolutions.sfs.data;



/**
 * 
 * @author carlo cancellieri
 *
 */
public abstract class FeatureFactorySPI {

	private final int priority;
	
	public FeatureFactorySPI(final int priority) throws Exception {
		this.priority=priority;
	}
	
	public abstract boolean canCreate() throws Exception;
	
	public abstract FeatureFactory getFeatureFactory() throws Exception;

	public int getPriority() {
		return priority;
	}

}
