/*
 *  SFS - Open Source Simple Feature Service implementation
 *  Copyright (C) 2007-2012 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.sfs.data.join;

import java.io.File;
import java.util.Properties;

import it.geosolutions.sfs.data.FeatureFactory;
import it.geosolutions.sfs.data.FeatureFactorySPI;
import it.geosolutions.sfs.data.PropertiesUtils;

/**
 * 
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 *
 */
public class JoinFeatureFactorySPI extends FeatureFactorySPI {


    private final File dsProperties;
    private final File dwProperties;

    private Properties dsProp;
    private Properties dwProp;

            
	public JoinFeatureFactorySPI(final int priority,final File dsProperties,final File dwProperties) throws Exception {
		super(priority);
		this.dwProperties=dwProperties;
		this.dsProperties=dsProperties;
	}

	/**
	 * 
	 */
	@Override
	public boolean canCreate() throws Exception {
		try {
		    dsProp = PropertiesUtils.loadPropertiesFromURL(dsProperties.toURI().toURL());
		    dwProp = PropertiesUtils.loadPropertiesFromURL(dwProperties.toURI().toURL());
			// TODO may we want to check other here???
			
		} catch (Exception e){
			return false;
		}
		return true; 
	}
	
	@Override
	public FeatureFactory getFeatureFactory() throws Exception {
		return new JoinFeatureFactory(dsProp,dwProp);
	}

    @Override
    public FeatureFactory createFeatureFactory(String name, boolean noGeom) throws Exception {
        return new JoinFeatureFactory(dsProp,dwProp);
    }

}
