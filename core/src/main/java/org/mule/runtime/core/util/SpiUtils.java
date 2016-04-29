/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util;

import org.mule.runtime.core.api.registry.ServiceType;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.transport.service.TransportFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TODO This will eventually use the OSGi Service Registry for locating services

//@ThreadSafe
public class SpiUtils
{
    private static final Log logger = LogFactory.getLog(SpiUtils.class);

    public static final String SERVICE_ROOT = "META-INF/services/";


    public static Properties findServiceDescriptor(ServiceType type, String name)
    {
        if (type.equals(ServiceType.TRANSPORT))
        {
            // for better EE transports support from earlier versions, try the preferred-xxx lookup first without
            // fallback
            Properties tsd = findServiceDescriptor(type.getPath(), name, TransportFactory.class, false);

            if (tsd == null)
            {
                // regular flow
                tsd = findServiceDescriptor(type.getPath(), name, TransportFactory.class);
            }

            return tsd;
        }
        else if (type.equals(ServiceType.EXCEPTION))
        {
            return findServiceDescriptor(type.getPath(), name, ExceptionHelper.class);
        }
        else
        {
            logger.warn("Attempt to lookup unrecognized service type: " + type);
            return null;
        }

    }

    public static Properties findServiceDescriptor(String path, String name, Class currentClass)
    {
        return findServiceDescriptor(path, name, currentClass, true);
    }

    /**
     * @param fallbackToNonPreferred whether the search should attempt the preferred-xxx.properties lookup
     */
    public static Properties findServiceDescriptor(String path, String name, Class currentClass, boolean fallbackToNonPreferred)
    {
        //Preferred name and preferred path - used to construct a URI for alternative or preferred
        //property set.  This enables alternative implementations of a transport to exist side by side
        //in a single Mule VM.  Most transports will not have a preferred property set.
        String preferredName = null;
        String preferredPath = null;

        if (!name.endsWith(".properties"))
        {
            name += ".properties";
            //convention is preferred-<protocol>.properties
            preferredName = "preferred-" + name;
        }

        if (path.startsWith("/"))
        {
            path = path.substring(1);
        }
        if (!path.endsWith("/"))
        {
            path += "/";
        }
        if (path.startsWith(SERVICE_ROOT))
        {
            path += name;
        }
        else
        {
            preferredPath = SERVICE_ROOT + path + preferredName;
            path = SERVICE_ROOT + path + name;
        }
        try
        {
            //get preferred path first
            InputStream is = IOUtils.getResourceAsStream(preferredPath, currentClass, false, false);

            //if no resource found, then go with default path
            if (is == null && fallbackToNonPreferred)
            {
                is = IOUtils.getResourceAsStream(path, currentClass, false, false);
            }

            if (is != null)
            {
                Properties props = new Properties();
                try
                {
                    props.load(is);
                    return props;
                }
                catch (IOException e)
                {
                    logger.warn("Descriptor found but unable to load properties for service " + name);
                    return null;
                }
            }
            else
            {
                return null;
            }
        }
        catch (IOException e)
        {
            return null;
        }
    }

    /**
     * Find all resources of the given name and merge them
     */
    public static Properties findServiceDescriptors(String path, String name, Class currentClass)
    {
        if (!name.endsWith(".properties"))
        {
            name += ".properties";
        }

        if (path.startsWith("/"))
        {
            path = path.substring(1);
        }
        if (!path.endsWith("/"))
        {
            path += "/";
        }
        if (path.startsWith(SERVICE_ROOT))
        {
            path += name;
        }
        else
        {
            path = SERVICE_ROOT + path + name;
        }
        try
        {
            Properties props = new Properties();
            Enumeration<URL> urls = currentClass.getClassLoader().getResources(path);
            while (urls.hasMoreElements())
            {
                URL url = urls.nextElement();
                InputStream inputStream = null;
                try
                {
                    inputStream = url.openStream();
                    props.load(inputStream);
                }
                catch (IOException e)
                {
                    logger.warn("Descriptor found but unable to load properties for service " + name);
                }
                finally
                {
                    IOUtils.closeQuietly(inputStream);
                }
            }
            return props;
        }
        catch (IOException e)
        {
            return null;
        }
    }
}
