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
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class looks for bootstrap properties in files named registry-bootstrap.properties inside the META-INF/services/org/mule/config directories contained in the classpath
 */
class ClassPathRegistryBootstrapDiscoverer implements RegistryBootstrapDiscoverer
{

    public static final String SERVICE_PATH = "META-INF/services/org/mule/config/";

    public static final String REGISTRY_PROPERTIES = "registry-bootstrap.properties";

    protected final transient Log logger = LogFactory.getLog(getClass());

    @Override
    public List<Properties> discover() throws IOException
    {
        Enumeration<?> allRegistries = ClassUtils.getResources(SERVICE_PATH + REGISTRY_PROPERTIES, getClass());
        List<Properties> bootstraps = new LinkedList<Properties>();

        while (allRegistries.hasMoreElements())
        {
            URL url = (URL) allRegistries.nextElement();
            if (logger.isDebugEnabled())
            {
                logger.debug("Reading bootstrap file: " + url.toString());
            }
            Properties properties = new OrderedProperties();
            properties.load(url.openStream());
            bootstraps.add(properties);
        }
        return bootstraps;
    }
}
