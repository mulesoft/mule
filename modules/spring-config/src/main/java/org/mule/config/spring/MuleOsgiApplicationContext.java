/*
 * $Id: MuleApplicationContext.java 11018 2008-02-25 20:56:00Z tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.transport.Connector;
import org.mule.config.factories.HostNameFactory;
import org.mule.config.spring.editors.ConnectorPropertyEditor;
import org.mule.config.spring.editors.URIBuilderPropertyEditor;
import org.mule.config.spring.processors.MuleObjectNameProcessor;
import org.mule.config.spring.processors.PropertyPlaceholderProcessor;
import org.mule.endpoint.URIBuilder;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;

/**
 * Adds custom Mule extensions to the standard OsgiBundleXmlApplicationContext.
 */
public class MuleOsgiApplicationContext extends OsgiBundleXmlApplicationContext
{
    private MuleContext muleContext;
    
    public MuleOsgiApplicationContext(String[] configLocations, MuleContext muleContext, BundleContext bundleContext) 
    {
        super(configLocations);
        this.muleContext = muleContext;
        setBundleContext(bundleContext);
        registerBeanFactoryPostProcessors();
    }

    protected void registerBeanFactoryPostProcessors()
    {
        PropertyPlaceholderProcessor ppp = new PropertyPlaceholderProcessor();
        ppp.setIgnoreUnresolvablePlaceholders(true);
        Map factories = new HashMap();
        factories.put("hostname", new HostNameFactory());
        ppp.setFactories(factories);        
        ppp.setMuleContext(muleContext);
        addBeanFactoryPostProcessor(ppp);
    }
    
    //@Override
    protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory)
    {
        super.customizeBeanFactory(beanFactory);

        beanFactory.addPropertyEditorRegistrar(
            new PropertyEditorRegistrar() 
            {
                public void registerCustomEditors(PropertyEditorRegistry registry)
                {
                    registry.registerCustomEditor(Connector.class, new ConnectorPropertyEditor());
                    //registry.registerCustomEditor(UMOEndpointURI.class, new EndpointURIPropertyEditor());
                    registry.registerCustomEditor(URIBuilder.class, new URIBuilderPropertyEditor());
                }
            });

        beanFactory.addBeanPostProcessor(new MuleContextPostProcessor(muleContext));        

        // TODO Is this one still needed?
        beanFactory.addBeanPostProcessor(new MuleObjectNameProcessor());
    }

    //@Override
    protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader)
    {
        super.initBeanDefinitionReader(beanDefinitionReader);        
        // Hook in our custom hierarchical reader
        beanDefinitionReader.setDocumentReaderClass(MuleBeanDefinitionDocumentReader.class);
        // Add error reporting
        beanDefinitionReader.setProblemReporter(new MissingParserProblemReporter());
    }
}
