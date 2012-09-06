package it.geosolutions.sfs.data;

import it.geosolutions.sfs.data.postgis.DataStoreUtils;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

public class PropertiesUtils {

    public PropertiesUtils() {
        // TODO Auto-generated constructor stub
    }

    public static String[] getAllResources(String name, Properties prop){
    	String allResources=prop.getProperty(name+".resources");
    	if (allResources==null)
    		throw new IllegalStateException("No configured resources were found");
    	return allResources.split(",");
    }

    /**
     * returns the geometry name if specified into the properties object in the following form:<br>
     * FEATUREFACTORY_NAME.RESOURCE_NAME.geometry=GEOMETRYNAME
     * @param c
     * @param prop
     * @param resourceName
     * @return
     */
    public static String getGeometryByResource(String name, Properties prop, String resourceName){
    	return prop.getProperty(name+"."+resourceName+".geometry");
    }

    /**
     * 
     * @param propsURL
     * @return
     * @throws IOException 
     */
    public static Properties loadPropertiesFromURL(URL propsURL) throws IOException {
        final Properties properties = new Properties();
        InputStream stream = null;
        InputStream openStream = null;
        try {
            openStream = propsURL.openStream();
            stream = new BufferedInputStream(openStream);
            properties.load(stream);
        } catch (FileNotFoundException e) {
            if (DataStoreUtils.LOGGER.isErrorEnabled())
                DataStoreUtils.LOGGER.error(e.getLocalizedMessage(), e);
            throw e;
        } catch (IOException e) {
            if (DataStoreUtils.LOGGER.isErrorEnabled())
                DataStoreUtils.LOGGER.error(e.getLocalizedMessage(), e);
            throw e;
        } finally {
    
            if (stream != null)
                IOUtils.closeQuietly(stream);
            if (openStream != null)
                IOUtils.closeQuietly(openStream);
    
        }
        return properties;
    }

}
