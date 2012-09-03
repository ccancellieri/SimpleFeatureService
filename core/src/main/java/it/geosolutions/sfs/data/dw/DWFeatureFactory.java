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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 * 
 */
public class DWFeatureFactory extends PGisFeatureFactory {

	private String[] attrs=null;
	private String[] attrsAppend=null;
	private Class[] classesAppend=null;
	private String sourceName=null;
	private String geometryName=null;

	// DataStore primary key
	private final String dsPk;
	
	public DWFeatureFactory(final Properties prop) throws Exception {
		super(prop);
		sourceName=getProp().getProperty(this.getClass().getSimpleName()+".source.name");
		if (sourceName==null || sourceName.isEmpty())
			throw new IllegalArgumentException("The source name is null or empty");
		
		Object attrsObj=getProp().get(this.getClass().getSimpleName()+"."+sourceName+".attrs");
		Object attrsAppendObj=getProp().get(this.getClass().getSimpleName()+"."+sourceName+".append.attrs");
		Object classesAppendObj=getProp().get(this.getClass().getSimpleName()+"."+sourceName+".append.classes");
		geometryName=getProp().getProperty(this.getClass().getSimpleName()+"."+sourceName+".geometry");
		
		if (attrsObj!=null){
			attrs=((String)attrsObj).split(",");
			if (attrsAppendObj!=null && classesAppendObj!=null){
				attrsAppend=((String)attrsAppendObj).split(",");
				String[] classesStrAppend=((String)classesAppendObj).split(",");
				classesAppend=new Class[classesStrAppend.length];
				if (classesStrAppend.length==attrsAppend.length){
					for (int i=0; i< classesStrAppend.length; i++){
						Class c=Class.forName(classesStrAppend[i]);
						classesAppend[i]=c;
					}
				} else
					throw new IllegalArgumentException("Classes array and attr array are different in length. Classes:"+classesStrAppend.length+" attrsAppend:"+attrsAppend.length);
			}
		}
		
		dsPk=getProp().getProperty("ds_pk");
		if (dsPk==null)
			throw new IllegalArgumentException("The primary key cannot be null");
		if (!ArrayUtils.contains(attrs,dsPk))
			throw new IllegalArgumentException("The primary key should be present into the attribute list");
		if (!ArrayUtils.contains(attrsAppend,getProp().getProperty("dw_pk")))
			throw new IllegalArgumentException("The primary key should be present into the append attribute list");
	}
	
//	public final static String VALUE="value"; 

	/**
	 * 
	 */
	@Override
	public void writeData(SFSParamsModel params,HttpServletResponse response) throws Exception {
		String[] reqAttrs=params.getAttrs();
		List<String> purgedReqAttrs=purgeAttrs(params.getAttrs(), this.attrs);
		params.setAttrs(purgedReqAttrs.toArray(new String[]{}));
		
		params.setLayerName(sourceName);
		
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
			OutputStreamWriter osw=null;
			Writer w=null;
			DataStore dataStore = null;
			try {

				// //////////////////////////
				// writer for result
				os = response.getOutputStream();
				osw=new OutputStreamWriter(os);
				w=new BufferedWriter(osw);
				
				// //////////////////////////
				// GET DataStore
				dataStore = DataStoreUtils.getDataStore(getProp());

				
				// //////////////////////////////////
				// prepare query to PostGis DataStore
//				List<String> reqHints=parseHintsParams(params, this.attrsAppend);
				List<String> reqAttrsAppend=purgeAttrs(reqAttrs, this.attrsAppend);
				if (!reqAttrsAppend.isEmpty()) {
					// check if DataStore primary key is included into the request
					boolean includePK = purgedReqAttrs.contains(dsPk);
					// we have to force including the attribute used to merge data with DW
					if (!includePK){
						purgedReqAttrs.add(dsPk);
						params.setAttrs(purgedReqAttrs.toArray(new String[]{}));
					} else {
						params.setAttrs(purgedReqAttrs.toArray(new String[]{}));
					}
					
					SimpleFeatureCollection collection = getFilteredFeatureCollection(params, dataStore, geometryName);
					
					// ////////////////////////////////////////
					// GET DATA FROM DataWarehouse
					
					// TODO removeme This is only for testing purpose
//					JSONObject json = DWJSONUtils.getDWJSON(new File(
//							"src/main/resources/mdx_results.json"));
					// JSONObject
					// TODO: probably use param.getHints() to build the query...
//					String mdxQuery="http://hqlqatcdras1.hq.un.fao.org:8080/techcdr-mdx/MdxQueryServlet?workspace=Faostat" +
//							"&catalog=sdw_faostat_q" +
//							"&schema=Faostat+Production" +
//							"&language=en" +
//							"&attributes=area%2Cyear%2Cuuid%2C%2C%2Carea_harvested_wheat%2Carea_harvested_rice%2Carea_harvested_maize%2Carea_harvested_flag_wheat%2Carea_harvested_flag_rice%2Carea_harvested_flag_maize%2Cyield_wheat%2Cyield_rice%2Cyield_maize%2Cyield_flag_wheat%2Cyield_flag_rice%2Cyield_flag_maize%2Cproduction_quantity_wheat%2Cproduction_quantity_rice%2Cproduction_quantity_maize%2Cproduction_quantity_flag_wheat%2Cproduction_quantity_flag_rice%2Cproduction_quantity_flag_maize" +
//							"&mdx=with+member+%5BMeasures%5D.%5Buuid%5D+as+%5BArea%5D.%5BArea%5D.CurrentMember.Properties%28%22uuid_area%22%29%0D%0A%0D%0Aselect+%0D%0A%0D%0A++NON+EMPTY+%0D%0A++++++Crossjoin%28%7B%0D%0A++++++++++%5BMeasures%5D.%5Buuid%5D%2C+%0D%0A++++++++++%5BMeasures%5D.%5Bm5510%5D%2C+%0D%0A++++++++++%5BMeasures%5D.%5Bf5510%5D%2C+%0D%0A++++++++++%5BMeasures%5D.%5Bm5312%5D%2C+%0D%0A++++++++++%5BMeasures%5D.%5Bf5312%5D%2C+%0D%0A++++++++++%5BMeasures%5D.%5Bm5419%5D%2C+%0D%0A++++++++++%5BMeasures%5D.%5Bf5419%5D%0D%0A++++++++++%7D%2C%0D%0A++++++++++%7B%0D%0A++++++++++%5BItem%5D.%5BWheat%5D%2C+%0D%0A++++++++++%5BItem%5D.%5BMaize%5D%2C+%0D%0A++++++++++%5BItem%5D.%5BRice%2C+paddy%5D%7D%29+ON+COLUMNS%2C%0D%0A%0D%0A++NON+EMPTY+%0D%0A++++++Crossjoin%28%5BArea%5D.%5BArea%5D.Members%2C+%7B%5BYear%5D.%5B2001%5D+%3A+%5BYear%5D.%5B2011%5D%7D%29+ON+ROWS%0D%0A%0D%0Afrom+%5BCROPS%5D"+
//							"&mime=application%2Fjson" +
//							"&arguments=" +
//							"&callback=";
//					JSONObject json = DWJSONUtils.fetchRESTObject(mdxQuery);

					// ////////////////////////////
					// load data into the map
//					Map<String, Map<String, String>> dwSource = DWJSONUtils
//							.loadSource(json, getProp().getProperty("dw_pk"));
//					dwSource = updateDWSource(dwSource);
					// TODO removeme This is only for testing purpose
					XStream xstream=new XStream();
					Map<String, Map<String, String>> dwSource=(Map<String, Map<String, String>>) xstream.fromXML(new File(getProp().getProperty("dwSource")));
					
					
					// /////////////////////////////////////
					// actually merge data producing output
					DWJSONUtils.writeDWFeatureCollection(collection, purgedReqAttrs, dwSource, reqAttrsAppend,//"yield_wheat"
							includePK, dsPk, true, w);

				} else {
					// query does not require DataWarehouse since no value attribute is specified
					
					SimpleFeatureCollection collection = getFilteredFeatureCollection(params, dataStore, geometryName);
					// /////////////////////////////////////
					// actually merge data producing output
			        JSONUtils.writeFeatureCollection(collection, true, w);
			        
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
			super.writeData(params,response);
			break;
		}
	}
	
	private static List<String> purgeAttrs(String[] attrsList, String[] incomingAttrs){
		List<String> ret=new ArrayList<String>();
		if (incomingAttrs==null || incomingAttrs==null)
			return ret;
		for (String attrName:attrsList){
			if (ArrayUtils.contains(incomingAttrs, attrName)){
				ret.add(attrName);
			}
		}
		return ret;
	}
	
	private static List<String> parseHintsParams(SFSParamsModel params, String[] attrs){
		List<String> ret=new ArrayList<String>();
		if (params.getHints()==null || attrs==null)
			return ret;
		for (String attrName:attrs){
			String paramName=params.getHintsValueAsString(attrName);
			if (paramName!=null){
				ret.add(paramName);
			}
		}
		return ret;
	}
	
	
	private static SimpleFeatureCollection getFilteredFeatureCollection(SFSParamsModel params, DataStore dataStore, String geometryName) throws IOException{
		SimpleFeatureType schema = dataStore.getSchema(params.getLayerName());
		GeometryDescriptor geomDesc=null;
		if (geometryName==null)
			geomDesc=schema.getGeometryDescriptor();
		else
			geomDesc=GTTools.getGeometryDescriptor(schema, geometryName);
		
		final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.init(schema);
		b.setDefaultGeometry(geomDesc.getLocalName());
		b.setName(schema.getName());
		
		
		final Query query = GTTools.buildQuery(params.getRequest()
				.getParameterMap(), params.getAttrs(), params.getFid(),
				params.getQueryable(), params.getCrs(), params
						.getOrderBy(), params.getDirections(), params
						.isNoGeom(), params.getGeometry(), params
						.getTolerance(), params.getBbox(), params
						.getLon(), params.getLat(), params.getOffset(),
				params.getLimit(), b.buildFeatureType());

		// //////////////////////////
		// GET DATA FROM POSTGIS
		final SimpleFeatureSource featureSource = dataStore.getFeatureSource(params.getLayerName());
		
		return featureSource.getFeatures(query);
	}

	private static SimpleFeatureType mergeSchema(SimpleFeatureType schema, String typeName, String[] attrs, String[] appendAttrs, Class<?>[] appendClass) {
		
		final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.init(schema);
//		for (AttributeDescriptor ad:schema.getAttributeDescriptors()){
//			String name=ad.getLocalName();
//			if (ArrayUtils.contains(attrs,name)){
//				b.add(ad);
//			} // TODO else LOG
//		}
		if (appendClass.length==appendAttrs.length){
			for (int i=0; i<appendAttrs.length; i++){
				b.add(appendAttrs[i],appendClass[i]);
			}
		} // TODO else LOG
//		b.setDefaultGeometry(geometryName);
		b.setName(typeName);
//		b.add(VALUE, String.class);
		return b.buildFeatureType();
	}
	
	@Override
	public SimpleFeatureType getSimpleFeatureType(final String typeName) throws Exception {
		try {
			
			
			return mergeSchema(super.getSimpleFeatureType(typeName),sourceName, attrs, attrsAppend, classesAppend);
			
		} catch (Exception ioe) {
			if (!IGNORE_NON_FATAL_ERRORS)
				throw ioe;
			return null;
		}
	}
	

	/**
	 * TODO this is only for testing purpose.
	 * remember to set 'dw_pk=area' into datastore.properties
	 * @param dwSource
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static Map<String, Map<String, String>> updateDWSource(Map<String, Map<String, String>> dwSource) throws FileNotFoundException, IOException{
		Map<String, Map<String, String>> ret=new HashMap<String, Map<String, String>>(); 
		Properties p=new Properties();
		p.load(new FileReader("src/main/resources/gaul_2008_uuid_names.csv"));
		ret.put(DWJSONUtils.MEDATADA_KEY, dwSource.get(DWJSONUtils.MEDATADA_KEY));
		for (Object o:p.keySet()){
			String s=(String)o;
//		for (String s:dwSource.keySet()){
//			dwSource.put((String)s, p.getProperty((String)s));
			Map<String, String> entry=dwSource.get(s);
			if (entry==null){
				System.out.println("UNABLE TO LOCATE: "+s+" Into mdx query");
				continue;
			} else if (s.equalsIgnoreCase("Mexico")){
				System.out.println("MEXICO: "+s);
				
			}
//			if (s.equalsIgnoreCase(DWJSONUtils.MEDATADA_KEY)){
//				
//				continue;
//			} else if (s.equalsIgnoreCase("Antarctica")){
//				//update uuid
//				String uuid=p.getProperty((String)s);
//				if (uuid!=null){
//					entry.put("uuid", uuid);
//					ret.put(uuid, entry);
//				}
//			}
			//update uuid
			String uuid=p.getProperty(s);
			if (uuid!=null){
				if (!ret.containsKey(uuid)){
					System.out.println("Adding: "+s+" with UUID:"+uuid);
					entry.put("uuid", uuid);
					ret.put(uuid, entry);
				} else {
					System.out.println("uuid already present for key: "+s);
					entry.put("uuid", uuid);
					ret.put(uuid+"__"+UUID.randomUUID(), entry);
				}
			} else {
				System.out.println("uuid NULL for: "+s);
			}
		}
		FileWriter fw =new FileWriter(new File("src/main/resources/tmp.log"));
		XStream xstream=new XStream();
		xstream.toXML(ret, fw);					
		fw.close();

		return ret;
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
