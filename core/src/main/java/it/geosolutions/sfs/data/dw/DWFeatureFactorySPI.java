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

import it.geosolutions.sfs.data.FeatureFactory;
import it.geosolutions.sfs.data.FeatureFactorySPI;
import it.geosolutions.sfs.data.PropertiesUtils;

import java.io.File;
import java.util.Properties;

/**
 * 
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 * 
 */
public class DWFeatureFactorySPI extends FeatureFactorySPI {

    private final File properties;

    private Properties prop;

    public DWFeatureFactorySPI(final File properties, final int priority) throws Exception {
        super(priority);
        this.properties = properties;
    }

    public Properties getProp() {
        return prop;
    }

    /**
     * 
     */
    @Override
    public boolean canCreate() throws Exception {
        try {
            prop = PropertiesUtils.loadPropertiesFromURL(properties.toURI().toURL());

            
            // TODO may we want to check the database settings here???

        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public FeatureFactory getFeatureFactory() throws Exception {
        return new DWFeatureFactory(DWFeatureFactory.class.getSimpleName(),getProp(),false);
    }
    
    @Override
    public FeatureFactory createFeatureFactory(String name,boolean noGeom) throws Exception {
        return new DWFeatureFactory(name,getProp(),noGeom);
    }

}
