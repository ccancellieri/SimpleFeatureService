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
package it.geosolutions.sfs.data.dw;

import it.geosolutions.sfs.data.FeatureFactory;
import it.geosolutions.sfs.data.postgis.PGisFeatureFactorySPI;

import java.io.File;

/**
 * 
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 *
 */
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
