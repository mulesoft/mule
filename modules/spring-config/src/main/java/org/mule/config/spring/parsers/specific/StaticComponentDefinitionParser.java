/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
