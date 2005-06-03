/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.config.builders;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.mule.MuleManager;
import org.mule.config.ConfigurationException;
import org.mule.umo.manager.UMOManager;

/**
 * <code>MuleXmlBuilderContextListener</code> is a bootstrap listener used to
 * construct a MuleManager instance. This listener delegates to the
 * <i>MuleXmlConfigurationBuilder</i>.
 * 
 * <p>
 * The location of the configuration file can be specified in a init parameter
 * called <i>org.mule.config</i>, the value can be a path on the local file
 * system or on the classpath. If a config parameter is not specified a default
 * <i>/mule-config.xml</i> will be used.
 * </p>
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 * @see MuleXmlConfigurationBuilder
 */

public class MuleXmlBuilderContextListener implements ServletContextListener
{
    public static final String CONFIG_INIT_PARAMETER = "org.mule.config";

    public void contextInitialized(ServletContextEvent event)
    {
        String config = event.getServletContext().getInitParameter(CONFIG_INIT_PARAMETER);
        if (config == null) {
            config = getDefaultConfigResource();
        }
        try {
            createManager(config, event.getServletContext());
        } catch (ConfigurationException e) {
            event.getServletContext().log(e.getMessage(), e);
        }
    }

    /**
     * Used to actually construct the UMOManager instance
     * 
     * @param configResource the location of the config resource, this can be on
     *            the local file system or on the classpath.
     * @return A configured UMOManager instance
     */
    protected UMOManager createManager(String configResource, ServletContext context) throws ConfigurationException
    {
        WebappMuleXmlConfigurationBuilder builder = new WebappMuleXmlConfigurationBuilder(context);
        return builder.configure(configResource);
    }

    /**
     * If no config location resource is configured on the servlet context, the
     * value returned from this method will be used to initialise the
     * MuleManager.
     * 
     * @return the default config resource location
     */
    protected String getDefaultConfigResource()
    {
        return "/WEB-INF/mule-config.xml";
    }

    public void contextDestroyed(ServletContextEvent event)
    {
        MuleManager.getInstance().dispose();
    }
}
