/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * <code>MuleXmlBuilderContextListener</code> is a bootstrap listener used to
 * construct a MuleManager instance. This listener delegates to the
 * <i>MuleXmlConfigurationBuilder</i>.
 * <p>
 * The location of the configuration file can be specified in a init parameter called
 * <i>org.mule.config</i>, the value can be a path on the local file system or on
 * the classpath. If a config parameter is not specified a default <i>mule-config.xml</i>
 * will be used.
 * </p>
 * 
 * @see SpringXmlConfigurationBuilder
 */

public class MuleXmlBuilderContextListener implements ServletContextListener
{
    /**
     * One or more Mule config files.
     */
    public static final String INIT_PARAMETER_MULE_CONFIG = "org.mule.config";

    private MuleContext muleContext;

    protected transient final Log logger = LogFactory.getLog(MuleXmlBuilderContextListener.class);

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
            if (logger.isDebugEnabled())
            {
                logger.debug("No Mule config file(s) specified, using default: " + config);
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Mule config file(s): " + config);
            }
        }

        try
        {
            muleContext = createMuleContext(config, context);
            muleContext.start();
        }
        catch (MuleException ex)
        {
            context.log(ex.getMessage(), ex);
            // Logging is not configured OOTB for Tomcat, so we'd better make a
            // start-up failure plain to see.
            ex.printStackTrace();
        }
        catch (Error error)
        {
            // WSAD doesn't always report the java.lang.Error, log it
            context.log(error.getMessage(), error);
            // Logging is not configured OOTB for Tomcat, so we'd better make a
            // start-up failure plain to see.
            error.printStackTrace();
            throw error;
        }
    }

    /**
     * Creates the MuleContext based on the configuration resource(s) and possibly 
     * init parameters for the Servlet.
     */
    protected MuleContext createMuleContext(String configResource, ServletContext context)
        throws ConfigurationException, InitialisationException
    {
        WebappMuleXmlConfigurationBuilder builder = new WebappMuleXmlConfigurationBuilder(context, configResource);
        MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();

        // Support Spring-first configuration in webapps
        final ApplicationContext parentContext = (ApplicationContext) context.getAttribute(
                                                        WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        if (parentContext != null)
        {
            builder.setParentContext(parentContext);
        }
        return muleContextFactory.createMuleContext(builder);
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
        if (muleContext != null)
        {
            if (!muleContext.isDisposing() || !muleContext.isDisposed())
            {
                muleContext.dispose();
            }
        }
    }
}
