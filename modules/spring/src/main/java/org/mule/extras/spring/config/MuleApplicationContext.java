/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.config;

import org.mule.config.MuleObjectNameProcessor;
import org.mule.registry.metadata.MetadataProcessor;
import org.mule.umo.transformer.UMOTransformer;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
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
        beanFactory.registerBeanDefinition("_MuleManagemenetContextFactoryBean", new RootBeanDefinition(AutowireManagementContextFactoryBean.class, true));
        beanFactory.registerBeanDefinition("_springContainerContext", new RootBeanDefinition(SpringContainerContextFactoryBean.class, true));
        beanFactory.addBeanPostProcessor(new MuleObjectNameProcessor());
        beanFactory.addBeanPostProcessor(new MetadataProcessor());
        beanFactory.registerCustomEditor(UMOTransformer.class, new TransformerPropertyEditor(beanFactory));

        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
        //hook in our customheirarchical reader
        beanDefinitionReader.setDocumentReaderClass(MuleBeanDefinitionDocumentReader.class);
        
        beanDefinitionReader.loadBeanDefinitions(configLocations);
        //initBeanDefinitionReader(beanDefinitionReader);
        //loadBeanDefinitions(beanDefinitionReader);

    }
}
