/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.config.ExceptionHelper;
import org.mule.impl.model.ModelFactory;
import org.mule.providers.service.TransportFactory;
import org.mule.registry.ServiceDescriptorFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @deprecated Mule 2.x will use the OSGi Service Registry for locating services
 */
// @ThreadSafe
public class SpiUtils
{
    private static final Log logger = LogFactory.getLog(SpiUtils.class);

    public static final String SERVICE_ROOT = "META-INF/services/";
    public static final String MODEL_SERVICE_PATH = "org/mule/models/";
    public static final String PROVIDER_SERVICE_PATH = "org/mule/providers/";
    public static final String EXCEPTION_SERVICE_PATH = "org/mule/config/";

    public static Properties findServiceDescriptor(String type, String name)
    {
        if (type.equals(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE)) 
        {
            return findServiceDescriptor(PROVIDER_SERVICE_PATH, name, TransportFactory.class);
        }
        else if (type.equals(ServiceDescriptorFactory.MODEL_SERVICE_TYPE))
        {
            return findServiceDescriptor(MODEL_SERVICE_PATH, name, ModelFactory.class);
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
        name += ".properties";
        
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
            InputStream is = IOUtils.getResourceAsStream(path, currentClass, false, false);
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
}
