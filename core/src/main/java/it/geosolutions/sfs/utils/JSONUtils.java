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
package it.geosolutions.sfs.utils;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.util.JSONBuilder;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.json.simple.JSONArray;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * 
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 * 
 */
public abstract class JSONUtils {

//    public static JSONArray getDescriptor(SimpleFeatureType schema) throws Exception {
//        if (schema == null)
//            throw new IllegalArgumentException("Unable to getDescriptor using null argument");
//        JSONArray array = new JSONArray();
//
//        Map<String, String> attributes = new LinkedHashMap<String, String>();
//        for (AttributeDescriptor att : schema.getAttributeDescriptors()) {
//            attributes.put(att.getLocalName(), findAttributeType(att));
//        }
//        array.add(attributes);
//        return array;
//    }

    public static void writeDescriptor(Logger logger, SimpleFeatureType schema, Writer w)
            throws IllegalArgumentException {
        if (schema == null || w == null)
            throw new IllegalArgumentException("Unable to writeDescriptor using null arguments");

        final JSONBuilder builder = new JSONBuilder(w);
        builder.array();
        builder.object();
        for (AttributeDescriptor att : schema.getAttributeDescriptors()) {

            builder.key(att.getLocalName());
            builder.value(findAttributeType(att));

        }
        builder.endObject();
        builder.endArray();
    }

    private static String findAttributeType(AttributeDescriptor att) {
        Class binding = att.getType().getBinding();
        if (Geometry.class.isAssignableFrom(binding)) {
            return findGeometryType(binding);
        } else if (Number.class.isAssignableFrom(binding)) {
            return "number";
        } else if (Date.class.isAssignableFrom(binding)) {
            return "timestamp";
        } else if (Boolean.class.isAssignableFrom(binding)) {
            return "boolean";
        } else {
            return "string";
        }
    }

    private static String findGeometryType(Class binding) {
        if (GeometryCollection.class.isAssignableFrom(binding)) {
            if (MultiPoint.class.isAssignableFrom(binding)) {
                return "MultiPoint";
            } else if (MultiPolygon.class.isAssignableFrom(binding)) {
                return "MultiPolygon";
            } else if (MultiLineString.class.isAssignableFrom(binding)) {
                return "MultiLineString";
            } else {
                return "GeometryCollection";
            }
        } else {
            if (Point.class.isAssignableFrom(binding)) {
                return "Point";
            } else if (Polygon.class.isAssignableFrom(binding)) {
                return "Polygon";
            } else if (LineString.class.isAssignableFrom(binding)) {
                return "LineString";
            } else {
                return "Geometry";
            }
        }
    }

    public static void writeCapabilities(Logger logger, List<SimpleFeatureType> schemas,
            List<ReferencedEnvelope> envelopes, Writer w) throws Exception {
        if (schemas == null || envelopes == null || w == null)
            throw new IllegalArgumentException("Unable to writeCapabilities using null arguments");
        if (schemas.size() != envelopes.size())
            throw new IllegalArgumentException(
                    "Unable to writeCapabilities using different in length arrays");
        JSONBuilder builder = new JSONBuilder(w);
        builder.array();
        for (int i = 0; i < schemas.size(); i++) {
            final SimpleFeatureType schema = schemas.get(i);
            final ReferencedEnvelope envelope = envelopes.get(i);

            if (schema == null || envelope == null)
                throw new IllegalArgumentException("Unable to writeDescriptor using null arguments");
            try {

                builder.object();

                builder.key("name");
                builder.value(schema.getName().getLocalPart());
                
//                    builder.key("bbox");
                // writeBB(envelope);
                writeBB(envelope,builder);

                CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
                if (crs != null) {
                    builder.key("crs");
                    builder.value("urn:ogc:def:crs:EPSG:" + CRS.lookupEpsgCode(crs, false));
                } else
                    throw (new IOException("Failed to get the resource crs:" + schema.getName()
                            + " bb: " + envelope.toString()));

                builder.key("axisorder");
                builder.value("xy");

            } catch (Exception e) {
                if (logger != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(e.getLocalizedMessage(), e);
                    } else if (logger.isErrorEnabled()) {
                        logger.error(e.getLocalizedMessage());
                    }
                }
            } finally {
                builder.endObject();
            }
        }
        builder.endArray();
    }

    public static JSONArray getCapabilities(Logger logger, List<SimpleFeatureType> schemas,
            List<ReferencedEnvelope> envelopes) throws Exception {
        if (schemas == null || envelopes == null)
            throw new IllegalArgumentException("Unable to getCapabilities using null arguments");
        if (schemas.size() != envelopes.size())
            throw new IllegalArgumentException(
                    "Unable to getCapabilities using different in length arrays");

        JSONArray array = new JSONArray();
        for (int i = 0; i < schemas.size(); i++) {
            final SimpleFeatureType schema = schemas.get(i);
            final ReferencedEnvelope envelope = envelopes.get(i);
            try {
                array.add(toJSON(schema, envelope));
            } catch (Exception e) {
                if (logger != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(e.getLocalizedMessage(), e);
                    } else if (logger.isErrorEnabled()) {
                        logger.error(e.getLocalizedMessage());
                    }
                }
            }
        }
        return array;
    }

    /**
     * 
     * Maps a layer info to the capabilities json structure. Using a linked hash map under covers to preserve the order of the attributes
     * 
     * @param layerInfo
     * @param envelope
     * @return map
     * @throws IOException
     * @throws IllegalArgumentException
     */
    protected static Map<String, Object> toJSON(SimpleFeatureType layerInfo,
            ReferencedEnvelope envelope) throws IOException, IllegalArgumentException {
        if (layerInfo == null || envelope == null)
            throw new IllegalArgumentException("Unable to getDescriptor using null arguments");
        try {
            Map<String, Object> json = new LinkedHashMap<String, Object>();
            json.put("name", layerInfo.getName().getLocalPart());

            try {
                json.put("bbox", getBB(envelope));
            } catch (Exception e) {
                throw ((IOException) new IOException("Failed to get the resource bounding box of:"
                        + layerInfo.getName()).initCause(e));
            }

            CoordinateReferenceSystem crs = layerInfo.getCoordinateReferenceSystem();
            if (crs != null)
                json.put("crs", "urn:ogc:def:crs:EPSG:" + CRS.lookupEpsgCode(crs, false));
            else
                throw (new IOException("Failed to get the resource crs:" + layerInfo.getName()
                        + " bb: " + envelope.toString()));

            json.put("axisorder", "xy");

            return json;
        } catch (FactoryException e) {
            throw ((IOException) new IOException("Failed to lookup the EPSG code").initCause(e));
        }
    }

    /**
     * 
     * @param featureCollection
     * @param featureBounding Generate bounds for every feature?
     * @param outWriter
     * @throws Exception
     */
    public static void writeFeatureCollection(SimpleFeatureCollection featureCollection,
            boolean featureBounding, Writer outWriter) throws Exception {
        final FeatureJSON json = new FeatureJSON();
        boolean geometryless = featureCollection.getSchema().getGeometryDescriptor() == null;
        json.setEncodeFeatureCollectionBounds(!geometryless);
        json.setEncodeFeatureCollectionCRS(!geometryless);
        json.writeFeatureCollection(featureCollection, outWriter);
    }

    private static JSONArray getBB(BoundingBox env) throws Exception {
        JSONArray array = new JSONArray();
        array.add(env.getMinX());
        array.add(env.getMinY());
        array.add(env.getMaxX());
        array.add(env.getMaxY());
        return array;
    }

    public static void writeBB(BoundingBox env, Writer w) throws Exception {
        JSONBuilder builder = new JSONBuilder(w);
        builder.key("bbox");
        builder.array();
        builder.value(env.getMinX());
        builder.value(env.getMinY());
        builder.value(env.getMaxX());
        builder.value(env.getMaxY());
        builder.endArray();
    }
    
    public static void writeBB(BoundingBox env, JSONBuilder builder) throws Exception {
        builder.key("bbox");
        builder.array();
        builder.value(env.getMinX());
        builder.value(env.getMinY());
        builder.value(env.getMaxX());
        builder.value(env.getMaxY());
        builder.endArray();
    }


}
