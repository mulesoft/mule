/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.parsers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * TODO
 */
public class NamespaceDefinitionParser extends AbstractChildBeanDefinitionParser
{
    public static final String PROPERTY_NAME = "namespaces";

    public String getPropertyName(Element e)
    {
        return PROPERTY_NAME;
    }

    protected Class getBeanClass(Element element)
    {
        return HashMap.class;
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        String prefix = element.getAttribute("prefix");
        String uri = element.getAttribute("uri");

        Map namespaces = (Map)builder.getBeanDefinition().getPropertyValues().getPropertyValue(PROPERTY_NAME);
        if(namespaces==null)
        {
            namespaces = new HashMap();
            builder.getBeanDefinition().getPropertyValues().addPropertyValue(PROPERTY_NAME, namespaces);
        }
        namespaces.put(prefix, uri);
        postProcess(builder, element);
    }
}