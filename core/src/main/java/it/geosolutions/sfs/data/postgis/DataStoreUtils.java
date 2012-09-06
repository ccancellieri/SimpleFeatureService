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
package it.geosolutions.sfs.data.postgis;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.geotools.data.DataAccessFactory.Param;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.Converters;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 *
 */
public abstract class DataStoreUtils {

	public final static Logger LOGGER = LoggerFactory.getLogger(DataStoreUtils.class);
	
	/**
	 * attrName IN (key1,key2,...,keyN)
	 * 
	 * @param attrName the attribute name to filter
	 * @param key optional list of string
	 * @return the query string if success, null otherwise.
	 * @throws NullPointerException
	 * @throws CQLException
	 */
	protected static Filter getInFilter(String attrName,
			String... keys) throws IllegalArgumentException, CQLException {
	
		if (keys == null) {
			return Filter.INCLUDE;
		}
	
		// check the size
		final int size = keys.length;
		if (size == 0) {
			return Filter.INCLUDE;
		}
		
		// case attrName IN ('f1','f2',...,'fn')
		if (keys[0] == null || attrName==null || attrName.isEmpty()) {
			throw new IllegalArgumentException(
					"The passed argument key list contains a null element or attribute is null or empty!");
		}
		StringBuilder query = new StringBuilder(attrName + " IN (");
		for (int i = 0; i < size; i++) {
			String key = keys[i];
			query.append((i == 0) ? "'" : ",'");
			query.append(key);
			query.append("'");
		}
		query.append(")");
	
		// filter=ff.equals(ff.property(locationKey), ff.literal());
		/**
		 * The "in predicate" was added in ECQL. (Have a look in the bnf
		 * http://docs
		 * .codehaus.org/display/GEOTOOLS/ECQL+Parser+Design#ECQLParserDesign-
		 * INPredicate) this is the rule for the falue list: <in value list> ::=
		 * <expression> {"," <expression>}
		 * 
		 * Thus, you could write sentences like: Filter filter =
		 * ECQL.toFilter("length IN (4100001,4100002, 4100003 )"); or Filter
		 * filter = ECQL.toFilter("name IN ('one','two','three')"); other Filter
		 * filter = ECQL.toFilter("length IN ( (1+2), 3-4, [5*6] )");
		 */
		return ECQL.toFilter(query.toString());
	}
	
	/**
	 * return the datastore or null
	 * 
	 * @param mosaicProp
	 * @param dataStoreProp
	 * @param mosaicDescriptor
	 * @param cmd
	 * @return
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 *             if datastoreProp is null
	 * @throws InstantiationException
	 * @throws IOException
	 */
	public static DataStore getDataStore(Properties dataStoreProp)
			throws IllegalArgumentException, InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException {
		if (dataStoreProp == null) {
			throw new IllegalArgumentException(
					"Unable to get datastore properties.");
		}
	
		DataStore dataStore = null;
	
		// SPI
		final String SPIClass = dataStoreProp.getProperty("SPI");
		try {
			DataStoreFactorySpi spi = (DataStoreFactorySpi) Class.forName(
					SPIClass).newInstance();
	
			final Map<String, Serializable> params = createDataStoreParamsFromPropertiesFile(dataStoreProp, spi);
	
			// datastore creation
			dataStore = spi.createDataStore(params);
	
		} catch (IOException ioe) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error(
						"Problems setting up (creating or connecting) the datasource. The message is: "
								+ ioe.getLocalizedMessage(), ioe);
			}
			throw ioe;
		}
	
		if (dataStore == null) {
			throw new NullPointerException(
					"The required resource (DataStore) was not found or if insufficent parameters were given.");
		}
		return dataStore;
	}

	/**
	 * 
	 * @param properties
	 * @param spi
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Serializable> createDataStoreParamsFromPropertiesFile(
	        Properties properties, DataStoreFactorySpi spi) throws IOException {
	    // get the params
	    final Map<String, Serializable> params = new HashMap<String, Serializable>();
	    final Param[] paramsInfo = spi.getParametersInfo();
	    for (Param p : paramsInfo) {
	        // search for this param and set the value if found
	        if (properties.containsKey(p.key))
	            params.put(p.key, (Serializable) Converters.convert(properties.getProperty(p.key),
	                    p.type));
	        else if (p.required && p.sample == null)
	            throw new IOException("Required parameter missing: " + p.toString());
	    }
	
	    return params;
	}
	
}
