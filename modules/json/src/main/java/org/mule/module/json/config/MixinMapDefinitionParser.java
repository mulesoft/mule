/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.config;

import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.collection.OrphanMapDefinitionParser;
import org.mule.util.ClassUtils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * TODO
 */
public class MixinMapDefinitionParser extends OrphanMapDefinitionParser
{
    public MixinMapDefinitionParser(Class mapClass)
    {
        super(mapClass);
    }

    public MixinMapDefinitionParser(Class mapClass, String name)
    {
        super(mapClass, name);
    }

    public MixinMapDefinitionParser(Class mapClass, String name, boolean attribute)
    {
        super(mapClass, name, attribute);
    }

    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        //Map parsedMap = parserContext.getDelegate().parseMapElement(element, builder.getRawBeanDefinition());
        Map<Class, Class> mixins = new HashMap<Class, Class>();

        for (int i = 0; i < element.getChildNodes().getLength(); i++)
        {
            Node node = element.getChildNodes().item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE)
            {
                String mixinClass = node.getAttributes().getNamedItem("mixinClass").getTextContent();
                String targetClass = node.getAttributes().getNamedItem("targetClass").getTextContent();
                try
                {
                    mixins.put(ClassUtils.loadClass(targetClass, getClass()), ClassUtils.loadClass(mixinClass, getClass()));
                }
                catch (ClassNotFoundException e)
                {
                    throw new BeanCreationException("Failed to create Mixin map", e);
                }
            }
        }
        builder.addPropertyValue("sourceMap", mixins);
        builder.addPropertyValue("targetMapClass", mapClass.getName());
        getBeanAssembler(element, builder).setBeanFlag(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE);
    }

}
