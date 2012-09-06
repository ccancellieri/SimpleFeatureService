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
import it.geosolutions.sfs.controller.SFSSettingLoader;
import it.geosolutions.sfs.controller.SFSParamsModel.ModeType;
import it.geosolutions.sfs.data.FeatureFactory;
import it.geosolutions.sfs.data.PropertiesUtils;
import it.geosolutions.sfs.utils.GTTools;
import it.geosolutions.sfs.utils.JSONUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceException;

import org.apache.commons.io.IOUtils;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 * 
 */
public class PGisFeatureFactory extends SFSSettingLoader implements FeatureFactory {

    protected org.slf4j.Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    // DataStore primary key
//    private final String dsPk;

    public PGisFeatureFactory(final String name, final Properties props, final boolean noGeom) throws Exception {
        super(name,props,noGeom);

//        dsPk = getProp().getProperty("ds_pk");
//        if (dsPk == null)
//            throw new IllegalArgumentException("The primary key cannot be null");
//
//        if (!ArrayUtils.contains(getAttrs(), dsPk))
//            throw new IllegalArgumentException(
//                    "The primary key should be present into the attribute list");
    }


    /**
     * @see {@link FeatureFactory#setQueryParams(String, String, boolean, String[], Integer, Integer, String[], SortOrder[], String, String, Double, String, String, String, String[], ModeType, String, HttpServletRequest)}
     */
    @Override
    public void writeData(SFSParamsModel params, HttpServletResponse response) throws Exception {

        OutputStream os = null;
        OutputStreamWriter osw = null;
        Writer w = null;
        DataStore dataStore = null;
        try {
            os = response.getOutputStream();
            osw = new OutputStreamWriter(os);
            w = new BufferedWriter(osw);

            dataStore = DataStoreUtils.getDataStore(getProp());

            SimpleFeatureType schema = getSimpleFeatureType(params.getLayerName());

            final Query query = GTTools.buildQuery(params.getRequest().getParameterMap(),
                    params.getAttrs(), params.getFid(), params.getQueryable(), params.getCrs(),
                    params.getOrderBy(), params.getDirections(), params.isNoGeom(),
                    params.getGeometry(), params.getTolerance(), params.getBbox(), params.getLon(),
                    params.getLat(), params.getOffset(), params.getLimit(), schema);

            final SimpleFeatureSource featureSource = dataStore.getFeatureSource(params
                    .getLayerName());

            switch (params.getMode()) {
            case bounds:
                // FeatureSource.getFeatures(Query/Filter)
                // /data/layername?mode=features&...
                // Should take the filter and split it into two parts, the
                // one natively supported, and the one that is
                // performed later in memory. Also, it should pass
                // down the view params contained in the
                // Hints.VIRTUAL_TABLE_PARAMETERS hint.

//                w.write(it.geosolutions.sfs.utils.JSONUtils.getBB(
//                        GTTools.getBB(featureSource, query)).toJSONString());
                it.geosolutions.sfs.utils.JSONUtils.writeBB(GTTools.getBB(featureSource, query),w);
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

    /**
     * 
     * @param resource
     * @return SimpleFeatureType
     * @throws Exception
     */
    public SimpleFeatureType getSimpleFeatureType(String resource) throws Exception {

        DataStore dataStore = null;
        try {
            dataStore = DataStoreUtils.getDataStore(getProp());

            GeometryDescriptor geomDesc = getGeometryDescriptor(dataStore, resource);

            return getSimpleFeatureType(resource, geomDesc, dataStore);

        } finally {
            if (dataStore != null)
                dataStore.dispose();
        }

    }

    /**
     * @see {@link SFSController#getCapabilities(javax.servlet.http.HttpServletRequest)}
     * @return an array containing all the available schemas for this store (should have the same size of the one returned by the
     *         {@link FeatureFactory#getAllReferencedEnvelopes()}
     * @throws Exception
     */
    @Override
    public void writeCapabilities(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        OutputStream os = null;
        OutputStreamWriter osw = null;
        Writer w = null;
        DataStore dataStore = null;
        try {
            os = response.getOutputStream();
            osw = new OutputStreamWriter(os);
            w = new BufferedWriter(osw);

            List<SimpleFeatureType> schemaList = new ArrayList<SimpleFeatureType>();
            List<ReferencedEnvelope> envelopeList = new ArrayList<ReferencedEnvelope>();

            String[] resources = PropertiesUtils.getAllResources(getName(), getProp());

            dataStore = DataStoreUtils.getDataStore(getProp());

            for (String resource : resources) {

                GeometryDescriptor geomDesc = getGeometryDescriptor(dataStore, resource);

                SimpleFeatureType sft = getSimpleFeatureType(resource, geomDesc, dataStore);
                ReferencedEnvelope env = getReferencedEnvelope(resource, geomDesc, dataStore);
                if (env != null && sft != null) {
                    schemaList.add(sft);
                    envelopeList.add(env);
                }
            }

//            w.write(JSONUtils.getCapabilities(null, schemaList, envelopeList).toJSONString());
            JSONUtils.writeCapabilities(LOGGER, schemaList, envelopeList, w);

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
    public void writeDescribeLayer(String layerName,
            HttpServletResponse request, HttpServletResponse response) throws Exception {
        OutputStream os = null;
        OutputStreamWriter osw = null;
        Writer w = null;
        try {
            os = response.getOutputStream();
            osw = new OutputStreamWriter(os);
            w = new BufferedWriter(osw);
            JSONUtils.writeDescriptor(LOGGER, getSimpleFeatureType(layerName), w);
//            w.write(JSONUtils.getDescriptor(getSimpleFeatureType(layerName)).toJSONString());

            w.flush();

        } catch (Exception e) {
            WebServiceException wse = new WebServiceException(e.getLocalizedMessage(), e);
            throw wse;
        } finally {
            IOUtils.closeQuietly(w);
            IOUtils.closeQuietly(osw);
            IOUtils.closeQuietly(os);
        }

    }

    /**
     * @see {@link SFSController#getCapabilities(javax.servlet.http.HttpServletRequest)}
     * @return an array containing all the available schemas for this store (should have the same size of the one returned by the
     *         {@link FeatureFactory#getAllSchemas()}
     * @throws Exception
     */
    public ReferencedEnvelope getReferencedEnvelope(String resourceName,
            GeometryDescriptor geomDesc, DataStore dataStore) throws Exception {

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
     * This is to avoid disposing the datastore each time we get a simpleFeatureType as the {@link PGisFeatureFactory#getSimpleFeatureType(String)}
     * do.
     * 
     * @see {@link PGisFeatureFactory#getData(SFSParamsModel)}
     * 
     * @param typeName
     * @param dataStore an opened datastore
     * @return the requested simplefeaturetype not disposing the passed datastore
     * @throws Exception
     */
    protected SimpleFeatureType getSimpleFeatureType(final String typeName,
            GeometryDescriptor geomDesc, final DataStore dataStore) throws Exception {

        SimpleFeatureType schema = dataStore.getSchema(typeName);

        final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();

        b.add(geomDesc);
        b.setDefaultGeometry(geomDesc.getLocalName());

        for (String name : getAttrsByResource(typeName)) {
            AttributeDescriptor ad = schema.getDescriptor(name);
            if (ad != null) {
                b.add(ad);
            } else {
                LOGGER.warn("Attribute called:"+name+" not found in schema:"+typeName);
            }
        }
        // for (AttributeDescriptor ad: schema.getAttributeDescriptors()){
        // if (!ad.equals(geomDesc))
        // b.add(ad);
        // }
        b.setName(schema.getName());
        return b.buildFeatureType();

    }

    protected GeometryDescriptor getGeometryDescriptor(DataStore dataStore, String resource)
            throws IOException {
        SimpleFeatureType schema = dataStore.getSchema(resource);
        String geometryName = PropertiesUtils.getGeometryByResource(getName(), getProp(),
                resource);

        GeometryDescriptor geomDesc = null;
        if (geometryName == null)
            geomDesc = schema.getGeometryDescriptor();
        else
            geomDesc = GTTools.getGeometryDescriptor(schema, geometryName);

        return geomDesc;
    }

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
