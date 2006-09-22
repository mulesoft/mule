/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.builders;

import org.mule.config.ConfigurationException;

import javax.servlet.ServletContext;

import java.io.InputStream;

/**
 * <code>WebappMuleXmlConfigurationBuilder</code> will first try and load
 * config resources from the Servlet context. If this fails it fails back to the
 * methods used by the MuleXmlConfigurationBuilder.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 * @see org.mule.config.builders.MuleXmlConfigurationBuilder
 */
public class WebappMuleXmlConfigurationBuilder extends MuleXmlConfigurationBuilder
{
    private ServletContext context;

    public WebappMuleXmlConfigurationBuilder(ServletContext context) throws ConfigurationException
    {
        super();
        this.context = context;
    }

    /**
     * ConfigResource can be a url, a path on the local file system or a
     * resource name on the classpath Finds and loads the configuration resource
     * by doing the following - 1. load it from the servelet context /WEB-INF 2.
     * load it form the classpath 3. load it from from the local file system 4.
     * load it as a url
     * 
     * @param configResource a single configuration resource
     * @return an inputstream to the resource
     * @throws org.mule.config.ConfigurationException
     * 
     */
    protected InputStream loadConfig(String configResource) throws ConfigurationException
    {
        InputStream is = context.getResourceAsStream(configResource);
        if (is == null) {
            is = super.loadConfig(configResource);
        }
        return is;
    }
}
