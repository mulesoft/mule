/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.bootstrap;

import org.mule.util.ClassUtils;
import org.mule.util.OrderedProperties;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RegistryBootstrapServiceUtil
{

    private static final String BOOTSTRAP_PROPERTIES = "META-INF/services/org/mule/config/registry-bootstrap.properties";

    private static Log logger = LogFactory.getLog(RegistryBootstrapServiceUtil.class);

    private RegistryBootstrapServiceUtil()
    {
    }

    public static void configureUsingClassPath(RegistryBootstrapService registryBootstrapService)
    {
        Enumeration<URL> allPropertiesResources = ClassUtils.getResources(BOOTSTRAP_PROPERTIES, RegistryBootstrapServiceUtil.class);
        while (allPropertiesResources.hasMoreElements())
        {
            URL propertiesResource = allPropertiesResources.nextElement();
            if (logger.isDebugEnabled())
            {
                logger.debug("Reading bootstrap properties from: " + propertiesResource.toString());
            }
            Properties properties = new OrderedProperties();
            try
            {
                properties.load(propertiesResource.openStream());
            }
            catch (IOException e)
            {
                throw new IllegalStateException("Unable to load transport descriptor", e);
            }

            BootstrapPropertiesService bootstrapPropertiesService = new MuleBootstrapPropertiesService(properties);
            registryBootstrapService.register(bootstrapPropertiesService);

        }
    }

}
