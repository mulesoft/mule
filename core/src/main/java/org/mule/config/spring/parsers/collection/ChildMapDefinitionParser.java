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

import org.mule.config.spring.parsers.generic.SimpleChildDefinitionParser;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.MapFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Creates a single Properties object and processes standard Spring Properties sub elements */
public class ChildMapDefinitionParser extends SimpleChildDefinitionParser
{

    public ChildMapDefinitionParser(String setterMethod)
    {
        super(setterMethod, HashMap.class);
    }

    public ChildMapDefinitionParser(String setterMethod, Class mapType)
    {
        super(setterMethod, mapType, Map.class);
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
