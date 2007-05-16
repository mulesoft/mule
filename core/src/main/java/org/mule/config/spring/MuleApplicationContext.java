/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.config.MuleProperties;
import org.mule.umo.UMOManagementContext;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

/**
 * <code>MuleApplicationContext</code> is A Simple extension Application context
 * that allows rosurces to be loaded from the Classpath of file system using the
 * MuleBeanDefinitionReader.
 *
 * @see MuleBeanDefinitionReader
 */
public class MuleApplicationContext extends AbstractXmlApplicationContext
{
    private final String[] configLocations;


    public MuleApplicationContext(ApplicationContext parent, String[] configLocations)
    {
        super(parent);
        this.configLocations = configLocations;
        refresh();
    }

    public MuleApplicationContext(String configLocation)
    {
        this(new String[]{configLocation});
    }

    public MuleApplicationContext(String[] configLocations)
    {
        this(configLocations, true);
    }

    public MuleApplicationContext(String[] configLocations, boolean refresh) throws BeansException
    {
        this.configLocations = configLocations;
        if (refresh)
        {
            refresh();
        }
    }

    protected String[] getConfigLocations()
    {
        return configLocations;
    }

    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException
    {
        XmlBeanDefinitionReader beanDefinitionReader = new MuleBeanDefinitionReader(beanFactory, configLocations.length);
        //hook in our customheirarchical reader
        beanDefinitionReader.setDocumentReaderClass(MuleBeanDefinitionDocumentReader.class);

        beanDefinitionReader.loadBeanDefinitions(configLocations);

    }
    public UMOManagementContext getManagementContext()
    {
        return (UMOManagementContext)getBeanFactory().getBean(MuleProperties.OBJECT_MANAGMENT_CONTEXT);

    }


    //@Override
    protected DefaultListableBeanFactory createBeanFactory()
    {
        //Copy all postProcessors defined in the defaultMuleConfig so that they get applied to the child container
        DefaultListableBeanFactory bf = super.createBeanFactory(); 
        if(getParent()!=null)
        {
            //Copy over all processors
            AbstractBeanFactory beanFactory = (AbstractBeanFactory)getParent().getAutowireCapableBeanFactory();
            bf.copyConfigurationFrom(beanFactory);
        }
        return bf;
    }

}
