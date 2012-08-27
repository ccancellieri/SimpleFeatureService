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
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.json.simple.JSONArray;
import org.opengis.feature.simple.SimpleFeature;
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
	
	public static JSONArray getDescriptor(SimpleFeatureType schema) throws Exception {
		if (schema==null)
			throw new IllegalArgumentException("Unable to getDescriptor using null argument");
		JSONArray array=new JSONArray();
		Map<String, String> attributes = new LinkedHashMap<String, String>();
	    for (AttributeDescriptor att : schema.getAttributeDescriptors()) {
	        attributes.put(att.getLocalName(), findAttributeType(att));
	    }
	    array.add(attributes);
	    return array;
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
	
	public static JSONArray writeCapabilities(Logger logger, SimpleFeatureType[] schemas, ReferencedEnvelope[] envelopes) throws Exception {
		if (schemas==null || envelopes==null)
			throw new IllegalArgumentException("Unable to getCapabilities using null arguments");
		if (schemas.length!=envelopes.length)
			throw new IllegalArgumentException("Unable to getCapabilities using different in length arrays");
		
		JSONArray array=new JSONArray();
		for (int i=0; i<schemas.length; i++){
			final SimpleFeatureType schema=schemas[i];
			final ReferencedEnvelope envelope=envelopes[i];
			try {
				array.add(toJSON(schema,envelope));
			} catch (Exception e){
				if (logger!=null){
					if (logger.isDebugEnabled()){
						logger.debug(e.getLocalizedMessage(),e);
					} else if (logger.isErrorEnabled()){
						logger.error(e.getLocalizedMessage());
					}
				}
			}
		}
        return array;
	}
	
	/**
	 * 
     * Maps a layer info to the capabilities json structure. Using a linked hash map under covers to
     * preserve the order of the attributes
     * 
	 * @param layerInfo
	 * @param envelope
	 * @return map
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
    protected static Map<String,Object> toJSON(SimpleFeatureType layerInfo, ReferencedEnvelope envelope) throws IOException,IllegalArgumentException {
    	if (layerInfo==null || envelope==null)
			throw new IllegalArgumentException("Unable to getDescriptor using null arguments");
        try {
            Map<String,Object> json = new LinkedHashMap<String,Object>();
            json.put("name", layerInfo.getName().getLocalPart());
            
            try {
                json.put("bbox", toJSON(envelope));
            } catch(Exception e) {
                throw ((IOException) new IOException("Failed to get the resource bounding box of:" + layerInfo.getName()).initCause(e));
            }
           
            CoordinateReferenceSystem crs=layerInfo.getCoordinateReferenceSystem();
            if (crs!=null)
            	json.put("crs", "urn:ogc:def:crs:EPSG:"+CRS.lookupEpsgCode(crs, false));
            else
                throw (new IOException("Failed to get the resource crs:" + layerInfo.getName() + " bb: "+envelope.toString()));
            
            json.put("axisorder", "xy");

            return json;
        } catch (FactoryException e) {
            throw ((IOException) new IOException("Failed to lookup the EPSG code").initCause(e));
        }
    }
    
   
	/**
     * Maps a layer info to the capabilities json structure. Using a linked hash map under covers to
     * preserve the order of the attributes
     * 
     * @param layerInfo
     * @return
     * @throws IOException
     * @deprecated unused
     */
    private static Map<String,Object> toJSON(SimpleFeature layerInfo) throws IOException {
        
        try {
            Map<String,Object> json = new LinkedHashMap<String,Object>();
            json.put("name", layerInfo.getName().getLocalPart());
            
            try {
                json.put("bbox", toJSON(layerInfo.getBounds()));
            } catch(Exception e) {
                throw ((IOException) new IOException("Failed to get the resource bounding box of:" + layerInfo.getName()).initCause(e));
            }
            json.put("crs", "urn:ogc:def:crs:EPSG:" + CRS.lookupEpsgCode(layerInfo.getBounds().getCoordinateReferenceSystem(), false));
            json.put("axisorder", "xy");

            return json;
        } catch (FactoryException e) {
            throw ((IOException) new IOException("Failed to lookup the EPSG code").initCause(e));
        }
    }
    

    /**
     * Maps a referenced envelope into a json bbox
     * 
     * @param bbox
     * @return
     */
    private static JSONArray toJSON(BoundingBox bbox) {
        JSONArray json = new JSONArray();
        json.add(bbox.getMinX());
        json.add(bbox.getMinY());
        json.add(bbox.getMaxX());
        json.add(bbox.getMaxY());
        return json;
    }
	
	/**
	 * 
	 * @param featureCollection
	 * @param featureBounding
	 *            Generate bounds for every feature?
	 * @param outWriter
	 * @throws Exception
	 */
	public static void writeFeatureCollection(
			SimpleFeatureCollection featureCollection, boolean featureBounding,
			Writer outWriter) throws Exception {
        final FeatureJSON json = new FeatureJSON();
        boolean geometryless = featureCollection.getSchema().getGeometryDescriptor() == null;
        json.setEncodeFeatureCollectionBounds(!geometryless);
        json.setEncodeFeatureCollectionCRS(!geometryless);
        json.writeFeatureCollection(featureCollection, outWriter);
//		
//        GeoJSONBuilder jsonWriter = new GeoJSONBuilder(outWriter);
//		//
//		boolean hasGeom = false;
//
//		try {
//			jsonWriter.object().key("type").value("FeatureCollection");
//			jsonWriter.key("features");
//			jsonWriter.array();
//
//			CoordinateReferenceSystem crs = null;
//			try {
//				SimpleFeatureType fType;
//				List<AttributeDescriptor> types;
//				FeatureIterator iterator = featureCollection.features();
//				while (iterator.hasNext()) {
//					SimpleFeature feature = (SimpleFeature) iterator.next();
//					jsonWriter.object();
//					jsonWriter.key("type").value("Feature");
//					jsonWriter.key("id").value(feature.getID());
//
//					fType = feature.getFeatureType();
//					types = fType.getAttributeDescriptors();
//
//					GeometryDescriptor defaultGeomType = fType
//							.getGeometryDescriptor();
//
//					if (crs == null && defaultGeomType != null)
//						crs = fType.getGeometryDescriptor()
//								.getCoordinateReferenceSystem();
//
//					jsonWriter.key("geometry");
//					Geometry aGeom = (Geometry) feature.getDefaultGeometry();
//
//					if (aGeom == null) {
//						// In case the default geometry is not set, we will
//						// just use the first geometry we find
//						for (int j = 0; j < types.size() && aGeom == null; j++) {
//							Object value = feature.getAttribute(j);
//							if (value != null && value instanceof Geometry) {
//								aGeom = (Geometry) value;
//							}
//						}
//					}
//					// Write the geometry, whether it is a null or not
//					if (aGeom != null) {
//						jsonWriter.writeGeom(aGeom);
//						hasGeom = true;
//					} else {
//						jsonWriter.value(null);
//					}
//					if (defaultGeomType != null)
//						jsonWriter.key("geometry_name").value(
//								defaultGeomType.getLocalName());
//
//					jsonWriter.key("properties");
//					jsonWriter.object();
//
//					for (int j = 0; j < types.size(); j++) {
//						Object value = feature.getAttribute(j);
//						AttributeDescriptor ad = types.get(j);
//
//						if (value != null) {
//							if (value instanceof Geometry) {
//								// This is an area of the spec where they
//								// decided to 'let convention evolve',
//								// that is how to handle multiple
//								// geometries. My take is to print the
//								// geometry here if it's not the default.
//								// If it's the default that you already
//								// printed above, so you don't need it here.
//								if (ad.equals(defaultGeomType)) {
//									// Do nothing, we wrote it above
//									// jsonWriter.value("geometry_name");
//								} else {
//									jsonWriter.key(ad.getLocalName());
//									jsonWriter.writeGeom((Geometry) value);
//								}
//							} else {
//								jsonWriter.key(ad.getLocalName());
//								jsonWriter.value(value);
//							}
//
//						} else {
//							jsonWriter.key(ad.getLocalName());
//							jsonWriter.value(null);
//						}
//					}
//					// Bounding box for feature in properties
//					ReferencedEnvelope refenv = new ReferencedEnvelope(
//							feature.getBounds());
//					if (featureBounding && !refenv.isEmpty())
//						jsonWriter.writeBoundingBox(refenv);
//
//					jsonWriter.endObject(); // end the properties
//					jsonWriter.endObject(); // end the feature
//				}
//			} // catch an exception here?
//			finally {
//				// featureCollection.close(iterator);
//			}
//
//			jsonWriter.endArray(); // end features
//
//			// Coordinate Referense System, currently only if the namespace is
//			// EPSG
//			if (crs != null) {
//				Set<ReferenceIdentifier> ids = crs.getIdentifiers();
//				// WKT defined crs might not have identifiers at all
//				if (ids != null && ids.size() > 0) {
//					NamedIdentifier namedIdent = (NamedIdentifier) ids
//							.iterator().next();
//					String csStr = namedIdent.getCodeSpace().toUpperCase();
//
//					if (csStr.equals("EPSG")) {
//						jsonWriter.key("crs");
//						jsonWriter.object();
//						jsonWriter.key("type").value(csStr);
//						jsonWriter.key("properties");
//						jsonWriter.object();
//						jsonWriter.key("code");
//						jsonWriter.value(namedIdent.getCode());
//						jsonWriter.endObject(); // end properties
//						jsonWriter.endObject(); // end crs
//					}
//				}
//			}
//
//			// Bounding box for featurecollection
//			if (hasGeom && featureBounding) {
//				ReferencedEnvelope e = featureCollection.getBounds();
//
//				if (e == null) {
//					throw new Exception(
//							"Unable to get Envelope for featurecollection"); // TODO
//																				// throw
//																				// a
//																				// more
//																				// specific
//																				// exception
//				} else {
//					jsonWriter.writeBoundingBox(e);
//				}
//			}
//
//			jsonWriter.endObject(); // end featurecollection
//
//		} catch (JSONException jsonException) {
//
//		} finally {
//			outWriter.flush();
//		}

	}

	public static JSONArray getBB(BoundingBox env)
			throws Exception {
		JSONArray array=new JSONArray();
		array.add(env.getMinX());
		array.add(env.getMinY());
		array.add(env.getMaxX());
		array.add(env.getMaxY());
		return array;
//		GeoJSONBuilder jsonWriter = new GeoJSONBuilder(outWriter);
//		try {
//			jsonWriter.writeBoundingBox(env); // TODO check
//		} catch (JSONException jsonException) {
//
//		} finally {
//			outWriter.flush();
//		}
	}

}
