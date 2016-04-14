/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.bootstrap;

import org.mule.config.i18n.MessageFactory;
import org.mule.util.ClassUtils;
import org.mule.util.OrderedProperties;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * Looks for bootstrap properties in resources named META-INF/services/org/mule/config/registry-bootstrap.properties
 * inside the classpath.
 * </p>
 * <p>
 * All found properties resources are collected and loaded during the discovery process.
 * Properties are returned in the same order they were found in the classpath.
 * If while loading some properties resource an exception occurs the whole process is interrupted and a
 * {@link org.mule.config.bootstrap.BootstrapException} exception is raised.
 * </p>
 */
public class ClassPathRegistryBootstrapDiscoverer implements RegistryBootstrapDiscoverer
{

    private static final String BOOTSTRAP_PROPERTIES = "META-INF/services/org/mule/config/registry-bootstrap.properties";

    private final transient Log logger = LogFactory.getLog(getClass());

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Properties> discover() throws BootstrapException
    {
        List<Properties> bootstrapsProperties = new LinkedList<Properties>();

        Enumeration<URL> allPropertiesResources = ClassUtils.getResources(BOOTSTRAP_PROPERTIES, getClass());
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
                try (InputStream resourceStream = propertiesResource.openStream())
                {
                    properties.load(resourceStream);
                }
            }
            catch (IOException e)
            {
                throw new BootstrapException(MessageFactory.createStaticMessage("Could not load properties from: %s", propertiesResource.toString()), e);
            }

            bootstrapsProperties.add(properties);
        }
        return bootstrapsProperties;
    }
}
