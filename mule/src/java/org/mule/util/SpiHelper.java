/*
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 14-Mar-2004
 * Time: 14:42:39
 */
package org.mule.util;

import org.apache.commons.discovery.DiscoveryException;
import org.apache.commons.discovery.resource.ClassLoaders;
import org.apache.commons.discovery.tools.DiscoverClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.util.Properties;

public class SpiHelper
{
    private static final Log logger = LogFactory.getLog(SpiHelper.class);

     public static final String SERVICE_ROOT = "META-INF/services/";
    /**
     * Find class implementing a specified SPI.
     *
     * @param spi              Service Provider Interface Class.
     * @param propertyFileName is a location of a property file that contains the SPI property value
     * @param defaultImpl      Default implementation class name.
     * @param currentClass     is used to include the classloader of the calling class in the search.
     *                         All system classloaders will be checked as well.
     * @return Class implementing the SPI or null if a service was not found
     */
    public static Class findService(Class spi, String propertyFileName,
                                    String defaultImpl, Class currentClass)
    {
        ClassLoaders loaders = ClassLoaders.getAppLoaders(spi, currentClass, false);
        DiscoverClass discover = new DiscoverClass(loaders);
        try
        {
            return discover.find(spi, propertyFileName, defaultImpl);
        } catch (DiscoveryException e)
        {
            logger.warn("Failed to find service for spi: " + spi.getName());
            return null;
        }
    }

    /**
     * Find class implementing a specified SPI. The system properties will be
     * checked for an SPI property to use. this will be the fully qualified SPI class name.
     *
     * @param spi          Service Provider Interface Class.
     * @param defaultImpl  Default implementation class name.
     * @param currentClass is used to include the classloader of the calling class in the search.
     *                     All system classloaders will be checked as well.
     * @return Class implementing the SPI or the default implementation class if nothing has been found
     */
    public static Class findService(Class spi, String defaultImpl, Class currentClass)
    {
        ClassLoaders loaders = ClassLoaders.getAppLoaders(spi, currentClass, false);
        DiscoverClass discover = new DiscoverClass(loaders);
        try
        {
            return discover.find(spi, System.getProperties(), defaultImpl);
        } catch (DiscoveryException e)
        {
            logger.warn("Failed to find service for spi: " + spi.getName());
            return null;
        }
    }

    /**
     * Find class implementing a specified SPI. The system properties will be
     * checked for an SPI property to use. this will be the fully qualified SPI class name.
     *
     * @param spi          Service Provider Interface Class.
     * @param currentClass is used to include the classloader of the calling class in the search.
     *                     All system classloaders will be checked as well.
     * @return Class implementing the SPI or null if a service was not found
     */
    public static Class findService(Class spi, Class currentClass)
    {
        ClassLoaders loaders = ClassLoaders.getAppLoaders(spi, currentClass, false);
        DiscoverClass discover = new DiscoverClass(loaders);
        try
        {
            return discover.find(spi, System.getProperties());
        } catch (DiscoveryException e)
        {
            logger.warn("Failed to find service for spi: " + spi.getName());
            return null;
        }
    }

    /**
     * Find class implementing a specified SPI.
     *
     * @param spi          Service Provider Interface Class.
     * @param currentClass is used to include the classloader of the calling class in the search.
     * @param props        The properties will be
     *                     checked for an SPI property to use. this will be the fully qualified SPI class name.
     *                     All system classloaders will be checked as well.
     * @return Class implementing the SPI or null if a service was not found
     */
    public static Class findService(Class spi, Properties props, Class currentClass)
    {
        ClassLoaders loaders = ClassLoaders.getAppLoaders(spi, currentClass, false);
        DiscoverClass discover = new DiscoverClass(loaders);
        try
        {
            return discover.find(spi, props);
        } catch (DiscoveryException e)
        {
            logger.warn("Failed to find service for spi: " + spi.getName());
            return null;
        }
    }

    public static InputStream findServiceDescriptor(String path, String name, Class currentClass)
    {
        if (path.startsWith("/"))
        {
            path = path.substring(1);
        }
        if (!path.endsWith("/"))
        {
            path += "/";
        }
        if(path.startsWith(SERVICE_ROOT)) {
            path += name;
        } else {
            path  = SERVICE_ROOT + path + name;
        }
        return ClassHelper.getResourceAsStream(path, currentClass);
    }
}