/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.config;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.MapFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WsSecurityConfigDefinitionParser extends ChildDefinitionParser
{

    public WsSecurityConfigDefinitionParser(String setterMethod)
    {
        super(setterMethod, ManagedMap.class);
    }

    protected Class getBeanClass(Element element)
    {
        return MapFactoryBean.class;
    }


    @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        ManagedMap values = new ManagedMap();

        NodeList properties = element.getChildNodes();
        for(int index = 0; index < properties.getLength(); index ++)
        {
            Node property = properties.item(index);
            if(property instanceof Element)
            {
                String key = parseKeyName(property.getLocalName());
                Object value = ((Element)property).getAttribute("value");
                if(key.endsWith("Ref"))
                {
                    value = new RuntimeBeanReference(String.valueOf(value));
                }

                values.put(key, value);
            }
        }

        builder.addPropertyValue("sourceMap", values);
        builder.addPropertyValue("targetMapClass", super.getBeanClass(element));
        postProcess(parserContext, getBeanAssembler(element, builder), element);

        builder.getBeanDefinition().setAttribute(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE, Boolean.TRUE);
    }

    public String parseKeyName(String key)
    {
        String[] words = key.split("-");
        StringBuffer result = new StringBuffer(words[0]);

        for(int index = 1; index < words.length; index++)
        {
            result.append(Character.toUpperCase(words[index].charAt(0)));
            if(words.length > 1)
            {
                result.append(words[index].substring(1, words[index].length()));
            }
        }

        return result.toString();
    }

}
