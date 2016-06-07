/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.builders;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.config.ConfigResource;
import org.mule.runtime.config.spring.MuleArtifactContext;
import org.mule.runtime.config.spring.OptionalObjectsController;
import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;

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
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.ServletContextResource;

/**
 * <code>WebappMuleXmlConfigurationBuilder</code> will first try and load config
 * resources using the ServletContext and if this fails then it will attempt to load
 * config resource from the classpath.
 * <li> ServletContext resources should be relative to the webapp root directory and
 * start with '/'.
 * <li> Classpath resources should be in the webapp classpath and should not start
 * with '/'.
 * 
 * @see org.mule.runtime.config.spring.SpringXmlConfigurationBuilder
 */
public class WebappMuleXmlConfigurationBuilder extends SpringXmlConfigurationBuilder
{    
    /**
     * Logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(getClass());

    private ServletContext context;

    public WebappMuleXmlConfigurationBuilder(ServletContext servletContext, String configResources)
        throws ConfigurationException
    {
        super(configResources);
        context = servletContext;
    }

    public WebappMuleXmlConfigurationBuilder(ServletContext servletContext, String[] configResources)
        throws ConfigurationException
    {
        super(configResources);
        context = servletContext;
    }

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        if (getParentContext() == null)
        {
            setParentContext(loadParentContext(context));
        }

        super.doConfigure(muleContext);
    }

    protected ConfigResource[] loadConfigResources(String[] configs) throws ConfigurationException
    {
        try
        {
            artifcatConfigResources = new ConfigResource[configs.length];
            for (int i = 0; i < configs.length; i++)
            {
                artifcatConfigResources[i] = new ServletContextOrClassPathConfigResource(configs[i]);
            }
            return artifcatConfigResources;
        }
        catch (IOException e)
        {
            throw new ConfigurationException(e);
        }
    }

    @Override
    protected ApplicationContext doCreateApplicationContext(MuleContext muleContext, ConfigResource[] artifactConfigResources, ConfigResource[] springResources, OptionalObjectsController optionalObjectsController)
    {
        Resource[] springServletContextResources = preProcessResources(springResources);
        Resource[] artifactConfigServletContextResources = preProcessResources(artifactConfigResources);
        return new MuleArtifactContext(muleContext, artifactConfigServletContextResources, springServletContextResources);
    }

    private Resource[] preProcessResources(ConfigResource[] configResources)
    {
        Resource[] configServletContextResources = new Resource[configResources.length];
        for (int i = 0; i < configResources.length; i++)
        {
            configServletContextResources[i] = new ServletContextOrClassPathResource(context, configResources[i].getResourceName());
        }
        return configServletContextResources;
    }

    /**
     * Used to lookup parent spring ApplicationContext. This allows a parent spring
     * ApplicatonContet to be provided in the same way you would configure a parent
     * ApplicationContext for a spring WebAppplicationContext
     * 
     * @param servletContext
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
                logger.debug("Getting parent context definition: using parent context key of '" + parentContextKey
                             + "' with BeanFactoryLocator");
            }
            parentContext = (ApplicationContext) locator.useBeanFactory(parentContextKey).getFactory();
        }

        return parentContext;
    }

    class ServletContextOrClassPathConfigResource extends ConfigResource
    {
        public ServletContextOrClassPathConfigResource(String resourceName) throws IOException
        {
            super(resourceName, null);
        }

    }

    /**
     * Combines {@link ServletContextResource} and {@link ClassPathResource} to
     * create a {@link Resource} implementation that first tries to load a resource
     * using the {@link ServletContext} and then fails back to use try to load the
     * resource from the classpath.
     */
    class ServletContextOrClassPathResource extends AbstractResource
    {

        private final ServletContext servletContext;

        private final String path;

        public ServletContextOrClassPathResource(ServletContext servletContext, String path)
        {
            Assert.notNull(servletContext, "Cannot resolve ServletContextResource without ServletContext");
            this.servletContext = servletContext;
            // check path
            Assert.notNull(path, "path is required");
            this.path = StringUtils.cleanPath(path);
        }

        @Override
        public String getFilename()
        {
            return this.path;
        }

        public InputStream getInputStream() throws IOException
        {
            InputStream is = getServletContextInputStream();
            if (is == null)
            {
                is = getClasspathInputStream();
            }
            if (is == null)
            {
                throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
            }
            return is;
        }

        protected InputStream getServletContextInputStream()
        {
            String servletContextPath = path;
            if (!servletContextPath.startsWith("/"))
            {
                servletContextPath = "/" + servletContextPath;
            }
            return servletContext.getResourceAsStream(servletContextPath);
        }

        protected InputStream getClasspathInputStream()
        {
            String classpathPath = path;
            if (classpathPath.startsWith("/"))
            {
                classpathPath = classpathPath.substring(1);
            }
            return ClassUtils.getDefaultClassLoader().getResourceAsStream(classpathPath);
        }

        public String getDescription()
        {
            return path;
        }
    }
}
