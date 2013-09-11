/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.collection;

import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
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
    protected Class mapClass;
    protected String name;
    protected boolean attributeName;
    protected boolean dynamicName = false;

    /**
     * Creates a Map parser that will add the map directly to the registry
     *
     * @param mapClass the type of map to create
     */
    public OrphanMapDefinitionParser(Class mapClass)
    {
        this.mapClass = mapClass;
        dynamicName = true;
    }

    /**
     * Creates a Map parser that will add the map directly to the registry
     *
     * @param mapClass the type of map to create
     * @param name the name of the map property
     */
    public OrphanMapDefinitionParser(Class mapClass, String name)
    {
        this.mapClass = mapClass;
        this.name = name;
    }

    /**
     * Creates a Map parser that will add the map directly to the registry
     *
     * @param mapClass the type of map to create
     * @param name the name of the map property
     * @param attributeName whether the name specified is actually an attribute name on the element.  The map name will
     * be retrieved from the element attribute.
     */
    public OrphanMapDefinitionParser(Class mapClass, String name, boolean attributeName)
    {
        this.mapClass = mapClass;
        this.name = name;
        this.attributeName = attributeName;
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
        getBeanAssembler(element, builder).setBeanFlag(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE);
    }

    @Override

    protected void preProcess(Element element)
    {
        super.preProcess(element);
        if (dynamicName)
        {
            name = null;
        }
    }
    
    @java.lang.Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException
    {
        if(attributeName)
        {
            return element.getAttribute(name);
        }
        return name;
    }
}
