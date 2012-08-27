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
package it.geosolutions.sfs.controller;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import it.geosolutions.sfs.controller.SFSParamsModel.ModeType;
import it.geosolutions.sfs.data.FeatureFactory;
import it.geosolutions.sfs.data.FeatureFactorySPI;
import it.geosolutions.sfs.utils.ControllerUtils;
import it.geosolutions.sfs.utils.JSONUtils;

import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceException;

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
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;


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
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 * 
 * @TODO: versioning controller
 */
@Controller
public class SFSController {

	/**
	 * Default logger
	 */
	private final static Logger LOGGER = LoggerFactory
			.getLogger(SFSController.class);
	
	private static FeatureFactory featureFactory;

	public SFSController() throws Exception {
		
		// get context
		WebApplicationContext context=ContextLoader.getCurrentWebApplicationContext();
		
		// get featureFactories
		Map<String,FeatureFactorySPI> spiMap=context.getBeansOfType(FeatureFactorySPI.class);
		
		// order by priority
		TreeSet<FeatureFactorySPI> spiOrderedMap=new TreeSet<FeatureFactorySPI>(new Comparator<FeatureFactorySPI>() {
			@Override
			public int compare(FeatureFactorySPI o1, FeatureFactorySPI o2) {
				return o1.getPriority()>o2.getPriority()?1:-1;
			}
		});		
		Iterator<FeatureFactorySPI> it=spiMap.values().iterator();
		while (it.hasNext()){
			FeatureFactorySPI spi=it.next();
			if (spi.canCreate()){
				spiOrderedMap.add(spi);				
			}
		}
		
		// check results and instantiate factory
		if (spiOrderedMap.size()>0){
			FeatureFactorySPI spi=null;
			while (((spi=spiOrderedMap.pollFirst())!=null)){
				try{
					featureFactory = spi.getFeatureFactory();
				} catch (Exception e){
					LOGGER.error(e.getLocalizedMessage(),e);
				}
			}
		}
		if (featureFactory==null)
			throw new WebServiceException("Unable to locate a valid "+FeatureFactory.class.getName());
		
	}


	/**
	 * /capabilities
	 * 
	 * @return DataStore.getTypeNames()
	 * @throws Exception
	 */
	@RequestMapping(value = "/capabilities", method = RequestMethod.GET)
	public @ResponseBody
	JSONArray getCapabilities(HttpServletRequest request)
			throws WebServiceException {
		try {
			return JSONUtils.writeCapabilities(LOGGER,featureFactory.getAllSchemas(),
					featureFactory.getAllReferencedEnvelopes());
		} catch (Exception e) {
			WebServiceException wse = new WebServiceException(
					e.getLocalizedMessage(), e);
			throw wse;
		}

	}

	/**
	 * /describe/layername Would use both the caps and describe to build the
	 * result (crs comes from caps)
	 * 
	 * @param layerName
	 * @return DataStore.getSchema()
	 * @throws Exception
	 */
	@RequestMapping(value = "/describe/{layername}", method = RequestMethod.GET)
	public @ResponseBody
	JSONArray describeLayer(@PathVariable(value = "layername") String layerName)
			throws WebServiceException {
		try {
			SimpleFeatureType schema = featureFactory
					.getSimpleFeatureType(layerName);

			return JSONUtils.getDescriptor(schema);
		} catch (Exception e) {
			WebServiceException wse = new WebServiceException(
					e.getLocalizedMessage(), e);
			throw wse;
		}

	}

	/**
	 * 
	 * @param layerName
	 * @param fid
	 * @param noGeom
	 *            no_geom=true: so that the returned feature has no geometry
	 *            ("geometry": null)xm
	 * @param attrs
	 *            attrs={field1}[,{field2},...]: to restrict the list of
	 *            properties returned in the feature
	 * @param limit
	 *            limit the number of features to num features (maxfeatures is
	 *            an alias to limit)
	 * @param offset
	 *            skip num features
	 * @param orderBy
	 *            order the features using field
	 * @param directions
	 *            determine the ordering direction (applies only if orderby_is
	 *            specified)
	 * @param lon
	 *            lon={x}: the x coordinate of the center of the search region,
	 *            this coord's projection system can be specified with the epsg
	 *            parameter
	 * @param lat
	 *            lat={y}: the y coordinate of the center of the search region,
	 *            this coord's projection system can be specified with the epsg
	 *            parameter
	 * @param tolerance
	 *            tolerance={num}: the tolerance around the center of the search
	 *            region, expressed in the units of the lon/lat coords'
	 *            projection system
	 * @param bbox
	 *            box={xmin,ymin,xmax,ymax}: a list of coordinates representing
	 *            a bounding box, the coords' projection system can be specified
	 *            with the epsg parameter
	 * @param geometry
	 *            geometry={geojson}: a GeoJSON string representing a geometry,
	 *            the coords' projection system can be specified with the epsg
	 *            parameter
	 * @param crs
	 *            crs={num}: the EPSG code of the lon, lat or box values
	 * @param queryable
	 *            queryable={field1}[,{field2},...]}: the names of the feature
	 *            fields that can be queried
	 * @param mode
	 *            mode. Can be features (default), count, bounds. In features
	 *            mode it just returns the features in json format, in count
	 *            mode it returns a count of the features satisfying the
	 *            filters, in bounds mode it returns the bounding box of the
	 *            features satisfying the filter as a json array
	 * @param hints
	 *            hints. A map providing implementation specific hints. The
	 *            expected format is key1:value1;key2:value2;...
	 * @param request
	 *            contains the map with {field}_{query_op}={value}: specify a
	 *            filter expression, field must be in the list of fields
	 *            specified by queryable, supported query_op's are: eq: equal to
	 *            ne: not equal to lt: lower than lte: lower than or equal to
	 *            gt: greater than gte: greater than or equal to like ilike
	 * @throws Exception
	 */
	@RequestMapping(value = { "/data/{layername}" }, method = RequestMethod.GET)
	public @ResponseBody
	Object getData(

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
			 * 
			 * @throws Exception
			 */
			@RequestParam(value = "hints", required = false) String hints,
			HttpServletRequest request) throws WebServiceException {
		try {
			SFSParamsModel params = new SFSParamsModel(layerName, fid, noGeom,
					attrs, limit, offset, orderBy, directions, lon, lat,
					tolerance, bbox, geometry, crs, queryable, mode, ControllerUtils.parseHints(hints),
					request);

			return featureFactory.getData(params);
		} catch (Exception e) {
			WebServiceException wse = new WebServiceException(
					e.getLocalizedMessage(), e);
			throw wse;
		}
	}

	@RequestMapping(value = { "/data/{layername}/{fid}" }, method = RequestMethod.GET)
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
			HttpServletRequest request) throws WebServiceException {
		try {
			SFSParamsModel params = new SFSParamsModel(layerName, fid, noGeom,
					attrs, limit, offset, orderBy, directions, lon, lat,
					tolerance, bbox, geometry, crs, queryable, mode, ControllerUtils.parseHints(hints),
					request);
			return featureFactory.getData(params);

		} catch (Exception e) {
			WebServiceException wse = new WebServiceException(
					e.getLocalizedMessage(), e);
			throw wse;
		}
	}
}
