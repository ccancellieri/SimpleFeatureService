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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.opengis.filter.sort.SortOrder;

/**
 * 
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 *
 */
public class SFSParamsModel {
	private String layerName;
	private String fid;
	private boolean noGeom;
	private String[] attrs;
	private Integer limit;
	private Integer offset;
	private String[] orderBy;
	private SortOrder[] directions;
	private String lon;
	private String lat;
	private Double tolerance;
	private String bbox;
	private String geometry;
	private String crs;
	private String[] queryable;
	private ModeType mode;
	private CaseInsensitiveMap hints;
	private HttpServletRequest request;
	
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
	public SFSParamsModel(String layerName, String fid, boolean noGeom,
			String[] attrs, Integer limit, Integer offset, String[] orderBy,
			SortOrder[] directions, String lon, String lat, Double tolerance,
			String bbox, String geometry, String crs, String[] queryable,
			ModeType mode, CaseInsensitiveMap hints, HttpServletRequest request) {
		super();
		this.layerName = layerName;
		this.fid = fid;
		this.noGeom = noGeom;
		this.attrs = attrs;
		this.limit = limit;
		this.offset = offset;
		this.orderBy = orderBy;
		this.directions = directions;
		this.lon = lon;
		this.lat = lat;
		this.tolerance = tolerance;
		this.bbox = bbox;
		this.geometry = geometry;
		this.crs = crs;
		this.queryable = queryable;
		this.mode = mode;
		this.hints = hints;
		this.request = request;
	}

	public String getLayerName() {
		return layerName;
	}

	public void setLayerName(String layerName) {
		this.layerName = layerName;
	}

	public String getFid() {
		return fid;
	}

	public void setFid(String fid) {
		this.fid = fid;
	}

	public boolean isNoGeom() {
		return noGeom;
	}

	public void setNoGeom(boolean noGeom) {
		this.noGeom = noGeom;
	}

	public String[] getAttrs() {
		return attrs;
	}

	public void setAttrs(String[] attrs) {
		this.attrs = attrs;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public String[] getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(String[] orderBy) {
		this.orderBy = orderBy;
	}

	public SortOrder[] getDirections() {
		return directions;
	}

	public void setDirections(SortOrder[] directions) {
		this.directions = directions;
	}

	public String getLon() {
		return lon;
	}

	public void setLon(String lon) {
		this.lon = lon;
	}

	public String getLat() {
		return lat;
	}

	public void setLat(String lat) {
		this.lat = lat;
	}

	public Double getTolerance() {
		return tolerance;
	}

	public void setTolerance(Double tolerance) {
		this.tolerance = tolerance;
	}

	public String getBbox() {
		return bbox;
	}

	public void setBbox(String bbox) {
		this.bbox = bbox;
	}

	public String getGeometry() {
		return geometry;
	}

	public void setGeometry(String geometry) {
		this.geometry = geometry;
	}

	public String getCrs() {
		return crs;
	}

	public void setCrs(String crs) {
		this.crs = crs;
	}

	public String[] getQueryable() {
		return queryable;
	}

	public void setQueryable(String[] queryable) {
		this.queryable = queryable;
	}

	public ModeType getMode() {
		return mode;
	}

	public void setMode(ModeType mode) {
		this.mode = mode;
	}

	public CaseInsensitiveMap getHints() {
		return hints;
	}
	
	public String getHintsValueAsString(String key) {
		if (key==null)
			return null;
		Object o=hints.get(key);
		return (o==null)?"":(String) o;
	}

	public void setHints(CaseInsensitiveMap hints) {
		this.hints = hints;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

}

