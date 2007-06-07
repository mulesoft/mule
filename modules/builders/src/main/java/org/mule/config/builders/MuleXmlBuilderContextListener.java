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

import org.mule.MuleServer;
import org.mule.config.ConfigurationException;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.util.StringUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * <code>MuleXmlBuilderContextListener</code> is a bootstrap listener used to
 * construct a MuleManager instance. This listener delegates to the
 * <i>MuleXmlConfigurationBuilder</i>.
 * <p>
 * The location of the configuration file can be specified in a init parameter called
 * <i>org.mule.config</i>, the value can be a path on the local file system or on
 * the classpath. If a config parameter is not specified a default
 * <i>mule-config.xml</i> will be used.
 * </p>
 *
 * @see MuleXmlConfigurationBuilder
 */

public class MuleXmlBuilderContextListener implements ServletContextListener
{
    /**
     * One or more Mule config files.
     */
    public static final String INIT_PARAMETER_MULE_CONFIG = "org.mule.config";

   /**
     * Classpath within the servlet context (e.g., "WEB-INF/classes").  Mule will attempt to load config
     * files from here first, and then from the remaining classpath.
     */
    public static final String INIT_PARAMETER_WEBAPP_CLASSPATH = "org.mule.webapp.classpath";

    private UMOManagementContext managementContext;

    public void contextInitialized(ServletContextEvent event)
    {
        initialize(event.getServletContext());
    }

    public void initialize(ServletContext context)
    {
        String config = context.getInitParameter(INIT_PARAMETER_MULE_CONFIG);
        if (config == null)
        {
            config = getDefaultConfigResource();
        }

        String webappClasspath = context.getInitParameter(INIT_PARAMETER_WEBAPP_CLASSPATH);
        if (StringUtils.isBlank(webappClasspath))
        {
            webappClasspath = null;
        }

        try
        {
            createManager(config, webappClasspath, context);
        }
        catch (UMOException ex)
        {
            context.log(ex.getMessage(), ex);
            // Logging is not configured OOTB for Tomcat, so we'd better make a start-up failure plain to see.
            ex.printStackTrace();
        }
        catch (Error error)
        {
            // WSAD doesn't always report the java.lang.Error, log it
            context.log(error.getMessage(), error);
            // Logging is not configured OOTB for Tomcat, so we'd better make a start-up failure plain to see.
            error.printStackTrace();
            throw error;
        }
    }

    /**
     * Used to actually construct the UMOManager instance
     *
     * @param configResource the location of the config resource, this can be on the
     *            local file system or on the classpath.
     * @return A configured UMOManager instance
     */
    protected UMOManagementContext createManager(String configResource, String webappClasspath, ServletContext context)
        throws ConfigurationException
    {
        WebappMuleXmlConfigurationBuilder builder = new WebappMuleXmlConfigurationBuilder(context, webappClasspath);
        managementContext = builder.configure(configResource, null);
        return managementContext;
    }

    /**
     * If no config location resource is configured on the servlet context, the value
     * returned from this method will be used to initialise the MuleManager.
     *
     * @return the default config resource location
     */
    protected String getDefaultConfigResource()
    {
        return MuleServer.DEFAULT_CONFIGURATION;
    }

    public void contextDestroyed(ServletContextEvent event)
    {
        destroy();
    }

    public void destroy()
    {
        managementContext.dispose();
    }
}
