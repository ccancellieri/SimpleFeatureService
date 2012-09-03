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
package it.geosolutions.sfs.data;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import it.geosolutions.sfs.controller.SFSController;
import it.geosolutions.sfs.controller.SFSParamsModel;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * 
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 *
 */
public abstract class FeatureFactory {

	/**
	 * 
	 * @param layerName
	 * @param fid
	 * @param noGeom
	 *            no_geom=true: so that the returned feature has no geometry
	 *            ("geometry": null)
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
	public abstract void writeData(SFSParamsModel params, HttpServletResponse response) throws Exception;

	/**
	 * @see {@link SFSController#describeLayer(String)}
	 * @param layerName the layerName
	 * @return A SimpleFeatureType containing the features schema
	 * @throws Exception
	 */
	public abstract SimpleFeatureType getSimpleFeatureType(String layerName)
			throws Exception;
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	public abstract void writeCapabilities(HttpServletRequest request, HttpServletResponse response) throws Exception;

}
