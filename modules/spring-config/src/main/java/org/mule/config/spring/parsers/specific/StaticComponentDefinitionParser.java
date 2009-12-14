/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.component.DefaultJavaComponent;
import org.mule.component.simple.StaticComponent;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * BDP which parses the <return-data> element of the <static-component>.
 */
public class StaticComponentDefinitionParser extends SimpleComponentDefinitionParser
{
    public StaticComponentDefinitionParser()
    {
        super(DefaultJavaComponent.class, StaticComponent.class);
    }

    @Override
    protected AbstractBeanDefinition getObjectFactoryDefinition(Element element)
    {
        AbstractBeanDefinition objectFactoryBeanDefinition = super.getObjectFactoryDefinition(element);
        
        String returnData = null;

        NodeList list = element.getChildNodes();
        for (int i = 0; i < list.getLength(); i++)
        {
            if ("return-data".equals(list.item(i).getLocalName()))
            {
                Element rData = (Element) list.item(i);
                if (StringUtils.isNotEmpty(rData.getAttribute("file")))
                {
                    String file = rData.getAttribute("file");
                    try
                    {
                        returnData = IOUtils.getResourceAsString(file, getClass());
                    }
                    catch (IOException e)
                    {
                        throw new BeanCreationException("Failed to load test-data resource: " + file, e);
                    }
                }
                else
                {
                    returnData = rData.getTextContent();
                }
            }
        }

        if (returnData != null)
        {
            Map props = new HashMap();
            props.put("data", returnData);
            objectFactoryBeanDefinition.getPropertyValues().addPropertyValue("properties", props);
        }
        
        return objectFactoryBeanDefinition;
    }
}
