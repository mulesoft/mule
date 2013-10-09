/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
public class ChildMapDefinitionParser extends ChildDefinitionParser
{

    public ChildMapDefinitionParser(String setterMethod)
    {
        super(setterMethod, HashMap.class);
        addBeanFlag(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE);
    }

    protected Class getBeanClass(Element element)
    {
        return MapFactoryBean.class;
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        super.parseChild(element, parserContext, builder);
        Map parsedMap = parserContext.getDelegate().parseMapElement(element, builder.getRawBeanDefinition());
        builder.addPropertyValue("sourceMap", parsedMap);
        builder.addPropertyValue("targetMapClass", super.getBeanClass(element));
    }

}
