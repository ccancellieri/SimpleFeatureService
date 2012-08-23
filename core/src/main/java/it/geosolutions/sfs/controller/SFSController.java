package it.geosolutions.sfs.controller;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.json.simple.JSONArray;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * OpenDataStore WebService
 * 
 * A webservice must be developed to interface with the GeoServer OpenDataStore
 * 
 * the basic requirement are:
 * 
 * - when calling the GeoServer GetMap request a (hint) parameter must be
 * provided to specify the URL of an XML file containing the description and
 * values of all the other parameters.
 * 
 * - in the XML file must be specified a list of items where each of them has
 * the following properties:
 * 
 * x_lon - the longitude coordinate of the point.
 * 
 * y_lat - the latitude coordinate of the point
 * 
 * size - will be used in the parametric SLD (linked with the OpenDataStore
 * layer) to render the point marker with the selected size.
 * 
 * At the end, the webservice must returned as layer a logical table with:
 * 
 * fid - (integer)feature id size - (integer)the size value extracted from the
 * XML file the_geom - (POINT, Geometry) the point coordinates in WKB format
 * 
 * 
 * Moreover, to display properly the point on the map, a parametric SLD must be
 * supplied as default style to the OpenDataStore layer
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */

@Controller
// @RequestMapping("/")
public class SFSController {

	/**
	 * Default logger
	 */
	private final static Logger LOGGER = LoggerFactory
			.getLogger(SFSController.class);

	/**
	 * /capabilities
	 * 
	 * @return DataStore.getTypeNames()
	 */
	@RequestMapping(value = "/capabilities", method = RequestMethod.GET)
	public @ResponseBody JSONArray getCapabilities(HttpServletRequest request) {
		Properties prop = null;
		DataStore dataStore = null;
		try {
			prop = DataStoreUtils.loadPropertiesFromURL(new File(
					"src/main/resources/datastore.properties").toURI().toURL());// TODO
			dataStore = DataStoreUtils.getDataStore(prop);
//			 for (Name s:dataStore.getNames())
//				 LOGGER.info(s.getURI());

			return JSONUtils.writeCapabilities(dataStore);

		} catch (MalformedURLException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		} finally {
			if (dataStore != null)
				dataStore.dispose();
		}
		return new JSONArray(); // never here

	}

	/**
	 * /describe/layername Would use both the caps and describe to build the
	 * result (crs comes from caps)
	 * 
	 * @param layerName
	 * @return DataStore.getSchema()
	 */
	@RequestMapping(value = "/describe/{layername}", method = RequestMethod.GET)
	public @ResponseBody
	JSONArray getSchema(@PathVariable(value = "layername") String layerName) {
		Properties prop = null;
		DataStore dataStore = null;
		try {
			prop = DataStoreUtils.loadPropertiesFromURL(new File(
					"src/main/resources/datastore.properties").toURI().toURL());// TODO
			dataStore = DataStoreUtils.getDataStore(prop);

			return JSONUtils.getDescriptor(dataStore.getSchema(layerName));

		} catch (MalformedURLException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		} finally {
			if (dataStore != null)
				dataStore.dispose();
		}
		return new JSONArray(); // never here

	}

	public enum ModeType {
		features, count, bounds;

		public ModeType getMode(String mode) {
			if (mode.equalsIgnoreCase("features")) {
				return features;
			} else if (mode.equalsIgnoreCase("count")) {
				return count;
			} else if (mode.equalsIgnoreCase("bounds")) {
				return bounds;
			} else {
				return null;
			}
		}
	}

	public enum QueryableType {
		eq, // equal to
		ne, // not equal to
		lt, // lower than
		lte, // lower than or equal to
		gt, // greater than
		gte, // greater than or equal to
		like, //
		ilike, //
	}
	
	@RequestMapping(value = {"/data/{layername}"}, method = RequestMethod.GET)
	public @ResponseBody Object getData(
			
			@PathVariable(value = "layername") String layerName,
			/**
			 * @see getDataFid(...)
			 */
			String fid,
			/**
			 * no_geom=true: so that the returned feature has no geometry
			 * ("geometry": null)xm
			 */
			@RequestParam(value = "no_geom", required = false, defaultValue = "false") boolean noGeom,
			/**
			 * attrs={field1}[,{field2},...]: to restrict the list of properties
			 * returned in the feature
			 */
			@RequestParam(value = "attrs", required = false) String[] attrs,
			// limit the number of features to num features
			// (maxfeatures is an alias to limit)
			@RequestParam(value = "limit", required = false, defaultValue = "-1") Integer limit,
			// skip num features
			@RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
			// order the features using field
			@RequestParam(value = "order_by", required = false) String[] orderBy,
			// determine the ordering direction (applies only
			// if orderby_is specified)
			@RequestParam(value = "dir", required = false, defaultValue = "ASC") SortOrder[] directions,
			/**
			 * lon={x}: the x coordinate of the center of the search region,
			 * this coord's projection system can be specified with the epsg
			 * parameter
			 */
			@RequestParam(value = "lon", required = false) String lon,
			/**
			 * lat={y}: the y coordinate of the center of the search region,
			 * this coord's projection system can be specified with the epsg
			 * parameter
			 */
			@RequestParam(value = "lat", required = false) String lat,
			/**
			 * tolerance={num}: the tolerance around the center of the search
			 * region, expressed in the units of the lon/lat coords' projection
			 * system
			 */
			@RequestParam(value = "tolerance", required = false) Double tolerance,
			/**
			 * box={xmin,ymin,xmax,ymax}: a list of coordinates representing a
			 * bounding box, the coords' projection system can be specified with
			 * the epsg parameter
			 */
			@RequestParam(value = "box", required = false) String bbox,
			/**
			 * geometry={geojson}: a GeoJSON string representing a geometry, the
			 * coords' projection system can be specified with the epsg
			 * parameter
			 */
			@RequestParam(value = "geometry", required = false) String geometry,
			/**
			 * crs={num}: the EPSG code of the lon, lat or box values
			 */
			@RequestParam(value = "crs", required = false) String crs,
			/**
			 * queryable={field1}[,{field2},...]}: the names of the feature
			 * fields that can be queried
			 */
			@RequestParam(value = "queryable", required = false) String[] queryable,
			/**
			 * {field}_{query_op}={value}: specify a filter expression, field
			 * must be in the list of fields specified by queryable, supported
			 * query_op's are: eq: equal to ne: not equal to lt: lower than lte:
			 * lower than or equal to gt: greater than gte: greater than or
			 * equal to like ilike
			 */
			// @RequestParam(value = "{field}_{query_opt}", required = false)
			// QueryableType field_query_opt, // TODO this should be done
			// parsing req
			/**
			 * mode. Can be features (default), count, bounds. In features mode
			 * it just returns the features in json format, in count mode it
			 * returns a count of the features satisfying the filters, in bounds
			 * mode it returns the bounding box of the features satisfying the
			 * filter as a json array
			 */
			@RequestParam(value = "mode", required = false, defaultValue = "features") ModeType mode,
			/**
			 * hints. A map providing implementation specific hints. The
			 * expected format is key1:value1;key2:value2;...
			 */
			@RequestParam(value = "hints", required = false) String hints,
			HttpServletRequest request) {
		Properties prop = null;
		DataStore dataStore = null;
		StringWriter sw = null;
		try {
			prop = DataStoreUtils.loadPropertiesFromURL(new File(
					"src/main/resources/datastore.properties").toURI().toURL());// TODO
			dataStore = DataStoreUtils.getDataStore(prop);
			// for (Name s:dataStore.getNames())
			// LOGGER.info(s.getURI());
			final SimpleFeatureSource featureSource = dataStore
					.getFeatureSource(layerName);
			SimpleFeatureType schema=dataStore.getSchema(layerName);
			
			Query query=GTTools.buildQuery(request.getParameterMap(), attrs, fid, queryable, crs, orderBy, directions, noGeom, geometry, tolerance, bbox, lon, lat, offset, limit, schema);
			
			sw = new StringWriter();
			
			
			switch (mode) {
			case bounds:
				// FeatureSource.getFeatures(Query/Filter)
				// /data/layername?mode=features&...
				// Should take the filter and split it into two parts, the
				// one natively supported, and the one that is
				// performed later in memory. Also, it should pass
				// down the view params contained in the
				// Hints.VIRTUAL_TABLE_PARAMETERS hint.
				// TODO checkQueryParams(request);
				

//				Filter filter=GTTools.buildQuery(params, attrs, fid, queryable, crs, orderBy, directions, noGeom, geometry, tolerance, bbox, lon, lat, offset, limit, 1,schema).getFilter();
				
				return it.geosolutions.sfs.controller.JSONUtils.getBB(GTTools.getBB(featureSource, query));

			case features:
				// FeatureSource.getFeatures(Query/Filter)
				// /data/layername?mode=features&...
				// Should take the filter and split it into two parts, the
				// one natively supported, and the one that is
				// performed later in memory. Also, it should pass
				// down the view params contained in the
				// Hints.VIRTUAL_TABLE_PARAMETERS hint.
				it.geosolutions.sfs.controller.JSONUtils
						.writeFeatureCollection(GTTools.getCollection(featureSource,query),
								true, sw);
				return sw.toString();

			case count:
				// FeatureSoruce.getCount(Query)
				// /data/layername?mode=count&...
				// Should also pass down the hints
				return GTTools.getCount(featureSource, query);
						//GTTools.buildQuery(request, schema).getFilter());

			default:
				return "EMPTY"; // TODO

			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		} catch (Throwable e) {
			LOGGER.error(e.getMessage(), e);
		} finally {
			if (dataStore != null)
				dataStore.dispose();
			IOUtils.closeQuietly(sw);
		}
		return ""; // never here
		// if (mode == ModeType.bounds) {
		// } else if (mode == ModeType.features) {
		//
		// } else if (mode == ModeType.count) {
		// return 1; //TODO
		// }
		//
		// return layerName;// TODO
	}

	@RequestMapping(value = {"/data/{layername}/{fid}"}, method = RequestMethod.GET)
	public @ResponseBody
	Object getDataFid(
			@PathVariable(value = "layername") String layerName,
			@PathVariable(value = "fid") String fid,
			@RequestParam(value = "no_geom", required = false, defaultValue = "false") boolean noGeom,
			@RequestParam(value = "attrs", required = false) String[] attrs,
			@RequestParam(value = "limit", required = false, defaultValue = "1") Integer limit,
			@RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
			@RequestParam(value = "order_by", required = false) String[] orderBy,
			@RequestParam(value = "dir", required = false, defaultValue = "ASC") SortOrder[] directions,
			@RequestParam(value = "lon", required = false) String lon,
			@RequestParam(value = "lat", required = false) String lat,
			@RequestParam(value = "tolerance", required = false) Double tolerance,
			@RequestParam(value = "box", required = false) String bbox,
			@RequestParam(value = "geometry", required = false) String geometry,
			@RequestParam(value = "crs", required = false) String crs,
			@RequestParam(value = "queryable", required = false) String[] queryable,
			@RequestParam(value = "mode", required = false, defaultValue = "features") ModeType mode,
			@RequestParam(value = "hints", required = false) String hints,
			HttpServletRequest request) {
		return getData(layerName, fid, noGeom, attrs, limit, offset, orderBy, directions, lon, lat, tolerance, bbox, geometry, crs, queryable, mode, hints, request);
	}
	
	/**
	 * {field}_{query_op}={value}: specify a filter expression, field must be in
	 * the list of fields specified by queryable, supported query_op's are: eq:
	 * equal to ne: not equal to lt: lower than lte: lower than or equal to gt:
	 * greater than gte: greater than or equal to like ilike
	 */
	private boolean checkQueryParams(HttpServletRequest request) {
		Map<String, String> map = request.getParameterMap();// (HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		// TODO

		// FAKE logic
		if (map != null)
			return true;
		return false;
	}
}
