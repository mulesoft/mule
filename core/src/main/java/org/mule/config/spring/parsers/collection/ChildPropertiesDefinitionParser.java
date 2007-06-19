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

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import java.util.Properties;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Creates a single Properties object and processes standard Spring sub elements.  The properties are
 * injected into the parent object (the enclosing XML element).  
 */
public class ChildPropertiesDefinitionParser extends ChildDefinitionParser
{

    public ChildPropertiesDefinitionParser(String setterMethod)
    {
        super(setterMethod, /*clazz*/null);
    }
    
    protected Class getBeanClass(Element element)
    {
        return PropertiesFactoryBean.class;
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        super.parseChild(element, parserContext, builder);
        
        Properties parsedProps = parserContext.getDelegate().parsePropsElement(element);
        builder.addPropertyValue("properties", parsedProps);
    }
}
