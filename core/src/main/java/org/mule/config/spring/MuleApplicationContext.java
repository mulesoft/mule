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

import org.mule.config.spring.editors.TransformerPropertyEditor;
import org.mule.umo.UMOManagementContext;
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
        beanFactory.registerBeanDefinition("_muleManagementContextFactoryBean", new RootBeanDefinition(ManagementContextFactoryBean.class, true));
//        beanFactory.addBeanPostProcessor(new MuleObjectNameProcessor());
        beanFactory.registerCustomEditor(UMOTransformer.class, new TransformerPropertyEditor(beanFactory));

        XmlBeanDefinitionReader beanDefinitionReader = new MuleBeanDefinitionReader(beanFactory, configLocations.length);
        //hook in our customheirarchical reader
        beanDefinitionReader.setDocumentReaderClass(MuleBeanDefinitionDocumentReader.class);

        beanDefinitionReader.loadBeanDefinitions(configLocations);

    }
    public UMOManagementContext getManagementContext()
    {
        return (UMOManagementContext)getBeanFactory().getBean("_muleManagementContextFactoryBean");

    }
}
