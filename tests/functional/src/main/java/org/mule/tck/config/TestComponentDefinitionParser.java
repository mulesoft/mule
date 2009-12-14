/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.config;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.component.DefaultJavaComponent;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.object.AbstractObjectFactory;
import org.mule.object.SingletonObjectFactory;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Configures a FunctionalTestComponent wrapped as a JavaComponent.  This parser provides a short form way of
 * configuring a test component in Mule.
 */
//TODO This should really extend StaticComponentDefinitionParser from mule-core as it is quite similar.
public class TestComponentDefinitionParser extends ComponentDefinitionParser
{
    private static Class OBJECT_FACTORY_TYPE = SingletonObjectFactory.class;
    private Class componentInstanceClass = FunctionalTestComponent.class;

    public TestComponentDefinitionParser()
    {
        super(DefaultJavaComponent.class);
        addIgnored("appendString");
        addIgnored("enableMessageHistory");
        addIgnored("enableNotifications");
        addIgnored("throwException");
        addIgnored("exceptionToThrow");
        addIgnored("waitTime");
        addIgnored("doInboundTransform");
        addIgnored("logMessageDetails");
    }

    public TestComponentDefinitionParser(Class componentInstanceClass)
    {
        this();
        this.componentInstanceClass = componentInstanceClass;
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        Element parent = (Element) element.getParentNode();
        String serviceName = parent.getAttribute(ATTRIBUTE_NAME);
        builder.addPropertyReference("service", serviceName);

        // Create a BeanDefinition for the nested object factory and set it a
        // property value for the component
        AbstractBeanDefinition objectFactoryBeanDefinition = new GenericBeanDefinition();
        objectFactoryBeanDefinition.setBeanClass(OBJECT_FACTORY_TYPE);
        objectFactoryBeanDefinition.getPropertyValues().addPropertyValue(AbstractObjectFactory.ATTRIBUTE_OBJECT_CLASS,
                componentInstanceClass);
        objectFactoryBeanDefinition.setInitMethodName(Initialisable.PHASE_NAME);
        objectFactoryBeanDefinition.setDestroyMethodName(Disposable.PHASE_NAME);
        Map props = new HashMap();
        for (int i = 0; i < element.getAttributes().getLength(); i++)
        {
            Node n = element.getAttributes().item(i);
                props.put(n.getLocalName(), n.getNodeValue());
        }
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
            else if ("callback".equals(list.item(i).getLocalName()))
            {
                Element ele = (Element) list.item(i);
                String c = ele.getAttribute("class");
                try
                {
                    EventCallback cb = (EventCallback)ClassUtils.instanciateClass(c);
                    props.put("eventCallback", cb);

                }
                catch (Exception e)
                {
                    throw new BeanCreationException("Failed to load event-callback: " + c, e);
                }
            }

        }

        if (returnData != null)
        {
            props.put("returnData", returnData);
        }
        objectFactoryBeanDefinition.getPropertyValues().addPropertyValue("properties", props);

        builder.addPropertyValue("objectFactory", objectFactoryBeanDefinition);

        super.parseChild(element, parserContext, builder);
    }

}
