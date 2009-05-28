/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.api.registry.ServiceDescriptorFactory;
import org.mule.config.ExceptionHelper;
import org.mule.transport.service.TransportFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// TODO This will eventually use the OSGi Service Registry for locating services
// @ThreadSafe
public class SpiUtils
{
    private static final Log logger = LogFactory.getLog(SpiUtils.class);

    public static final String SERVICE_ROOT = "META-INF/services/";
    
    /** 
     * @deprecated use {@link #TRANSPORT_SERVICE_PATH}
     */
    @Deprecated
    public static final String PROVIDER_SERVICE_PATH = "org/mule/providers/";
    public static final String TRANSPORT_SERVICE_PATH = "org/mule/transport/";
    public static final String EXCEPTION_SERVICE_PATH = "org/mule/config/";

    public static Properties findServiceDescriptor(String type, String name)
    {
        if (type.equals(ServiceDescriptorFactory.TRANSPORT_SERVICE_TYPE))
        {
            // for better EE transports support from earlier versions, try the preferred-xxx lookup first without fallback
            Properties tsd = findServiceDescriptor(TRANSPORT_SERVICE_PATH, name, TransportFactory.class, false);

            if (tsd == null)
            {
                // fallback to old-style 'providers' location, but still without fallback for preferred transports
                tsd = findServiceDescriptor(PROVIDER_SERVICE_PATH, name, TransportFactory.class, false);
                if (tsd != null)
                {
                    logTsdDeprecationWarning(name);
                }
            }

            if (tsd == null)
            {
                // regular flow
                tsd = findServiceDescriptor(TRANSPORT_SERVICE_PATH, name, TransportFactory.class);


                if (tsd == null)
                {
                    // fallback to old-style location
                    tsd = findServiceDescriptor(PROVIDER_SERVICE_PATH, name, TransportFactory.class);
                    if (tsd != null)
                    {
                        logTsdDeprecationWarning(name);
                    }
                }
            }

            return tsd;
        }
        else if (type.equals(ServiceDescriptorFactory.EXCEPTION_SERVICE_TYPE))
        {
            return findServiceDescriptor(EXCEPTION_SERVICE_PATH, name, ExceptionHelper.class);
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
     * Log a deprecation warning when a Transport Service Descriptor was located in old location
     * (META-INF/service/org/mule/providers).
     * @param transport - transport name
     */
    protected static void logTsdDeprecationWarning(String transport)
    {
        if (logger.isWarnEnabled())
        {
            // only warn if anything found, otherwise just propagate null to the caller
            logger.warn(MessageFormat.format(
                    "[{0}] transport service descriptor must be moved under {1}{2} " +
                    "Old-style {1}{3} has been deprecated and may not be supported in the future.",
                    transport, SERVICE_ROOT, TRANSPORT_SERVICE_PATH, PROVIDER_SERVICE_PATH
            ));
        }
    }

}
