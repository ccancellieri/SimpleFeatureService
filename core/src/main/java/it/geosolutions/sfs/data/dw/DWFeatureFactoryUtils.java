package it.geosolutions.sfs.data.dw;

import java.util.Map;

public abstract class DWFeatureFactoryUtils {

	
	public static boolean checkHints(Map<String, String> hints){
		if (hints!=null){
			if (hints.containsKey(DWFeatureFactory.VALUE)){
				return true;
			}
		}
		return false;
	}

}
