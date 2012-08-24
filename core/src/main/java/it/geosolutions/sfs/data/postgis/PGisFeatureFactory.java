package it.geosolutions.sfs.data.postgis;

import it.geosolutions.sfs.controller.SFSParamsModel;
import it.geosolutions.sfs.controller.SFSParamsModel.ModeType;
import it.geosolutions.sfs.data.FeatureFactory;
import it.geosolutions.sfs.utils.GTTools;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.sort.SortOrder;

public class PGisFeatureFactory extends FeatureFactory {

	private final Properties prop;
	
	private final static String IGNORE_NON_FATAL_ERRORS_KEY="ignoreNonFatalErrors";
	private final boolean IGNORE_NON_FATAL_ERRORS;
	
	public PGisFeatureFactory(final Properties prop) throws Exception {
		this.prop = prop;
		
		IGNORE_NON_FATAL_ERRORS=prop.getProperty(IGNORE_NON_FATAL_ERRORS_KEY,"false").equalsIgnoreCase("false")?false:true;
		
		
	}

	/**
	 *@see {@link FeatureFactory#setQueryParams(String, String, boolean, String[], Integer, Integer, String[], SortOrder[], String, String, Double, String, String, String, String[], ModeType, String, HttpServletRequest)} 
	 */
	@Override
	public Object getData(SFSParamsModel params)
			throws Exception {
		StringWriter sw = null;
		DataStore dataStore = null;
		try {
			dataStore = DataStoreUtils.getDataStore(prop);

			SimpleFeatureType schema = dataStore.getSchema(params.getLayerName());
		
			final Query query = GTTools.buildQuery(params.getRequest().getParameterMap(), params.getAttrs(), params.getFid(),
					params.getQueryable(), params.getCrs(), params.getOrderBy(), params.getDirections(), params.isNoGeom(), params.getGeometry(),
					params.getTolerance(), params.getBbox(), params.getLon(), params.getLat(), params.getOffset(), params.getLimit(), schema);

			final SimpleFeatureSource featureSource = dataStore
			.getFeatureSource(params.getLayerName());
			
			sw = new StringWriter();

			switch (params.getMode()) {
			case bounds:
				// FeatureSource.getFeatures(Query/Filter)
				// /data/layername?mode=features&...
				// Should take the filter and split it into two parts, the
				// one natively supported, and the one that is
				// performed later in memory. Also, it should pass
				// down the view params contained in the
				// Hints.VIRTUAL_TABLE_PARAMETERS hint.

				return it.geosolutions.sfs.utils.JSONUtils.getBB(GTTools
						.getBB(featureSource, query));

			case features:
				// FeatureSource.getFeatures(Query/Filter)
				// /data/layername?mode=features&...
				// Should take the filter and split it into two parts, the
				// one natively supported, and the one that is
				// performed later in memory. Also, it should pass
				// down the view params contained in the
				// Hints.VIRTUAL_TABLE_PARAMETERS hint.
				it.geosolutions.sfs.utils.JSONUtils
						.writeFeatureCollection(
								featureSource.getFeatures(query),
								true, sw);
				return sw.toString();

			case count:
				// FeatureSoruce.getCount(Query)
				// /data/layername?mode=count&...
				// Should also pass down the hints
				return GTTools.getCount(featureSource, query);
				// GTTools.buildQuery(request, schema).getFilter());

			default:
				return "EMPTY"; // TODO

			}
		} finally {
			if (dataStore != null)
				dataStore.dispose();
			IOUtils.closeQuietly(sw);
		}

	}

	@Override
	@SuppressWarnings("unchecked")
	public SimpleFeatureType getSimpleFeatureType(String layerName)
			throws Exception {

		DataStore dataStore = null;
		try {
			dataStore = DataStoreUtils.getDataStore(prop);

			return dataStore.getSchema(layerName);

		} finally {
			if (dataStore != null)
				dataStore.dispose();
		}

	}

	@Override
	public ReferencedEnvelope[] getAllReferencedEnvelopes() throws Exception {
		DataStore dataStore = null;
		try {
			dataStore = DataStoreUtils.getDataStore(prop);
			String[] names = dataStore.getTypeNames();
			int size = names.length;
			ReferencedEnvelope[] envelopes = new ReferencedEnvelope[size];
			for (int i = 0; i < size; i++) {
				String typeName = names[i];
				try {
					envelopes[i] = dataStore.getFeatureSource(typeName).getBounds();		
				} catch (Exception ioe){
					if (!IGNORE_NON_FATAL_ERRORS)
						throw ioe;
				}
			}
			return envelopes;
		} finally {
			if (dataStore != null)
				dataStore.dispose();
		}
	}

	public ReferencedEnvelope getReferencedEnvelope(final String typeName)
			throws Exception {
		DataStore dataStore = null;
		try {
			dataStore = DataStoreUtils.getDataStore(prop);
			return dataStore.getFeatureSource(typeName).getBounds();		
		} catch (Exception ioe){
			if (!IGNORE_NON_FATAL_ERRORS)
				throw ioe;
			return null;
		} finally {
			if (dataStore != null)
				dataStore.dispose();
		}

	}

	@Override
	public SimpleFeatureType[] getAllSchemas() throws Exception {
		DataStore dataStore = null;
		try {
			dataStore = DataStoreUtils.getDataStore(prop);
			String[] names = dataStore.getTypeNames();
			int size = names.length;
			SimpleFeatureType[] schemas = new SimpleFeatureType[size];
			for (int i = 0; i < size; i++) {
				String typeName = names[i];
				try {
					schemas[i] = dataStore.getSchema(typeName);	
				} catch (Exception ioe){
					if (!IGNORE_NON_FATAL_ERRORS)
						throw ioe;
				}
			}
			return schemas;
		} finally {
			if (dataStore != null)
				dataStore.dispose();
		}
	}

	public SimpleFeatureType getSchema(final String typeName) throws Exception {
		DataStore dataStore = null;
		try {
			dataStore = DataStoreUtils.getDataStore(prop);
			return dataStore.getSchema(typeName);		
		} catch (Exception ioe){
			if (!IGNORE_NON_FATAL_ERRORS)
				throw ioe;
			return null;
		} finally {
			if (dataStore != null)
				dataStore.dispose();
		}
	}

//	/**
//	 * @param typeName
//	 * @throws Exception
//	 */
//	public BoundingBox getBoundingBox(final String typeName) throws Exception {
//		DataStore dataStore = null;
//		try {
//			dataStore = DataStoreUtils.getDataStore(prop);
//			GTTools.getBB(dataStore.getFeatureSource(typeName), query);
//		} finally {
//			if (dataStore != null)
//				dataStore.dispose();
//		}
//	}

//	/**
//	 * @deprecated model
//	 * @param typeName
//	 * @throws Exception
//	 */
//	public void get(final String typeName) throws Exception {
//		DataStore dataStore = null;
//		try {
//			dataStore = DataStoreUtils.getDataStore(prop);
//		} finally {
//			if (dataStore != null)
//				dataStore.dispose();
//		}
//	}
	

//	/**
//	 * {field}_{query_op}={value}: specify a filter expression, field must be in
//	 * the list of fields specified by queryable, supported query_op's are: eq:
//	 * equal to ne: not equal to lt: lower than lte: lower than or equal to gt:
//	 * greater than gte: greater than or equal to like ilike
//	 * @deprecated model
//	 */
//	private boolean checkQueryParams(HttpServletRequest request) {
//		Map<String, String> map = request.getParameterMap();// (HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
//		// TODO
//
//		// FAKE logic
//		if (map != null)
//			return true;
//		return false;
//	}
}
