package it.geosolutions.sfs.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceException;

import net.sf.json.JSONArray;

import org.geotools.data.Query;
import org.geotools.data.ows.Request;
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

	
	protected static int getCount(SimpleFeatureSource featureSource, Filter cql) throws IOException{
		SimpleFeatureType schema = featureSource.getSchema();
		Query query = new Query( schema.getTypeName(), cql );
		int count = featureSource.getCount( query );
		if( count == -1 ){
		  // information was not available in the header!
		  SimpleFeatureCollection collection = featureSource.getFeatures( query );
		  count = collection.size();
		}
		return count;
	}

	protected static SimpleFeatureCollection getCollection(SimpleFeatureSource featureSource, Filter cql) throws IOException{
		SimpleFeatureCollection collection = featureSource.getFeatures(cql);
		return collection;
	}

	protected static  BoundingBox getBB(SimpleFeatureSource featureSource, Filter cql) throws IOException{
		SimpleFeatureType schema = featureSource.getSchema();
		Query query = new Query( schema.getTypeName(), cql );
		BoundingBox bounds = featureSource.getBounds( query );
		if( bounds == null ){
		   // information was not available in the header
		   FeatureCollection<SimpleFeatureType, SimpleFeature> collection = featureSource.getFeatures( query );
		   bounds = collection.getBounds();
		}
		return bounds;
	}

	protected static int getSRID(CoordinateReferenceSystem crs) throws FactoryException{
		Integer SRID = CRS.lookupEpsgCode(crs, true);
		/*
		 * CRS.lookupIdentifier(bb.getCoordinateReferenceSystem(), true);
		 */
		if (SRID == null) {
			throw new IllegalArgumentException(
					"Unable to get the EPSG code for the CRS: "+crs);
		}
		return SRID;
	}
	
	static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2(null);
	
    /**
     * Build a query based on the
     * 
     * @param request
     * @return
     */
    protected static Query buildQuery(HttpServletRequest request, SimpleFeatureType schema) {
        // get the query string params as a form
        Map<String,String> form = request.getParameterMap();
        Query query = new Query();
        applyFilter(request, schema, form, query);
        applyAttributeSelection(schema, form, query);

        // the following apply only in feature collection mode
        String fid = form.get("fid");
        if (fid == null) {
            applyMaxFeatures(form, query);
            applyOffset(form, query);
            applyOrderBy(schema, form, query);
        }

        return query;
    }

    private static void applyAttributeSelection(SimpleFeatureType schema, Map<String,String> form, Query query) {
        Set<String> attributes = Collections.emptySet();
        String attrs = form.get("attrs");
        if (attrs != null) {
            String[] parsedAttributes = attrs.split("\\s*,\\s*");
            attributes = new HashSet<String>(Arrays.asList(parsedAttributes));
        }

        // build the output property list, if any
        List<String> properties = new ArrayList<String>();
        boolean skipGeom = "true".equals(form.get("no_geom"));
        boolean filterAttributes = attributes.size() > 0;
        if (skipGeom || attributes.size() > 0) {
            SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
            tb.setName(schema.getName());
            for (AttributeDescriptor attribute : schema.getAttributeDescriptors()) {
                // skip geometric attributes if so requested
                if (attribute instanceof GeometryDescriptor && skipGeom) {
                    continue;
                }
                // skip unselected attributes (but keep the geometry, that has to be excluded explicitly using nogeom)
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
                        "The following attributes are not known to this service: " + attributes);
            }
        }
    }

    private static void applyOrderBy(SimpleFeatureType schema, Map<String,String> form, Query query) {
        String orderBy = form.get("order_by");
        if (orderBy != null) {
            String[] orderByAtts = orderBy.split("\\s*,\\s*");
            String dir = form.get("dir");
            String[] directions = null;
            if (dir != null) {
                directions = dir.split("\\s*,\\s*");
            }

            // check directions and attributes are matched
            if (directions != null && directions.length != orderByAtts.length) {
                if (directions.length < orderByAtts.length) {
                	throw new WebServiceException("dir list has less entries than order_by");
                } else {
                	throw new WebServiceException("dir list has more entries than order_by");
                }
            }

            SortBy[] sortBy = new SortBy[orderByAtts.length];
            for (int i = 0; i < orderByAtts.length; i++) {
                String name = orderByAtts[i];
                SortOrder order = getSortOrder(directions, i);

                AttributeDescriptor descriptor = schema.getDescriptor(name);
                if (descriptor == null) {
                	throw new WebServiceException("Uknown order_by attribute " + name);
                } else if (descriptor instanceof GeometryDescriptor) {
                	throw new WebServiceException("order_by attribute " + name + " is a geometry, "
                            + "cannot sort on it");
                }

                sortBy[i] = FF.sort(name, order);
            }
            query.setSortBy(sortBy);
        }
    }

    private static void applyOffset(Map<String,String> form, Query query) {
        String offset = form.get("offset");
        if (offset != null) {
            try {
                query.setStartIndex(Integer.parseInt(offset));
            } catch (NumberFormatException e) {
            	throw new WebServiceException("Invalid offset expression: " + offset);
            }
        }
    }

    private static void applyMaxFeatures(Map<String,String> form, Query query) {
        String limit = form.get("limit");
        if (limit == null) {
            limit = form.get("maxfeatures");
        }
        if (limit != null) {
            try {
                query.setMaxFeatures(Integer.parseInt(limit));
            } catch (NumberFormatException e) {
            	throw new WebServiceException("Invalid limit expression: " + limit);
            }
        }
    }

    private static void applyFilter(HttpServletRequest request, SimpleFeatureType schema, Map<String,String> form, Query query) {
        String fid = form.get("fid");
        if (fid != null) {
            final Id fidFilter = FF.id(Collections.singleton(FF.featureId(fid)));
            query.setFilter(fidFilter);
        } else {
            List<Filter> filters = new ArrayList<Filter>();

            // build the geometry filters
            filters.add(buildGeometryFilter(schema, form));
            filters.add(buildBBoxFilter(schema, form));
            filters.add(buildXYToleranceFilter(schema, form));

            // see if we have any non geometric one
            String queryable = form.get("queryable");
            if (queryable != null) {
                String[] attributes = queryable.split("\\s*,\\s*");
                for (String name : attributes) {
                    AttributeDescriptor ad = schema.getDescriptor(name);
                    if (ad == null) {
                    	throw new WebServiceException("Uknown queryable attribute " + name);
                    } else if (ad instanceof GeometryDescriptor) {
                        throw new WebServiceException("queryable attribute " + name
                                + " is a geometry, " + "cannot perform non spatial filters on it");
                    }

                    final PropertyName property = FF.property(name);
                    final String prefix = name + "__";
                    for (String paramName : form.keySet()) {
                        if (paramName.startsWith(prefix)) {
                            Literal value = FF.literal(form.get(paramName));
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
                                String pattern = form.get(paramName);
                                filters.add(FF.like(property, pattern, "%", "_", "\\", true));
                            } else if ("ilike".equals(op)) {
                                String pattern = form.get(paramName);
                                filters.add(FF.like(property, pattern, "%", "_", "\\", false));
                            } else {
                                throw new WebServiceException("Uknown query operand '" + op + "'");
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
                String crs = form.get("crs");
                if (crs == null) {
                    crs = form.get("epsg");
                }
                if (crs != null) {
                    try {
                        // apply the default srs into the spatial filters
                        CoordinateReferenceSystem sourceCrs = CRS.decode("EPSG:" + crs, true);
                        DefaultCRSFilterVisitor crsForcer = new DefaultCRSFilterVisitor(FF,
                                sourceCrs);
                        result = (Filter) result.accept(crsForcer, null);
                    } catch (Exception e) {
                    	throw new WebServiceException("Uknown EPSG code '" + crs + "'");
                    }
                }

                query.setFilter(result);
            }
        }
    }
    
    private static double getTolerance(Map<String,String> form) {
        String tolerance = form.get("tolerance");
        if(tolerance != null) {
            double tolValue = parseDouble(tolerance, "tolerance");
            if(tolValue < 0) {
            	throw new WebServiceException("Invalid tolerance, it should be zero or positive: " + tolValue);
            }
            return tolValue;
        } else {
            return 0d;
        }
    }

    private static Filter buildXYToleranceFilter(SimpleFeatureType schema, Map<String,String> form) {
        String x = form.get("lon");
        String y = form.get("lat");
        if (x == null && y == null) {
            return Filter.INCLUDE;
        }
        if (x == null || y == null) {
        	throw new WebServiceException(
                    "Incomplete x/y specification, must provide both values");
        }
        double ordx = parseDouble(x, "x");
        double ordy = parseDouble(y, "y");
        
        final Point centerPoint = new GeometryFactory().createPoint(new Coordinate(ordx, ordy));
        final double tolerance = getTolerance(form);
        return geometryFilter(schema, centerPoint, tolerance);

    }

    private static Filter geometryFilter(SimpleFeatureType schema, Geometry geometry, double tolerance) {
        PropertyName defaultGeometry = FF.property(schema.getGeometryDescriptor().getLocalName());
        Literal center = FF.literal(geometry);
        
        if(tolerance == 0) {
            return FF.intersects(defaultGeometry, center);
        } else {
            return FF.dwithin(defaultGeometry, center, tolerance, null);
        }

    }

    static double parseDouble(String value, String name) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
        	throw new WebServiceException("Expected a number for " + name + " but got " + value);
        }
    }

    private static Filter buildBBoxFilter(SimpleFeatureType schema, Map<String,String> form) {
        String bbox = form.get("bbox");
        if (bbox == null) {
            return Filter.INCLUDE;
        } else {
            try {
                JSONArray ordinates = JSONArray.fromObject("[" + bbox + "]");
                String defaultGeomName = schema.getGeometryDescriptor().getLocalName();
                final double tolerance = getTolerance(form);
                double minx = ordinates.getDouble(0);
                double miny = ordinates.getDouble(1);
                double maxx = ordinates.getDouble(2);
                double maxy = ordinates.getDouble(3);
                if(tolerance > 0) {
                    minx -= tolerance;
                    miny -= tolerance;
                    maxx += tolerance;
                    maxy += tolerance;
                }
                return FF.bbox(defaultGeomName, minx, miny, maxx, maxy, null);
            } catch (Exception e) {
            	throw new WebServiceException("Could not parse the bbox: " + e.getMessage());
            }
        }
    }

    private static Filter buildGeometryFilter(SimpleFeatureType schema, Map<String,String> form) {
        String geometry = form.get("geometry");
        if (geometry == null) {
            return Filter.INCLUDE;
        } else {
            try {
                Geometry geom = new GeometryJSON().read(geometry);
                final double tolerance = getTolerance(form);
                return geometryFilter(schema, geom, tolerance);
            } catch (IOException e) {
            	throw new WebServiceException("Could not parse the geometry geojson: "
                        + e.getMessage());
            }
        }
    }

    static SortOrder getSortOrder(String[] orders, int idx) {
        if (orders == null) {
            return SortOrder.ASCENDING;
        }
        String order = orders[idx];
        if (order == null) {
            return SortOrder.ASCENDING;
        } else if ("DESC".equals(order)) {
            return SortOrder.DESCENDING;
        } else if ("ASC".equals(order)) {
            return SortOrder.ASCENDING;
        } else {
        	throw new WebServiceException("Unknown ordering direction: " + order);
        }
    }

}
