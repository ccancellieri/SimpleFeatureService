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

import it.geosolutions.sfs.controller.SFSParamsModel;
import it.geosolutions.sfs.data.postgis.DataStoreUtils;
import it.geosolutions.sfs.data.postgis.PGisFeatureFactory;
import it.geosolutions.sfs.utils.GTTools;
import it.geosolutions.sfs.utils.JSONUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * 
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 * 
 */
public class DWFeatureFactory extends PGisFeatureFactory {

	public DWFeatureFactory(final Properties prop) throws Exception {
		super(prop);
	}
	
	public final static String VALUE="value"; 

	/**
	 * 
	 */
	@Override
	public Object getData(SFSParamsModel params) throws Exception {
		switch (params.getMode()) {
		// FeatureSource.getFeatures(Query/Filter)
		// /data/layername?mode=features&...
		// Should take the filter and split it into two parts, the
		// one natively supported, and the one that is
		// performed later in memory. Also, it should pass
		// down the view params contained in the
		// Hints.VIRTUAL_TABLE_PARAMETERS hint.
		case features:
			StringWriter sw = null;
			DataStore dataStore = null;
			try {
				// do we need data from DataWarehouse?
				boolean dwNeeded =  DWFeatureFactoryUtils.checkHints(params.getHints());
				
				// //////////////////////////
				// GET Postgis DataStore
				dataStore = DataStoreUtils.getDataStore(getProp());

				SimpleFeatureType schema = dataStore.getSchema(params.getLayerName());

				// //////////////////////////////////
				// prepare query to PostGis DataStore
				
				if (ArrayUtils.contains(params.getAttrs(), VALUE)) {
					params.setAttrs((String[])ArrayUtils.removeElement(params.getAttrs(), VALUE));
				} else {
					// query does not require DataWarehouse since no value attribute is specified
					dwNeeded=false;
				}
				
				final Query query = GTTools.buildQuery(params.getRequest()
						.getParameterMap(), params.getAttrs(), params.getFid(),
						params.getQueryable(), params.getCrs(), params
								.getOrderBy(), params.getDirections(), params
								.isNoGeom(), params.getGeometry(), params
								.getTolerance(), params.getBbox(), params
								.getLon(), params.getLat(), params.getOffset(),
						params.getLimit(), schema);

				// //////////////////////////
				// GET DATA FROM POSTGIS
				final SimpleFeatureSource featureSource = dataStore
						.getFeatureSource(params.getLayerName());
				SimpleFeatureCollection collection = featureSource
						.getFeatures(query);

				sw = new StringWriter();

				if (dwNeeded) {
					// ////////////////////////////////////////
					// GET DATA FROM DataWarehouse
//					JSONObject json = DWJSONUtils.getDWJSON(new File(
//							"src/main/resources/mdx_results.json")); 
					// JSONObject
					// TODO: probably use param.getHints() to build the query...
					String mdxQuery="http://hqlqatcdras1.hq.un.fao.org:8080/techcdr-mdx/MdxQueryServlet" +
							"?workspace=Faostat&catalog=sdw_faostat_q&schema=Faostat+Production&language=en" +
							"&attributes=area,year,area_harvested_wheat,area_harvested_rice,area_harvested_maize,area_harvested_flag_wheat,area_harvested_flag_rice,area_harvested_flag_maize,yield_wheat,yield_rice,yield_maize,yield_flag_wheat,yield_flag_rice,yield_flag_maize,production_quantity_wheat,production_quantity_rice,production_quantity_maize,production_quantity_flag_wheat,production_quantity_flag_rice,production_quantity_flag_maize&mdx=select+NON+EMPTY+Hierarchize%28Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5510%5D%7D%2C+%7B%5BItem%5D.%5BWheat%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5510%5D%7D%2C+%7B%5BItem%5D.%5BMaize%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5510%5D%7D%2C+%7B%5BItem%5D.%5BRice%2C+paddy%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5510%5D%7D%2C+%7B%5BItem%5D.%5BWheat%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5510%5D%7D%2C+%7B%5BItem%5D.%5BMaize%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5510%5D%7D%2C+%7B%5BItem%5D.%5BRice%2C+paddy%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5312%5D%7D%2C+%7B%5BItem%5D.%5BWheat%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5312%5D%7D%2C+%7B%5BItem%5D.%5BMaize%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5312%5D%7D%2C+%7B%5BItem%5D.%5BRice%2C+paddy%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5312%5D%7D%2C+%7B%5BItem%5D.%5BWheat%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5312%5D%7D%2C+%7B%5BItem%5D.%5BMaize%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5312%5D%7D%2C+%7B%5BItem%5D.%5BRice%2C+paddy%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5419%5D%7D%2C+%7B%5BItem%5D.%5BWheat%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5419%5D%7D%2C+%7B%5BItem%5D.%5BMaize%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5419%5D%7D%2C+%7B%5BItem%5D.%5BRice%2C+paddy%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5419%5D%7D%2C+%7B%5BItem%5D.%5BWheat%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5419%5D%7D%2C+%7B%5BItem%5D.%5BMaize%5D%7D%29%2C+Crossjoin%28%7B%5BMeasures%5D.%5Bf5419%5D%7D%2C+%7B%5BItem%5D.%5BRice%2C+paddy%5D%7D%29%29%29%29%29%29%29%29%29%29%29%29%29%29%29%29%29%29%29+ON+COLUMNS%2C%0D%0A%0D%0A++NON+EMPTY+Hierarchize%28Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2001%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2002%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2003%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2004%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2005%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2006%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2007%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2008%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2009%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2010%5D%7D%29%2C+Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2011%5D%7D%29%29%29%29%29%29%29%29%29%29%29%29+ON+ROWS%0D%0A%0D%0Afrom+%5BCROPS%5D" +
							"&arguments=&callback=%2C+Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2011%5D%7D%29%29%29%29%29%29%29%29%29%29%29%29+ON+ROWS%0D%0A%0D%0Afrom+%5BCROPS%5D" +
							"&mime=application%2Fjson&arguments=&callback=";
					JSONObject json = DWJSONUtils.fetchRESTObject(mdxQuery);

					// ////////////////////////////
					// load data into the map
					Map<String, Map<String, String>> dwSource = DWJSONUtils
							.loadSource(json, getProp().getProperty("dw_pk"));

					// /////////////////////////////////////
					// actually merge data producing output
					DWJSONUtils.writeDWFeatureCollection(collection, dwSource, params.getHints().get(VALUE),//"yield_wheat"
							getProp().getProperty("pg_pk"), true, sw);
				} else {
					// /////////////////////////////////////
					// actually merge data producing output
					JSONUtils.writeFeatureCollection(collection, true, sw);
				}

			} finally {
				if (dataStore != null)
					dataStore.dispose();
				IOUtils.closeQuietly(sw);
			}
			
			String out=sw.toString();
			FileWriter fw =new FileWriter(new File("src/main/resources/tmp.log"));
			fw.write(out);
			fw.close();
			return out;


		case count:
		case bounds:
		default:
			return super.getData(params);

		}

	}

	private static SimpleFeatureType mergeSchema(SimpleFeatureType schema) {
		final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.init(schema);
		b.setName(schema.getName());
		b.add(VALUE, String.class);
		return b.buildFeatureType();
	}

	@Override
	protected SimpleFeatureType getSimpleFeatureType(final String typeName,
			DataStore dataStore) throws Exception {
		try {
			return mergeSchema(super.getSimpleFeatureType(typeName, dataStore));
		} catch (Exception ioe) {
			if (!IGNORE_NON_FATAL_ERRORS)
				throw ioe;
			return null;
		}
	}


	//
	// @Override
	// public SimpleFeatureType[] getAllSchemas() throws Exception {
	// SimpleFeatureType[] schemas = super.getAllSchemas();
	// int size = schemas.length;
	// for (int i = 0; i < size; i++) {
	// try {
	// schemas[i] = mergeSchema(schemas[i]);
	// } catch (Exception ioe) {
	// if (!IGNORE_NON_FATAL_ERRORS)
	// throw ioe;
	// }
	// }
	// return schemas;
	//
	// }
	
	// /**
	// * @param typeName
	// * @throws Exception
	// */
	// public BoundingBox getBoundingBox(final String typeName) throws Exception
	// {
	// DataStore dataStore = null;
	// try {
	// dataStore = DataStoreUtils.getDataStore(prop);
	// GTTools.getBB(dataStore.getFeatureSource(typeName), query);
	// } finally {
	// if (dataStore != null)
	// dataStore.dispose();
	// }
	// }

	// /**
	// * @deprecated model
	// * @param typeName
	// * @throws Exception
	// */
	// public void get(final String typeName) throws Exception {
	// DataStore dataStore = null;
	// try {
	// dataStore = DataStoreUtils.getDataStore(prop);
	// } finally {
	// if (dataStore != null)
	// dataStore.dispose();
	// }
	// }

	// /**
	// * {field}_{query_op}={value}: specify a filter expression, field must be
	// in
	// * the list of fields specified by queryable, supported query_op's are:
	// eq:
	// * equal to ne: not equal to lt: lower than lte: lower than or equal to
	// gt:
	// * greater than gte: greater than or equal to like ilike
	// * @deprecated model
	// */
	// private boolean checkQueryParams(HttpServletRequest request) {
	// Map<String, String> map = request.getParameterMap();//
	// (HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
	// // TODO
	//
	// // FAKE logic
	// if (map != null)
	// return true;
	// return false;
	// }
}
