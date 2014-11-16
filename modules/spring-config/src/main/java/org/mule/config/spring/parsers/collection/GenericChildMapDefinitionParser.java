/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.collection;

import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.MapFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
* Creates a single Map and processes standard Spring sub elements.  The map is injected
* into the parent object (the enclosing XML element).
*/
public class GenericChildMapDefinitionParser extends ChildDefinitionParser
{

    private final String childElementName;
    private final String childElementKeyAttribute;
    private final String childElementValueAttribute;

    public GenericChildMapDefinitionParser(String setterMethod, String childElementName, String childElementKeyAttribute, String childElementValueAttribute)
    {
        super(setterMethod, HashMap.class);
        addBeanFlag(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE);
        this.childElementName = childElementName;
        this.childElementKeyAttribute = childElementKeyAttribute;
        this.childElementValueAttribute = childElementValueAttribute;
    }

    protected Class getBeanClass(Element element)
    {
        return MapFactoryBean.class;
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        super.parseChild(element, parserContext, builder);
        final Map parseMap = ((MuleHierarchicalBeanDefinitionParserDelegate) parserContext.getDelegate()).parseMapElement(element, childElementName, childElementKeyAttribute, childElementValueAttribute);
        builder.addPropertyValue("sourceMap", parseMap);
        builder.addPropertyValue("targetMapClass", super.getBeanClass(element));
    }

}
