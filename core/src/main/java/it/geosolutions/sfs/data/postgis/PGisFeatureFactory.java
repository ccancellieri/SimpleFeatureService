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

import it.geosolutions.sfs.controller.SFSController;
import it.geosolutions.sfs.controller.SFSParamsModel;
import it.geosolutions.sfs.controller.SFSParamsModel.ModeType;
import it.geosolutions.sfs.data.FeatureFactory;
import it.geosolutions.sfs.utils.GTTools;
import it.geosolutions.sfs.utils.JSONUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang.ArrayUtils;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.ows.Request;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;

/**
 * 
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 * 
 */
public class PGisFeatureFactory extends FeatureFactory {

	private final Properties prop;

	public final static String IGNORE_NON_FATAL_ERRORS_KEY = "ignoreNonFatalErrors";
	protected final boolean IGNORE_NON_FATAL_ERRORS;

	public PGisFeatureFactory(final Properties prop) throws Exception {
		this.prop = prop;

		IGNORE_NON_FATAL_ERRORS = prop.getProperty(IGNORE_NON_FATAL_ERRORS_KEY,
				"false").equalsIgnoreCase("false") ? false : true;

	}

	public Properties getProp() {
		return prop;
	}

	/**
	 * @see {@link FeatureFactory#setQueryParams(String, String, boolean, String[], Integer, Integer, String[], SortOrder[], String, String, Double, String, String, String, String[], ModeType, String, HttpServletRequest)}
	 */
	@Override
	public void writeData(SFSParamsModel params, HttpServletResponse response)
			throws Exception {

		OutputStream os = null;
		OutputStreamWriter osw = null;
		Writer w = null;
		DataStore dataStore = null;
		try {
			os = response.getOutputStream();
			osw = new OutputStreamWriter(os);
			w = new BufferedWriter(osw);

			dataStore = DataStoreUtils.getDataStore(prop);

			SimpleFeatureType schema = getSimpleFeatureType(params.getLayerName());

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

			switch (params.getMode()) {
			case bounds:
				// FeatureSource.getFeatures(Query/Filter)
				// /data/layername?mode=features&...
				// Should take the filter and split it into two parts, the
				// one natively supported, and the one that is
				// performed later in memory. Also, it should pass
				// down the view params contained in the
				// Hints.VIRTUAL_TABLE_PARAMETERS hint.

				w.write(it.geosolutions.sfs.utils.JSONUtils.getBB(
						GTTools.getBB(featureSource, query)).toJSONString());
				break;
			case features:
				// FeatureSource.getFeatures(Query/Filter)
				// /data/layername?mode=features&...
				// Should take the filter and split it into two parts, the
				// one natively supported, and the one that is
				// performed later in memory. Also, it should pass
				// down the view params contained in the
				// Hints.VIRTUAL_TABLE_PARAMETERS hint.
				it.geosolutions.sfs.utils.JSONUtils.writeFeatureCollection(
						featureSource.getFeatures(query), true, w);
				break;
			case count:
				// FeatureSoruce.getCount(Query)
				// /data/layername?mode=count&...
				// Should also pass down the hints
				w.write(GTTools.getCount(featureSource, query).toString());
				break;
			}

			w.flush();

		} finally {
			if (dataStore != null)
				dataStore.dispose();
			IOUtils.closeQuietly(os);
			IOUtils.closeQuietly(osw);
			IOUtils.closeQuietly(w);
		}

	}

	@Override
	public SimpleFeatureType getSimpleFeatureType(String resource)
			throws Exception {

		DataStore dataStore = null;
		try {
			dataStore = DataStoreUtils.getDataStore(prop);

			GeometryDescriptor geomDesc = getGeometryDescriptor(dataStore, resource);
			
			return getSimpleFeatureType(resource, geomDesc, dataStore);

		} finally {
			if (dataStore != null)
				dataStore.dispose();
		}

	}


	/**
	 * @see {@link SFSController#getCapabilities(javax.servlet.http.HttpServletRequest)}
	 * @return an array containing all the available schemas for this store
	 *         (should have the same size of the one returned by the
	 *         {@link FeatureFactory#getAllReferencedEnvelopes()}
	 * @throws Exception
	 */
	@Override
	public void writeCapabilities(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		OutputStream os = null;
		OutputStreamWriter osw = null;
		Writer w = null;
		DataStore dataStore = null;
		try {
			os = response.getOutputStream();
			osw = new OutputStreamWriter(os);
			w = new BufferedWriter(osw);
			
			List<SimpleFeatureType> schemaList = new ArrayList<SimpleFeatureType>();
			List<ReferencedEnvelope> envelopeList=new ArrayList<ReferencedEnvelope>();
			
			

			String[] resources=DataStoreUtils.getAllResources(this.getClass(), prop);
			
			dataStore = DataStoreUtils.getDataStore(prop);
			
			for (String resource:resources){
				
				GeometryDescriptor geomDesc = getGeometryDescriptor(dataStore, resource);
				
				SimpleFeatureType sft=getSimpleFeatureType(resource, geomDesc, dataStore);
				ReferencedEnvelope env=getReferencedEnvelope(resource, geomDesc, dataStore);
				if (env!=null && sft!=null){
					schemaList.add(sft);
					envelopeList.add(env);
				}
			}
			boolean add=false;
			
			w.write(JSONUtils.writeCapabilities(null,schemaList,envelopeList).toJSONString());

			w.flush();

		} finally {
			if (dataStore != null)
				dataStore.dispose();
			IOUtils.closeQuietly(os);
			IOUtils.closeQuietly(osw);
			IOUtils.closeQuietly(w);
		}

	}

	/**
	 * @see {@link SFSController#getCapabilities(javax.servlet.http.HttpServletRequest)}
	 * @return an array containing all the available schemas for this store
	 *         (should have the same size of the one returned by the
	 *         {@link FeatureFactory#getAllSchemas()}
	 * @throws Exception
	 */
	public ReferencedEnvelope getReferencedEnvelope(String resourceName, GeometryDescriptor geomDesc, DataStore dataStore)
			throws Exception {

			Query query = null;
			if (geomDesc != null)
				query = new Query(resourceName, Filter.INCLUDE,
						new String[] { geomDesc.getLocalName() });
			else
				query = new Query();
			return dataStore.getFeatureSource(resourceName).getBounds(query);
	}

	/**
	 * This method is provided to be overridden.<BR>
	 * This is to avoid disposing the datastore each time we get a
	 * simpleFeatureType as the
	 * {@link PGisFeatureFactory#getSimpleFeatureType(String)} do.
	 * 
	 * @see {@link PGisFeatureFactory#getData(SFSParamsModel)}
	 * 
	 * @param typeName
	 * @param dataStore
	 *            an opened datastore
	 * @return the requested simplefeaturetype not disposing the passed
	 *         datastore
	 * @throws Exception
	 */
	protected SimpleFeatureType getSimpleFeatureType(final String typeName, GeometryDescriptor geomDesc,
			final DataStore dataStore) throws Exception {
		
			SimpleFeatureType schema = dataStore.getSchema(typeName);

			final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();

			b.add(GTTools.getGeometryDescriptor(schema, geomDesc.getLocalName()));
			b.setDefaultGeometry(geomDesc.getLocalName());
			for (AttributeDescriptor ad: schema.getAttributeDescriptors()){
				if (!ad.equals(geomDesc))
					b.add(ad);
			}
			
			b.setName(schema.getName());
			return b.buildFeatureType();

	}
	
	protected GeometryDescriptor getGeometryDescriptor(DataStore dataStore, String resource) throws IOException{
		SimpleFeatureType schema = dataStore.getSchema(resource);
		String geometryName = DataStoreUtils.getGeometryForResource(this.getClass(), prop, resource);
		
		GeometryDescriptor geomDesc = null;
		if (geometryName == null)
			geomDesc = schema.getGeometryDescriptor();
		else
			geomDesc = GTTools.getGeometryDescriptor(schema, geometryName);
		
		return geomDesc;
	}

	// public ReferencedEnvelope getReferencedEnvelope(final String typeName)
	// throws Exception {
	// DataStore dataStore = null;
	// try {
	// dataStore = DataStoreUtils.getDataStore(prop);
	// return dataStore.getFeatureSource(typeName).getBounds();
	// } catch (Exception ioe){
	// if (!IGNORE_NON_FATAL_ERRORS)
	// throw ioe;
	// return null;
	// } finally {
	// if (dataStore != null)
	// dataStore.dispose();
	// }
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
