/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.config;

import org.mule.tools.visualizer.components.EndpointRegistry;

import java.util.Properties;

/**
 * todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GraphEnvironment
{

    // Determines if the manager is running synchronously by default
    private boolean defaultTwoWay = false;

    // Is the parser doing a combined generation
    private boolean doingCombinedGeneration = false;

    // Stores references to endpoints registered in the graph
    private EndpointRegistry endpointRegistry;

    private GraphConfig config;

    private Properties properties;

    public GraphEnvironment(GraphConfig config)
    {
        this.config = config;
        properties = new Properties();
        endpointRegistry = new EndpointRegistry(this);
    }

    public boolean isDefaultTwoWay()
    {
        return defaultTwoWay;
    }

    public void setDefaultTwoWay(boolean defaultTwoWay)
    {
        this.defaultTwoWay = defaultTwoWay;
    }

    public boolean isDoingCombinedGeneration()
    {
        return doingCombinedGeneration;
    }

    public void setDoingCombinedGeneration(boolean doingCombinedGeneration)
    {
        this.doingCombinedGeneration = doingCombinedGeneration;
    }

    public EndpointRegistry getEndpointRegistry()
    {
        return endpointRegistry;
    }

    public void setEndpointRegistry(EndpointRegistry endpointRegistry)
    {
        this.endpointRegistry = endpointRegistry;
    }

    public GraphConfig getConfig()
    {
        return config;
    }

    public void setConfig(GraphConfig config)
    {
        this.config = config;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public void setProperties(Properties properties)
    {
        this.properties = properties;
    }

    public void setProperty(String name, String value)
    {
        properties.setProperty(name, value);
    }

    public String getProperty(String name)
    {
        return getProperty(name, null);
    }

    public String getProperty(String name, String defaultValue)
    {
        return properties.getProperty(name, defaultValue);
    }

    public void log(String message)
    {
        System.out.println(message);
    }

    public void logError(String message, Exception e)
    {
        System.err.println(message);
        if (e != null)
        {
            e.printStackTrace(System.err);
        }
    }
}
