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

import it.geosolutions.sfs.utils.GeoJSONBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.NamedIdentifier;
import org.json.simple.parser.ParseException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.sun.org.apache.xerces.internal.util.URI.MalformedURIException;
import com.vividsolutions.jts.geom.Geometry;
/**
 * 
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 *
 */
public class DWJSONUtils {

	public static JSONObject getDWJSON(final File sourceFile)
			throws IOException, ParseException {

		FileReader fr = null;
		try {
			fr = new FileReader(sourceFile);
			// String restObj=fetchRESTObject();
			// source = (JSONObject) JSONSerializer.toJSON(restObj);
			return (JSONObject) JSONSerializer.toJSON(IOUtils.toString(fr));

		} finally {
			IOUtils.closeQuietly(fr);
		}
	}

	/**
	 * @return
	 * @throws MalformedURIException
	 */
	protected static JSONObject fetchRESTObject(String uri) throws MalformedURIException {
    	RestTemplate restTemplate=new RestTemplate();
    	List<HttpMessageConverter<?>> messageConverters=new LinkedList<HttpMessageConverter<?>>();
    	messageConverters.add(new org.springframework.http.converter.json.MappingJacksonHttpMessageConverter());
    	
    	restTemplate.setMessageConverters(messageConverters);
    	HttpURLConnection connection = null;
    	try { 
            URL url = new URL(uri); 
            connection = (HttpURLConnection) url.openConnection(); 
//            connection.setDoOutput(false); 
//            connection.setInstanceFollowRedirects(false); 
//            connection.setRequestMethod("GET"); 
//            connection.setRequestProperty("Content-Type", "application/json"); 

    		
            return (JSONObject) JSONSerializer.toJSON(IOUtils.toString(connection.getInputStream(),connection.getContentEncoding())); 
            
//            jaxbContext.createMarshaller().marshal(customer, os); 
//            os.flush(); 

             
        } catch(Exception e) { 
			e.printStackTrace();
        } finally {
        	if (connection!=null){
        		connection.disconnect();
            }
        }
    	return null;
    }

	/*
	 * 
	 * { result: { list: { items: [ { area: "Armenia", year: "2001",
	 * area_harvested_wheat: 108380, area_harvested_rice: null,
	 * area_harvested_maize: 2602, area_harvested_flag_wheat: "  ",
	 * area_harvested_flag_rice: null, area_harvested_flag_maize: "  ",
	 * yield_wheat: 22659.07, yield_rice: null, yield_maize: 37913.14,
	 * yield_flag_wheat: "Fc", yield_flag_rice: null, yield_flag_maize: "Fc",
	 * production_quantity_wheat: 245579, production_quantity_rice: null,
	 * production_quantity_maize: 9865, production_quantity_flag_wheat: "  ",
	 * production_quantity_flag_rice: null, production_quantity_flag_maize: "  "
	 * },
	 */
	protected static Map<String, Map<String, String>> loadSource(
			JSONObject source, final String dw_pk) {
		Map<String, Map<String, String>> data = new TreeMap<String, Map<String, String>>();
		JSONArray array = source.getJSONObject("result").getJSONObject("list")
				.getJSONArray("items");
		Map<String, String> metadata = new HashMap<String, String>();
		data.put(MEDATADA_KEY, metadata);
		Iterator<JSONObject> it = array.listIterator();
		if (it.hasNext()) {
			JSONObject item = it.next();
			Map<String, String> itemStore = new HashMap<String, String>();
			Integer position = 0;
			Iterator<String> itKeys = item.keys();
			while (itKeys.hasNext()) {
				String key = itKeys.next();
				// store metadata and position
				metadata.put(key, (position++).toString());

				final String value = item.getString(key);

				if (key.equalsIgnoreCase(dw_pk)) {
					// store data into the datastore
					data.put(value, itemStore);
				} else {
					// store data for the first item
					itemStore.put(key, value);
				}
			}

		}
		while (it.hasNext()) {
			JSONObject item = it.next();
			Map<String, String> itemStore = new HashMap<String, String>();
			Iterator<String> itKeys = item.keys();
			while (itKeys.hasNext()) {
				String key = itKeys.next();
				final String value = item.getString(key);
				if (key.equalsIgnoreCase(dw_pk)) {
					// store data into the datastore
					data.put(value, itemStore);
				} else {
					// store data for the first item
					itemStore.put(key, value);
				}
			}
		}

		return data;
	}

	public final static String MEDATADA_KEY = "METADATA";

	/**
	 * @param featureCollection
	 * @param featureBounding
	 *            Generate bounds for every feature?
	 * @param outWriter
	 * @throws Exception
	 */
	public static void writeDWFeatureCollection(
			SimpleFeatureCollection featureCollection, List<String> attrName,
			Map<String, Map<String, String>> values, List<String> appendAttrNames, boolean includePK, String dsPK,
			boolean featureBounding, Writer outWriter) throws Exception {
		final FeatureJSON json = new FeatureJSON();
		boolean geometryless = featureCollection.getSchema()
				.getGeometryDescriptor() == null;
		json.setEncodeFeatureCollectionBounds(!geometryless);
		json.setEncodeFeatureCollectionCRS(!geometryless);
		// json.writeFeatureCollection(featureCollection, outWriter);

		GeoJSONBuilder jsonWriter = new GeoJSONBuilder(outWriter);
		//
		boolean hasGeom = false;
		boolean first = true;

		try {
			jsonWriter.object().key("type").value("FeatureCollection");
			jsonWriter.key("features");
			jsonWriter.array();

			CoordinateReferenceSystem crs = null;
			SimpleFeatureIterator iterator = null;
			try {
				SimpleFeatureType fType;
				List<AttributeDescriptor> types;
				iterator = featureCollection.features();
				int pkPos = -1;
				Collection<String> attr = null;
				while (iterator.hasNext()) {
					SimpleFeature feature = (SimpleFeature) iterator.next();
					jsonWriter.object();
					jsonWriter.key("type").value("Feature");
					jsonWriter.key("id").value(feature.getID());

					fType = feature.getFeatureType();
					types = fType.getAttributeDescriptors();

					GeometryDescriptor defaultGeomType = fType
							.getGeometryDescriptor();

					if (crs == null && defaultGeomType != null)
						crs = fType.getGeometryDescriptor()
								.getCoordinateReferenceSystem();

					jsonWriter.key("geometry");
					Geometry aGeom = (Geometry) feature.getDefaultGeometry();

					if (aGeom == null) {
						// In case the default geometry is not set, we will
						// just use the first geometry we find
						for (int j = 0; j < types.size() && aGeom == null; j++) {
							Object value = feature.getAttribute(j);
							if (value != null && value instanceof Geometry) {
								aGeom = (Geometry) value;
							}
						}
					}
					// Write the geometry, whether it is a null or not
					if (aGeom != null) {
						jsonWriter.writeGeom(aGeom);
						hasGeom = true;
					} else {
						jsonWriter.value(null);
					}
					if (defaultGeomType != null)
						jsonWriter.key("geometry_name").value(
								defaultGeomType.getLocalName());

					if (first) {
						// store the pg_key position
						attr = values.remove(MEDATADA_KEY).keySet();
						for (int j = 0; j < types.size(); j++) {
							Object value = feature.getAttribute(j);
							AttributeDescriptor ad = types.get(j);
							if (ad.getLocalName().equalsIgnoreCase(dsPK)) {
								pkPos = j;
								break;
							}
						}
						if (pkPos == -1)
							throw new IllegalArgumentException(
									"Unable to locate the pk:" + dsPK);

						if (!attr.containsAll(appendAttrNames))
							throw new IllegalArgumentException(
									"Unable to locate the an attribute called:" + appendAttrNames + ".\nUse one of the following: "+ArrayUtils.toString(attr));
						
						first = false;
					}
					
					jsonWriter.key("properties");
					jsonWriter.object();

					String pkValue = null;
					for (int j = 0; j < attrName.size(); j++) {
						AttributeDescriptor ad = types.get(j);
//						if (!attr.contains(attrName.get(j)))
//							continue;
						Object value = feature.getAttribute(ad.getName());//attrName.get(j)
						if (value != null) {
							
							if (j == pkPos) {
								pkValue = (String) value;
								if (!includePK){
									continue;
								}
							}
							
							if (value instanceof Geometry) {
								// This is an area of the spec where they
								// decided to 'let convention evolve',
								// that is how to handle multiple
								// geometries. My take is to print the
								// geometry here if it's not the default.
								// If it's the default that you already
								// printed above, so you don't need it here.
								if (ad.equals(defaultGeomType)) {
									// Do nothing, we wrote it above
									// jsonWriter.value("geometry_name");
								} else {
									jsonWriter.key(ad.getLocalName());
									jsonWriter.writeGeom((Geometry) value);
								}
							} else {
								jsonWriter.key(ad.getLocalName());
								jsonWriter.value(value);
							}

						} else {
							jsonWriter.key(ad.getLocalName());
							jsonWriter.value(null);
						}
					}
					// append other attributes (JOIN)
					Map<String, String> item = values.get(pkValue);
					if (item != null) {
						for (String value: appendAttrNames){
							jsonWriter.key(value);
							jsonWriter.value(item.get(value));	
						}
					} else {
						for (String value: appendAttrNames){
							jsonWriter.key(value);
							jsonWriter.value(null);	
						}
					}

//					// Bounding box for feature in properties
//					ReferencedEnvelope refenv = new ReferencedEnvelope(
//							feature.getBounds());
//					if (featureBounding && !refenv.isEmpty())
//						jsonWriter.writeBoundingBox(refenv);

					jsonWriter.endObject(); // end the properties
					jsonWriter.endObject(); // end the feature
				}
			} // catch an exception here?
			finally {
				if (iterator!=null)
					iterator.close();
				
				// featureCollection.close(iterator);
			}

			jsonWriter.endArray(); // end features

			// Coordinate Referense System, currently only if the namespace is
			// EPSG
			if (crs != null) {
				Set<ReferenceIdentifier> ids = crs.getIdentifiers();
				// WKT defined crs might not have identifiers at all
				if (ids != null && ids.size() > 0) {
					NamedIdentifier namedIdent = (NamedIdentifier) ids
							.iterator().next();
					String csStr = namedIdent.getCodeSpace().toUpperCase();

					if (csStr.equals("EPSG")) {
						jsonWriter.key("crs");
						jsonWriter.object();
						jsonWriter.key("type").value(csStr);
						jsonWriter.key("properties");
						jsonWriter.object();
						jsonWriter.key("code");
						jsonWriter.value(namedIdent.getCode());
						jsonWriter.endObject(); // end properties
						jsonWriter.endObject(); // end crs
					}
				}
			}

			// Bounding box for featurecollection
			if (hasGeom && featureBounding) {
				ReferencedEnvelope e = featureCollection.getBounds();

				if (e == null) {
					throw new Exception(
							"Unable to get Envelope for featurecollection"); // TODO
																				// throw
																				// a
																				// more
																				// specific
																				// exception
				} else {
					jsonWriter.writeBoundingBox(e);
				}
			}

			jsonWriter.endObject(); // end featurecollection

		} finally {
			if (outWriter!=null)
				outWriter.flush();
		}

	}

}
