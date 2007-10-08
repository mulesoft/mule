/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.collection;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.util.CoreXMLUtils;

import org.springframework.beans.factory.config.MapFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Creates a single, stand-alone map object and processes all attributes to this map
 */
public class AttributeMapDefinitionParser extends ChildDefinitionParser
{
    public AttributeMapDefinitionParser(String setter)
    {
        super(setter, ManagedMap.class);
    }

    protected Class getBeanClass(Element element)
    {
        return MapFactoryBean.class;
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        ManagedMap values = new ManagedMap();
        NamedNodeMap attributes = element.getAttributes();
        for (int x = 0; x < attributes.getLength(); x++)
        {
            Attr attribute = (Attr) attributes.item(x);
            String oldName = CoreXMLUtils.attributeName(attribute);
            //TODO How can I use bestGuessName
            String name = beanPropertyConfiguration.translateName(oldName);
            String value = beanPropertyConfiguration.translateValue(oldName, attribute.getNodeValue());
            if (oldName.endsWith(ATTRIBUTE_REF_SUFFIX))
            {
                values.put(name, new RuntimeBeanReference(attribute.getNodeValue()));
            }
            else
            {
                values.put(name, value);
            }
        }
        builder.addPropertyValue("sourceMap", values);
        builder.addPropertyValue("targetMapClass", super.getBeanClass(element));
        postProcess(getBeanAssembler(element, builder), element);
    }

}
