/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.collection;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

import java.util.Map;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.MapFactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Creates a single, stand-alone map object and processes standard Spring sub elements
 */
public class OrphanMapDefinitionParser extends AbstractMuleBeanDefinitionParser
{
    private Class mapClass;
    private String name;
    private boolean dynamicName = false;

    public OrphanMapDefinitionParser(Class mapClass)
    {
        this.mapClass = mapClass;
        dynamicName = true;
    }

    public OrphanMapDefinitionParser(Class mapClass, String name)
    {
        this.mapClass = mapClass;
        this.name = name;
    }

    protected Class getBeanClass(Element element)
    {
        return MapFactoryBean.class;
    }

    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        Map parsedMap = parserContext.getDelegate().parseMapElement(element, builder.getRawBeanDefinition());
        builder.addPropertyValue("sourceMap", parsedMap);
        builder.addPropertyValue("targetMapClass", mapClass.getName());
    }

    //@Override

    protected void preProcess()
    {
        super.preProcess();
        if (dynamicName)
        {
            name = null;
        }
    }
    
    //@java.lang.Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException
    {
        return name;
    }
}
