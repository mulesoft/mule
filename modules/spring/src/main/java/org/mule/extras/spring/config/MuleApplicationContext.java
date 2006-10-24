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

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.AbstractXmlApplicationContext;

/**
 * <code>MuleApplicationContext</code> is A Simple extension Application context
 * that allows rosurces to be loaded from the Classpath of file system using the
 * MuleBeanDefinitionReader.
 * 
 * @see MuleBeanDefinitionReader
 * @version $Revision$
 */
public class MuleApplicationContext extends AbstractXmlApplicationContext
{
    private String[] configLocations;

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

    protected void initBeanDefinitionReader(XmlBeanDefinitionReader xmlBeanDefinitionReader)
    {
        super.initBeanDefinitionReader(xmlBeanDefinitionReader);
    }

    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException
    {
        XmlBeanDefinitionReader beanDefinitionReader = new MuleBeanDefinitionReader(beanFactory,
            configLocations.length);
        initBeanDefinitionReader(beanDefinitionReader);
        loadBeanDefinitions(beanDefinitionReader);
    }
}
