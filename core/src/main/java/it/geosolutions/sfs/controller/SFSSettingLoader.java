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

import it.geosolutions.sfs.data.PropertiesUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 * 
 */
public class SFSSettingLoader {

    protected Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    public final static String IGNORE_NON_FATAL_ERRORS_KEY = "ignoreNonFatalErrors";

    private Properties prop;

    protected boolean IGNORE_NON_FATAL_ERRORS;

    private final Map<String, String[]> attrs = new HashMap<String, String[]>();

    private String[] resources = null;

    // private String sourceName = null;

    private final Map<String, String> geometries = new HashMap<String, String>();
    
    // the name of this instance
    private final String name;

    public final String getName() {
        return name;
    }

    public SFSSettingLoader(final String name, final Properties prop) throws Exception {
        this.name=name;
        init(prop, false);
    }

    public SFSSettingLoader(final String name, final Properties prop, boolean noGeom) throws Exception {
        this.name=name;
        init(prop, noGeom);
    }

    private void init(final Properties prop, boolean noGeom) throws Exception {
        this.prop = prop;

        IGNORE_NON_FATAL_ERRORS = prop.getProperty(IGNORE_NON_FATAL_ERRORS_KEY, "false")
                .equalsIgnoreCase("false") ? false : true;

        resources = PropertiesUtils.getAllResources(getName(), getProp());
        if (resources == null || ArrayUtils.isEmpty(resources))
            throw new IllegalArgumentException("The resources array is null or empty");

        for (String resource : resources) {
            Object attrsObj = getProp().get(
                    this.getClass().getSimpleName() + "." + resource + ".attrs");
            if (attrsObj != null) {
                attrs.put(resource, ((String) attrsObj).split(","));
            } else
                throw new IllegalArgumentException("The attrs array may not be empty");

            if (!noGeom) {
                initGeom(prop, resource);
            }
        }
        // sourceName = getProp().getProperty(this.getClass().getSimpleName() + ".source.name");
        // if (sourceName == null || sourceName.isEmpty())
        // throw new IllegalArgumentException("The source name is null or empty");

    }

    protected void initGeom(Properties prop, String resource) {
        String geometryName = PropertiesUtils.getGeometryByResource(getName(), prop,
                resource);
        if (geometryName == null || geometryName.isEmpty())
            throw new IllegalArgumentException("The geometryName is null or empty");
        geometries.put(resource, geometryName);
    }

    public boolean isIGNORE_NON_FATAL_ERRORS() {
        return IGNORE_NON_FATAL_ERRORS;
    }

    public Properties getProp() {
        return prop;
    }

    public Map<String, String[]> getAttrs() {
        return attrs;
    }

    public String[] getAttrsByResource(String resource) {
        return attrs.get(resource);
    }

    public String[] getResources() {
        return resources;
    }

    public Map<String, String> getGeometries() {
        return geometries;
    }

    public String getGeometriesByResource(String resource) {
        return geometries.get(resource);
    }

    public void setProp(Properties prop) {
        this.prop = prop;
    }

}
