package it.geosolutions.sfs.data.dw;

import it.geosolutions.sfs.controller.SFSParamsModel;
import it.geosolutions.sfs.data.postgis.PGisFeatureFactory;
import it.geosolutions.sfs.utils.GTTools;

import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;

import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;

public class DWFeatureFactory extends PGisFeatureFactory {


	public DWFeatureFactory(final Properties prop) throws Exception {
		super(prop);
	}

	/**
	 * 
	 */
	@Override
	public Object getData(SFSParamsModel params) throws Exception {
		StringWriter sw = null;
		DataStore dataStore = null;

		switch (params.getMode()) {
		// FeatureSource.getFeatures(Query/Filter)
		// /data/layername?mode=features&...
		// Should take the filter and split it into two parts, the
		// one natively supported, and the one that is
		// performed later in memory. Also, it should pass
		// down the view params contained in the
		// Hints.VIRTUAL_TABLE_PARAMETERS hint.
		case features:
			try {
					dataStore = DataStoreUtils.getDataStore(getProp());
		
					SimpleFeatureType schema = dataStore.getSchema(params
							.getLayerName());
	
					sw = new StringWriter();
					final Query query = GTTools.buildQuery(params.getRequest()
							.getParameterMap(), params.getAttrs(), params.getFid(),
							params.getQueryable(), params.getCrs(),
							params.getOrderBy(), params.getDirections(), params
									.isNoGeom(), params.getGeometry(), params
									.getTolerance(), params.getBbox(), params.getLon(),
							params.getLat(), params.getOffset(), params.getLimit(),
							schema);
	
					final SimpleFeatureSource featureSource = dataStore
							.getFeatureSource(params.getLayerName());

					SimpleFeatureCollection collection=featureSource.getFeatures(query);
					
//					JSONObject json=DWJSONUtils.getDWJSON(new File("src/main/resources/mdx_results.json")); //TODO change me
					JSONObject json=DWJSONUtils.fetchRESTObject("http://hqlqatcdras1.hq.un.fao.org:8080/techcdr-mdx/MdxQueryServlet?workspace=Faostat&catalog=sdw_faostat_q&schema=Faostat+Production&language=en&attributes=area,year,area_harvested_wheat,area_harvested_rice,area_harvested_maize,area_harvested_flag_wheat,area_harvested_flag_rice,area_harvested_flag_maize,yield_wheat,yield_rice,yield_maize,yield_flag_wheat,yield_flag_rice,yield_flag_maize,production_quantity_wheat,production_quantity_rice,production_quantity_maize,production_quantity_flag_wheat,production_quantity_flag_rice,production_quantity_flag_maize&mdx=select+NON+EMPTY+Hierarchize%28Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5510%5D%7D%2C+%7B%5BItem%5D.%5BWheat%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5510%5D%7D%2C+%7B%5BItem%5D.%5BMaize%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5510%5D%7D%2C+%7B%5BItem%5D.%5BRice%2C+paddy%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5510%5D%7D%2C+%7B%5BItem%5D.%5BWheat%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5510%5D%7D%2C+%7B%5BItem%5D.%5BMaize%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5510%5D%7D%2C+%7B%5BItem%5D.%5BRice%2C+paddy%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5312%5D%7D%2C+%7B%5BItem%5D.%5BWheat%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5312%5D%7D%2C+%7B%5BItem%5D.%5BMaize%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5312%5D%7D%2C+%7B%5BItem%5D.%5BRice%2C+paddy%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5312%5D%7D%2C+%7B%5BItem%5D.%5BWheat%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5312%5D%7D%2C+%7B%5BItem%5D.%5BMaize%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5312%5D%7D%2C+%7B%5BItem%5D.%5BRice%2C+paddy%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5419%5D%7D%2C+%7B%5BItem%5D.%5BWheat%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5419%5D%7D%2C+%7B%5BItem%5D.%5BMaize%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bm5419%5D%7D%2C+%7B%5BItem%5D.%5BRice%2C+paddy%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5419%5D%7D%2C+%7B%5BItem%5D.%5BWheat%5D%7D%29%2C+Union%28Crossjoin%28%7B%5BMeasures%5D.%5Bf5419%5D%7D%2C+%7B%5BItem%5D.%5BMaize%5D%7D%29%2C+Crossjoin%28%7B%5BMeasures%5D.%5Bf5419%5D%7D%2C+%7B%5BItem%5D.%5BRice%2C+paddy%5D%7D%29%29%29%29%29%29%29%29%29%29%29%29%29%29%29%29%29%29%29+ON+COLUMNS%2C%0D%0A%0D%0A++NON+EMPTY+Hierarchize%28Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2001%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2002%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2003%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2004%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2005%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2006%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2007%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2008%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2009%5D%7D%29%2C+Union%28Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2010%5D%7D%29%2C+Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2011%5D%7D%29%29%29%29%29%29%29%29%29%29%29%29+ON+ROWS%0D%0A%0D%0Afrom+%5BCROPS%5D&arguments=&callback=%2C+Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2011%5D%7D%29%29%29%29%29%29%29%29%29%29%29%29+ON+ROWS%0D%0A%0D%0Afrom+%5BCROPS%5D&mime=application%2Fjson&arguments=&callback=");
					// extract data into the map
					Map<String,Map<String, String>> dwSource=DWJSONUtils.loadSource(json,getProp().getProperty("dw_pk"));
					// actually merge data producing output
					DWJSONUtils.writeDWFeatureCollection(collection,dwSource,getProp().getProperty("pg_pk"),true, sw);
					
					return sw.toString();

				} finally {
					if (dataStore != null)
						dataStore.dispose();
					IOUtils.closeQuietly(sw);
				}

			default:
				
				return super.getData(params);

			}

	}

	/**
	 * describe
	 */
	@Override
	@SuppressWarnings("unchecked")
	public SimpleFeatureType getSimpleFeatureType(String layerName)
			throws Exception {

			final SimpleFeatureType schema = super.getSimpleFeatureType(layerName);
			final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
			b.init(schema);
			b.setName(schema.getName());
			b.add("value", String.class ); 
			return b.buildFeatureType();

	}

	@Override
	public SimpleFeatureType[] getAllSchemas() throws Exception {
		SimpleFeatureType[] schemas = super.getAllSchemas();
		int size = schemas.length;
		for (int i = 0; i < size; i++) {
			try {
				schemas[i] = getSimpleFeatureType(schemas[i].getTypeName());
			} catch (Exception ioe) {
				if (!IGNORE_NON_FATAL_ERRORS)
					throw ioe;
			}
		}
		return schemas;
		
	}

	public SimpleFeatureType getSchema(final String typeName) throws Exception {
		try {
			return getSimpleFeatureType(typeName);
		} catch (Exception ioe) {
			if (!IGNORE_NON_FATAL_ERRORS)
				throw ioe;
			return null;
		}
	}

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
