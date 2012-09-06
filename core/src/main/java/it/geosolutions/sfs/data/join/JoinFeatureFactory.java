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

import it.geosolutions.sfs.controller.SFSParamsModel;
import it.geosolutions.sfs.data.FeatureFactory;
import it.geosolutions.sfs.data.PropertiesUtils;
import it.geosolutions.sfs.data.dw.DWFeatureFactory;
import it.geosolutions.sfs.data.postgis.DataStoreUtils;
import it.geosolutions.sfs.data.postgis.PGisFeatureFactory;
import it.geosolutions.sfs.utils.GTTools;
import it.geosolutions.sfs.utils.JSONUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.WebServiceException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

/**
 * 
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 * 
 */
public class JoinFeatureFactory implements FeatureFactory {
    
    private Logger LOGGER=LoggerFactory.getLogger(JoinFeatureFactory.class);

    // DataStore primary key
    private final String dsPk;

    private final String dwPk;

    private final DWFeatureFactory dwFF;

    private final PGisFeatureFactory dsFF;

    // TODO how to map a layer to a resource ???
    private final String dwResource;

    private final String dsResource;

    public JoinFeatureFactory(final Properties dsProp, final Properties dwProp) throws Exception {

        dsFF = new PGisFeatureFactory(PGisFeatureFactory.class.getSimpleName(), dsProp, false);
        dsPk = dsProp.getProperty("ds_pk");
        if (dsPk == null)
            throw new IllegalArgumentException("The DS primary key cannot be null");
        
        for (String[] attrs : dsFF.getAttrs().values()) { // TODO more flexible here!!!
            if (!ArrayUtils.contains(attrs, dsPk))
                throw new IllegalArgumentException(
                        "The primary key should be present into the attribute list");
        }
        
        dwFF = new DWFeatureFactory(DWFeatureFactory.class.getSimpleName(), dwProp, true);
        dwPk = dwProp.getProperty("dw_pk");
        if (dwPk == null)
            throw new IllegalArgumentException("The DW primary key cannot be null");
        
        for (String[] attrs : dwFF.getAttrs().values()) { // TODO more flexible here!!!
            if (!ArrayUtils.contains(attrs, dwPk))
                throw new IllegalArgumentException(
                        "The primary key should be present into the append attribute list");
        }

        // TODO how to map a layer to a resource ???
        dwResource = dwFF.getResources()[0];
        dsResource = dsFF.getResources()[0];
    }

    
    @Override
    public void writeData(SFSParamsModel params, HttpServletResponse response) throws Exception {

        switch (params.getMode()) {
        // FeatureSource.getFeatures(Query/Filter)
        // /data/layername?mode=features&...
        // Should take the filter and split it into two parts, the
        // one natively supported, and the one that is
        // performed later in memory. Also, it should pass
        // down the view params contained in the
        // Hints.VIRTUAL_TABLE_PARAMETERS hint.
        case features:
            OutputStream os = null;
            OutputStreamWriter osw = null;
            Writer w = null;
            DataStore dataStore = null;
            try {

                // //////////////////////////
                // writer for result
                os = response.getOutputStream();
                osw = new OutputStreamWriter(os);
                w = new BufferedWriter(osw);

                // //////////////////////////
                // GET DataStore
                dataStore = DataStoreUtils.getDataStore(dsFF.getProp());

                SimpleFeatureType dsSchema = dsFF.getSimpleFeatureType(dsResource);

                GeometryDescriptor geomDesc = null;
                String geomName = PropertiesUtils.getGeometryByResource(dsFF.getName(),
                        dsFF.getProp(), dsResource);
                if (geomName == null)
                    geomDesc = dsSchema.getGeometryDescriptor();
                else
                    geomDesc = GTTools.getGeometryDescriptor(dsSchema, geomName);

                // //////////////////////////////////
                // prepare query to PostGis DataStore
                // List<String> reqHints=parseHintsParams(params, this.attrsAppend);

                if (!ArrayUtils.isEmpty(params.getAttrs())) {
                    // check if DataStore primary key is included into the request
                    boolean includePK = ArrayUtils.contains(params.getAttrs(), dsPk);
                    // we have to force including the attribute used to merge data with DW
                    if (!includePK) {
                        params.setAttrs((String[]) ArrayUtils.add(params.getAttrs(), dsPk));
                    }

                    SimpleFeatureCollection collection = getFilteredFeatureCollection(
                            params,
                            SFSParamsModel.purgeAttrs(dsFF.getAttrsByResource(dsResource),
                                    params.getAttrs()).toArray(new String[] {}), dataStore,
                            dsSchema, geomDesc);

                    // ////////////////////////////////////////
                    // GET DATA FROM DataWarehouse
                    // dwFF.dwSource;

                    // /////////////////////////////////////
                    // actually merge data producing output
                    String resource = dsResource;
                    JoinJSONUtils.writeDWFeatureCollection(
                            mergeSchema(dsSchema, dsResource, dsFF.getAttrsByResource(resource),
                                    dwFF.getAttrsByResource(resource),
                                    dwFF.getClassesByResource(resource)), collection,
                            dwFF.dwSource, includePK, dsPk, true, w);

                } else {
                    // query does not require DataWarehouse since no value attribute is specified

                    SimpleFeatureCollection collection = getFilteredFeatureCollection(
                            params,
                            SFSParamsModel.purgeAttrs(dsFF.getAttrsByResource(dsResource),
                                    params.getAttrs()).toArray(new String[] {}), dataStore,
                            dsSchema, geomDesc);
                    // /////////////////////////////////////
                    // actually producing output
                    JoinJSONUtils.writeDWFeatureCollection(dsSchema, collection, true, w);
                    // JSONUtils.writeFeatureCollection(collection, true, w);

                }

                w.flush();
            } finally {
                if (dataStore != null)
                    dataStore.dispose();
                IOUtils.closeQuietly(os);
                IOUtils.closeQuietly(osw);
                IOUtils.closeQuietly(w);
            }

            break;
        case count:
        case bounds:
        default:
            params.setAttrs(SFSParamsModel.purgeAttrs(dsFF.getAttrsByResource(dsResource),
                    params.getAttrs()).toArray(new String[] {}));
            dsFF.writeData(params, response);
            break;
        }
    }

    private static SimpleFeatureCollection getFilteredFeatureCollection(SFSParamsModel params,
            String[] attrs, DataStore dataStore, SimpleFeatureType schema,
            GeometryDescriptor geomDesc) throws IOException {

        final Query query = GTTools.buildQuery(params.getRequest().getParameterMap(), attrs,
                params.getFid(), params.getQueryable(), params.getCrs(), params.getOrderBy(),
                params.getDirections(), params.isNoGeom(), params.getGeometry(),
                params.getTolerance(), params.getBbox(), params.getLon(), params.getLat(),
                params.getOffset(), params.getLimit(), schema);

        // //////////////////////////
        // GET DATA FROM POSTGIS
        final SimpleFeatureSource featureSource = dataStore.getFeatureSource(params.getLayerName());

        return featureSource.getFeatures(query);
    }

    private static SimpleFeatureType mergeSchema(SimpleFeatureType schema, String typeName,
            String[] attrs, String[] appendAttrs, Class<?>[] appendClass) {

        final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.init(schema);
        // for (AttributeDescriptor ad:schema.getAttributeDescriptors()){
        // String name=ad.getLocalName();
        // if (ArrayUtils.contains(getAttrs(),name)){
        // b.add(ad);
        // } // TODO else LOG
        // }
        if (appendClass.length == appendAttrs.length) {
            for (int i = 0; i < appendAttrs.length; i++) {
                b.add(appendAttrs[i], appendClass[i]);
            }
        } // TODO else LOG
          // b.setDefaultGeometry(getGeometryName());
        b.setName(typeName);
        // b.add(VALUE, String.class);
        return b.buildFeatureType();
    }

//    @Cacheable(value = { "cacheManager" }, key="typeName", condition = "cached==true")
    public SimpleFeatureType getSimpleFeatureType(final String typeName, final boolean cached) throws Exception {
        return mergeSchema(dsFF.getSimpleFeatureType(dsResource), dsResource,
                dsFF.getAttrsByResource(dsResource), dwFF.getAttrsByResource(dwResource),
                dwFF.getClassesByResource(dwResource));
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
            JSONUtils.writeDescriptor(LOGGER, getSimpleFeatureType(layerName,true), w);
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

    @Override
    public void writeCapabilities(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        dsFF.writeCapabilities(request, response);
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
