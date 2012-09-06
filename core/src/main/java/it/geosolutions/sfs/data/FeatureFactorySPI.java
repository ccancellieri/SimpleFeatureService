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
package it.geosolutions.sfs.data;

/**
 * 
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 *
 */
public abstract class FeatureFactorySPI {

	private final int priority;
	
	/**
	 * 
	 * @param priority an integer representing the priority of this FeatureFactory
	 * @throws Exception
	 */
	public FeatureFactorySPI(final int priority) throws Exception {
		this.priority=priority;
	}
	
	/**
	 * Check if the FeatureFactory can be created by this SPI
	 * @return true if success
	 * @throws Exception
	 */
	public abstract boolean canCreate() throws Exception;
	
	/**
	 * Actually instantiate the FeatureFactory
	 * @return the newly created FeatureFactory
	 * @throws Exception
	 */
	public abstract FeatureFactory getFeatureFactory() throws Exception;
	
	/**
         * Actually instantiate the FeatureFactory
         * @return the newly created FeatureFactory
         * @throws Exception
         */
        public abstract FeatureFactory createFeatureFactory(String name, boolean noGeom) throws Exception;

	/**
	 * @return an integer representing the priority of this FeatureFactory
	 */
	public int getPriority() {
		return priority;
	}

}
