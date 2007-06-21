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
import org.mule.config.i18n.CoreMessages;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>WebappMuleXmlConfigurationBuilder</code> will first try and load config
 * resources from the Servlet context. If this fails it fails back to the methods
 * used by the MuleXmlConfigurationBuilder.
 *
 * @see org.mule.config.builders.MuleXmlConfigurationBuilder
 */
public class WebappMuleXmlConfigurationBuilder extends MuleXmlConfigurationBuilder
{
    /**
     * Logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(getClass());    private ServletContext context;

    /**
     * Classpath within the servlet context (e.g., "WEB-INF/classes").  Mule will attempt to load config
     * files from here first, and then from the remaining classpath.
     */
    private String webappClasspath;

    public WebappMuleXmlConfigurationBuilder(ServletContext context, String webappClasspath)
        throws ConfigurationException
    {
        super();
        this.context = context;
        this.webappClasspath = webappClasspath;
    }

    /**
     * TODO TC MERGE THIS DOES NOT OVERRIDE SUPER NOW! Recheck.
     * Attempt to load any resource from the Servlet Context first, then from the classpath.
     */
    protected InputStream loadResource(String resource) throws ConfigurationException
    {
        String resourcePath = new File(webappClasspath, resource).getPath();
        logger.debug("Searching for resource " + resourcePath + " in Servlet Context.");
        InputStream is = context.getResourceAsStream(resourcePath);
        if (is == null)
        {
            try
            {
                logger.debug("Resource " + resourcePath + " not found in Servlet Context, loading from classpath");
                is = IOUtils.getResourceAsStream(resource, getClass());
            }
            catch (IOException ioex)
            {
                throw new ConfigurationException(CoreMessages.cannotLoadFromClasspath(resource), ioex);
            }
        }
        return is;
    }
}
