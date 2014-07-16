/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.builders;

import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * This ServletContextListener should be used instead of
 * {@link MuleXmlBuilderContextListener} when the webapp is contributing a
 * configuration to an existing Mule instance configured and started at the server
 * level rather than embedding a Mule instance in the webapp itself.
 */
public class DeployableMuleXmlContextListener implements ServletContextListener
{

    protected transient final Log logger = LogFactory.getLog(DeployableMuleXmlContextListener.class);

    private WebappMuleXmlConfigurationBuilder configurationBuilder;
    private static MuleContext muleContext;

    public void contextInitialized(ServletContextEvent event)
    {
        initialize(event.getServletContext());
    }

    public void initialize(ServletContext context)
    {
        String config = context.getInitParameter(MuleXmlBuilderContextListener.INIT_PARAMETER_MULE_CONFIG);
        if (config == null)
        {
            config = MuleServer.DEFAULT_CONFIGURATION;
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

        if (muleContext == null)
        {
            throw new RuntimeException("MuleContext is not available");
        }

        try
        {
            configurationBuilder = new WebappMuleXmlConfigurationBuilder(context, config);
            configurationBuilder.setUseDefaultConfigResource(false);

            // Support Spring-first configuration in webapps
            final ApplicationContext parentContext = (ApplicationContext) context.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
            if (parentContext != null)
            {
                configurationBuilder.setParentContext(parentContext);
            }
            configurationBuilder.configure(muleContext);
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

    public void contextDestroyed(ServletContextEvent event)
    {
        if (muleContext != null && configurationBuilder != null)
        {
            configurationBuilder.unconfigure(muleContext);
        }
    }

    /**
     * This method is to be used only by application server or web container
     * integrations that allow web applications to be hot-deployed.
     * 
     * @param context the single shared muleContext instance that will be used to
     *            configure mule configurations hot-deployed as web application.
     */
    public static void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

}
