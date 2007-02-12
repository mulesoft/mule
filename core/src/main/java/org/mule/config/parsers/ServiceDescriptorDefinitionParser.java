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

import org.mule.impl.MuleDescriptor;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Element;

/**
 * TODO
 */
public class ServiceDescriptorDefinitionParser extends AbstractChildBeanDefinitionParser
{

        protected Class getBeanClass(Element element)
        {
            return MuleDescriptor.class;
        }

        public boolean isCollection(Element element)
        {
            Element parent = (Element)element.getParentNode();
            if(parent.getNodeName().equals("beans"))
            {
                return false;
            }
            return true;
        }

        public String getPropertyName(Element e)
        {
            return "serviceDescriptor";
        }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        registry = parserContext.getRegistry();
        NamedNodeMap attributes = element.getAttributes();
        for (int x = 0; x < attributes.getLength(); x++)
        {
            Attr attribute = (Attr) attributes.item(x);
            String name = attribute.getName();
            if (ID_ATTRIBUTE.equals(name))
            {
                continue;
            }

            String propertyName = extractPropertyName(name);
            Assert.state(org.springframework.util.StringUtils.hasText(propertyName),
                    "Illegal property name returned from 'extractPropertyName(String)': cannot be null or empty.");
            builder.addPropertyValue(propertyName, attribute.getValue());
        }
        postProcess(builder, element);
    }


    protected void postProcess(BeanDefinitionBuilder builder, Element element)
    {
        String name = element.getAttribute("id");
        builder.addPropertyValue("name", name);
        super.postProcess(builder, element);
    }
}
