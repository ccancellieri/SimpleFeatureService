package it.geosolutions.sfs.controller;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.CRS;
import org.json.simple.JSONArray;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
 * @author carlo cancellieri
 *
 */
public abstract class JSONUtils {
	
	protected static JSONArray getDescriptor(SimpleFeatureType schema) throws Exception {
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
	
	protected static JSONArray writeCapabilities(DataStore dataStore) throws Exception {
		JSONArray array=new JSONArray();
		for (String name:dataStore.getTypeNames()){
			SimpleFeatureType schema=dataStore.getSchema(name);
			try {
				array.add(toJSON(schema,dataStore.getFeatureSource(name)));
			} catch (IOException e){
				// skip
			}
			
		}
		
        // write out the layers
//        for (SimpleFeatureIterator it = featureCollection.features(); it.hasNext();) {
//            SimpleFeature feature = it.next();
//        	array.add(toJSON(feature));
//        }
        return array;
	}
	
	/**
     * Maps a layer info to the capabilities json structure. Using a linked hash map under covers to
     * preserve the order of the attributes
     * 
     * @param layerInfo
     * @return
     * @throws IOException
     */
    protected static Map<String,Object> toJSON(SimpleFeatureType layerInfo, SimpleFeatureSource fs) throws IOException {
        
        try {
            Map<String,Object> json = new LinkedHashMap<String,Object>();
            json.put("name", layerInfo.getName().getLocalPart());
            
            try {
                json.put("bbox", toJSON(fs.getBounds()));
            } catch(Exception e) {
                throw ((IOException) new IOException("Failed to get the resource bounding box of:" + layerInfo.getName()).initCause(e));
            }
           
            CoordinateReferenceSystem crs=layerInfo.getCoordinateReferenceSystem();
            String srs=null;
            if (crs!=null)
            	json.put("crs", "urn:ogc:def:crs:EPSG:"+CRS.lookupEpsgCode(crs, false));
            else
                throw ((IOException) new IOException("Failed to get the resource crs:" + layerInfo.getName()));
            
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
     */
    protected static Map<String,Object> toJSON(SimpleFeature layerInfo) throws IOException {
        
//    	SimpleFeatureType schema=dataStore.getSchema(prop.getProperty("FeatureName"));
        try {
            Map<String,Object> json = new LinkedHashMap<String,Object>();
            json.put("name", layerInfo.getName().getLocalPart());
//            json.put("name", schema.getName().getLocalPart());
            
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
	protected static void writeFeatureCollection(
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

	protected static JSONArray getBB(BoundingBox env)
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
