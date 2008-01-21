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

import org.mule.api.MuleContext;
import org.mule.config.spring.MuleApplicationContext;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.registry.Registry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.core.io.Resource;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.ServletContextResource;

/**
 * <code>WebappMuleXmlConfigurationBuilder</code> will first try and load config
 * resources from the Servlet context. If this fails it fails back to the methods
 * used by the MuleXmlConfigurationBuilder.
 * 
 * @see org.mule.config.builders.SpringXmlConfigurationBuilder
 */
public class WebappMuleXmlConfigurationBuilder extends SpringXmlConfigurationBuilder
{
    /**
     * Logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(getClass());

    private ServletContext context;

    public WebappMuleXmlConfigurationBuilder(ServletContext servletContext, String[] configResources)
    {
        super(configResources);
        context = servletContext;
    }

    public WebappMuleXmlConfigurationBuilder(ServletContext servletContext, String configResources)
    {
        super(configResources);
        context = servletContext;
    }

    protected void createSpringParentRegistry(MuleContext muleContext, Registry registry, String[] all)
    {
        Resource[] servletContextResources = new Resource[all.length];
        for (int i = 0; i < all.length; i++)
        {
            servletContextResources[i] = new ServletContextOrClassPathResource(context, all[i]);
        }

        parentContext = loadParentContext(context);

        try
        {
            if (parentContext != null)
            {
                new MuleApplicationContext(muleContext, registry, servletContextResources, parentContext);
            }
            else
            {
                new MuleApplicationContext(muleContext, registry, servletContextResources);
            }
        }
        catch (BeansException e)
        {
            // If creation of MuleApplicationContext fails, remove
            // TransientRegistry->SpringRegistry parent relationship
            registry.setParent(null);
            throw e;
        }
    }

    /**
     * Used to lookup parent spring ApplicationContext. This allows a parent spring
     * ApplicatonContet to be provided in the same way you would configure a parent
     * ApplicationContext for a spring WebAppplicationContext
     * 
     * @param servletContext
     * @return
     * @throws BeansException
     */
    protected ApplicationContext loadParentContext(ServletContext servletContext) throws BeansException
    {

        ApplicationContext parentContext = null;
        String locatorFactorySelector = servletContext.getInitParameter(ContextLoader.LOCATOR_FACTORY_SELECTOR_PARAM);
        String parentContextKey = servletContext.getInitParameter(ContextLoader.LOCATOR_FACTORY_KEY_PARAM);

        if (parentContextKey != null)
        {
            // locatorFactorySelector may be null, indicating the default
            // "classpath*:beanRefContext.xml"
            BeanFactoryLocator locator = ContextSingletonBeanFactoryLocator.getInstance(locatorFactorySelector);
            if (logger.isDebugEnabled())
            {
                logger.debug("Getting parent context definition: using parent context key of '"
                             + parentContextKey + "' with BeanFactoryLocator");
            }
            parentContext = (ApplicationContext) locator.useBeanFactory(parentContextKey).getFactory();
        }

        return parentContext;
    }

}

class ServletContextOrClassPathResource extends ServletContextResource
{

    public ServletContextOrClassPathResource(ServletContext servletContext, String path)
    {
        super(servletContext, path);
    }

    public InputStream getInputStream() throws IOException
    {
        InputStream is = getServletContext().getResourceAsStream(getPath());
        if (is == null)
        {
            is = getClass().getResourceAsStream(getPath());
        }
        if (is == null)
        {
            throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
        }
        return is;
    }

}
