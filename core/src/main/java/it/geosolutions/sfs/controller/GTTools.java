package it.geosolutions.sfs.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.ws.WebServiceException;

import net.sf.json.JSONArray;

import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * @author carlo cancellieri
 * 
 */
public abstract class GTTools {

	protected static int getCount(SimpleFeatureSource featureSource, Query query)
			throws IOException {
		int count = featureSource.getCount(query);
		if (count == -1) {
			// information was not available in the header!
			SimpleFeatureCollection collection = featureSource
					.getFeatures(query);
			count = collection.size();
		}
		return count;
	}

	protected static SimpleFeatureCollection getCollection(
			SimpleFeatureSource featureSource, Query cql) throws IOException {
		SimpleFeatureCollection collection = featureSource.getFeatures(cql);
		return collection;
	}

	protected static BoundingBox getBB(SimpleFeatureSource featureSource,
			Query query) throws IOException {
		BoundingBox bounds = featureSource.getBounds(query);
		if (bounds == null) {
			// information was not available in the header
			SimpleFeatureCollection collection = featureSource.getFeatures(query);
			bounds = collection.getBounds();
		}
		return bounds;
	}

	protected static int getSRID(CoordinateReferenceSystem crs)
			throws FactoryException {
		Integer SRID = CRS.lookupEpsgCode(crs, true);
		/*
		 * CRS.lookupIdentifier(bb.getCoordinateReferenceSystem(), true);
		 */
		if (SRID == null) {
			throw new IllegalArgumentException(
					"Unable to get the EPSG code for the CRS: " + crs);
		}
		return SRID;
	}

	static final FilterFactory2 FF = CommonFactoryFinder
			.getFilterFactory2(null);

	/**
	 * Build a query based on the
	 * 
	 * @param request
	 * @return
	 */
	protected static Query buildQuery(Map params, String[] attrs, String fid, String[] queryable, String crs, String[] orderBy, SortOrder[] directions, boolean noGeom, String geometry, Double tolerance, String bbox, String lon, String lat, Integer offset, Integer limit, 
			SimpleFeatureType schema) {
		Query query = new Query();
		applyFilter(params, attrs, fid, queryable, crs, geometry, tolerance, bbox, lon, lat, schema, query);
		
		applyAttributeSelection(schema, attrs, noGeom, query);

		// the following apply only in feature collection mode
		if (fid == null) {
			applyMaxFeatures(limit,query);
			applyOffset(offset, query);
			applyOrderBy(orderBy, directions,schema, query);
		}

		return query;
	}

	
	
	private static void applyFilter(Map<String, String[]> form, String[] attrs, String fid, String[] queryable, String crs, String geometry, Double tolerance, String bbox, String lon, String lat,
			SimpleFeatureType schema, Query query) {
		if (fid != null) {
			final Id fidFilter = FF
					.id(Collections.singleton(FF.featureId(fid)));
			query.setFilter(fidFilter);
		} else {
			List<Filter> filters = new ArrayList<Filter>();

			// build the geometry filters
			filters.add(buildGeometryFilter(schema, geometry, tolerance));
			filters.add(buildBBoxFilter(schema, tolerance, bbox));
			filters.add(buildXYToleranceFilter(schema, tolerance, lon, lat));

			// see if we have any non geometric one
			if (queryable != null) {
				for (String name : queryable) {
					AttributeDescriptor ad = schema.getDescriptor(name);
					if (ad == null) {
						throw new WebServiceException(
								"Uknown queryable attribute " + name);
					} else if (ad instanceof GeometryDescriptor) {
						throw new WebServiceException("queryable attribute "
								+ name + " is a geometry, "
								+ "cannot perform non spatial filters on it");
					}

					final PropertyName property = FF.property(name);
					final String prefix = name + "__";
					for (String paramName : form.keySet()) {
						if (paramName.startsWith(prefix)) {
							Literal value = FF.literal(form.get(paramName)[0]);
							String op = paramName.substring(prefix.length());
							if ("eq".equals(op)) {
								filters.add(FF.equals(property, value));
							} else if ("ne".equals(op)) {
								filters.add(FF.notEqual(property, value));
							} else if ("lt".equals(op)) {
								filters.add(FF.less(property, value));
							} else if ("lte".equals(op)) {
								filters.add(FF.lessOrEqual(property, value));
							} else if ("ge".equals(op)) {
								filters.add(FF.greater(property, value));
							} else if ("gte".equals(op)) {
								filters.add(FF.greaterOrEqual(property, value));
							} else if ("like".equals(op)) {
								String pattern = form.get(paramName)[0];
								filters.add(FF.like(property, pattern, "%",
										"_", "\\", true));
							} else if ("ilike".equals(op)) {
								String pattern = form.get(paramName)[0];
								filters.add(FF.like(property, pattern, "%",
										"_", "\\", false));
							} else {
								throw new WebServiceException(
										"Uknown query operand '" + op + "'");
							}
						}
					}
				}
			}

			if (filters.size() > 0) {
				// summarize all the filters
				Filter result = FF.and(filters);
				SimplifyingFilterVisitor simplifier = new SimplifyingFilterVisitor();
				result = (Filter) result.accept(simplifier, null);

				// if necessary, reproject the filters
				if (crs != null) {
					try {
						// apply the default srs into the spatial filters
						CoordinateReferenceSystem sourceCrs = CRS.decode(
								"EPSG:" + crs, true);
						DefaultCRSFilterVisitor crsForcer = new DefaultCRSFilterVisitor(
								FF, sourceCrs);
						result = (Filter) result.accept(crsForcer, null);
					} catch (Exception e) {
						throw new WebServiceException("Uknown EPSG code '"
								+ crs + "'");
					}
				}

				query.setFilter(result);
			}
		}
	}


	private static void applyAttributeSelection(SimpleFeatureType schema,
			String[] attrs, boolean noGeom, Query query) {
		Set<String> attributes = Collections.emptySet();
		if (attrs != null) {
			attributes = new HashSet<String>(Arrays.asList(attrs));
		}

		// build the output property list, if any
		List<String> properties = new ArrayList<String>();
		boolean filterAttributes = attributes.size() > 0;
		if (noGeom || attributes.size() > 0) {
			SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
			tb.setName(schema.getName());
			for (AttributeDescriptor attribute : schema
					.getAttributeDescriptors()) {
				// skip geometric attributes if so requested
				if (attribute instanceof GeometryDescriptor && noGeom) {
					continue;
				}
				// skip unselected attributes (but keep the geometry, that has
				// to be excluded explicitly using nogeom)
				final String name = attribute.getLocalName();
				if (filterAttributes && !attributes.contains(name)
						&& !attribute.equals(schema.getGeometryDescriptor())) {
					continue;
				}
				properties.add(name);
				attributes.remove(name);
			}
			if (properties.size() > 0) {
				query.setPropertyNames(properties);
			}

			// check if we have residual, unknown attributes
			if (attributes.size() > 0) {
				throw new WebServiceException(
						"The following attributes are not known to this service: "
								+ attributes);
			}
		}
	}

	private static void applyOrderBy(
			String[] orderBy, SortOrder[] directions, SimpleFeatureType schema, Query query) {
		if (orderBy != null) {

			// check directions and attributes are matched
			if (directions != null && directions.length != orderBy.length) {
				if (directions.length < orderBy.length) {
					throw new WebServiceException(
							"dir list has less entries than order_by");
				} else {
					throw new WebServiceException(
							"dir list has more entries than order_by");
				}
			}

			SortBy[] sortBy = new SortBy[orderBy.length];
			for (int i = 0; i < orderBy.length; i++) {
				String name = orderBy[i];
				AttributeDescriptor descriptor = schema.getDescriptor(name);
				if (descriptor == null) {
					throw new WebServiceException("Uknown order_by attribute "
							+ name);
				} else if (descriptor instanceof GeometryDescriptor) {
					throw new WebServiceException("order_by attribute " + name
							+ " is a geometry, " + "cannot sort on it");
				}

				sortBy[i] = FF.sort(name, directions[i]);
			}
			query.setSortBy(sortBy);
		}
	}

	private static void applyOffset(Integer offset, Query query) {
		if (offset != null) {
			try {
				query.setStartIndex(offset);
			} catch (NumberFormatException e) {
				throw new WebServiceException("Invalid offset expression: "
						+ offset);
			}
		}
	}

	private static void applyMaxFeatures(Integer limit, Query query) {
		if (limit == null) {
			throw new WebServiceException("Invalid limit expression: "
					+ limit);
		} else {
			query.setMaxFeatures(limit);
		}
	}

//		private static double getTolerance(Map<String, String> form) {
//		String tolerance = form.get("tolerance");
//		if (tolerance != null) {
//			double tolValue = parseDouble(tolerance, "tolerance");
//			if (tolValue < 0) {
//				throw new WebServiceException(
//						"Invalid tolerance, it should be zero or positive: "
//								+ tolValue);
//			}
//			return tolValue;
//		} else {
//			return 0d;
//		}
//	}

	private static Filter buildXYToleranceFilter(SimpleFeatureType schema,
			final Double tolerance, String x, String y) {
		if (x == null && y == null) {
			return Filter.INCLUDE;
		}
		if (x == null || y == null) {
			throw new WebServiceException(
					"Incomplete x/y specification, must provide both values");
		}
		double ordx = parseDouble(x, "x");
		double ordy = parseDouble(y, "y");

		final Point centerPoint = new GeometryFactory()
				.createPoint(new Coordinate(ordx, ordy));
		return geometryFilter(schema, centerPoint, tolerance);

	}

	private static Filter geometryFilter(SimpleFeatureType schema,
			Geometry geometry, Double tolerance) {
		PropertyName defaultGeometry = FF.property(schema
				.getGeometryDescriptor().getLocalName());
		Literal center = FF.literal(geometry);
		
		if (tolerance==null || tolerance == 0) {
			return FF.intersects(defaultGeometry, center);
		} else {
			return FF.dwithin(defaultGeometry, center, tolerance, null);
		}

	}

	private static double parseDouble(String value, String name) {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new WebServiceException("Expected a number for " + name
					+ " but got " + value);
		}
	}

	private static Filter buildBBoxFilter(SimpleFeatureType schema,
			final Double tolerance, String bbox) {
		if (bbox == null) {
			return Filter.INCLUDE;
		} else {
			try {
				JSONArray ordinates = JSONArray.fromObject("[" + bbox + "]");
				String defaultGeomName = schema.getGeometryDescriptor()
						.getLocalName();
				double minx = ordinates.getDouble(0);
				double miny = ordinates.getDouble(1);
				double maxx = ordinates.getDouble(2);
				double maxy = ordinates.getDouble(3);
				if (tolerance!=null && tolerance > 0) {
					minx -= tolerance;
					miny -= tolerance;
					maxx += tolerance;
					maxy += tolerance;
				}
				return FF.bbox(defaultGeomName, minx, miny, maxx, maxy, null);
			} catch (Exception e) {
				throw new WebServiceException("Could not parse the bbox: "
						+ e.getMessage());
			}
		}
	}

	private static Filter buildGeometryFilter(SimpleFeatureType schema,
			String geometry, final Double tolerance) {
		if (geometry == null) {
			return Filter.INCLUDE;
		} else {
			try {
				Geometry geom = new GeometryJSON().read(geometry);
				return geometryFilter(schema, geom, tolerance);
			} catch (IOException e) {
				throw new WebServiceException(
						"Could not parse the geometry geojson: "
								+ e.getMessage());
			}
		}
	}

//	private static SortOrder getSortOrder(OrderType order) {
//		if (order == null) {
//			return SortOrder.ASCENDING;
//		}
//		if (order == null) {
//			return SortOrder.ASCENDING;
//		} else if ("DESC".equals(order)) {
//			return SortOrder.DESCENDING;
//		} else if ("ASC".equals(order)) {
//			return SortOrder.ASCENDING;
//		} else {
//			throw new WebServiceException("Unknown ordering direction: "
//					+ order);
//		}
//	}

}
