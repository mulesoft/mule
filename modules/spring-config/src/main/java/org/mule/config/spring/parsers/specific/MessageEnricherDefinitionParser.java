/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.enricher.MessageEnricher.EnrichExpressionPair;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class MessageEnricherDefinitionParser extends ChildDefinitionParser
{

    public MessageEnricherDefinitionParser(String setterMethod, Class clazz)
    {
        super(setterMethod, clazz);
    }

    @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        if (!StringUtils.isEmpty(element.getAttribute("source")) || !StringUtils.isEmpty(element.getAttribute("target")))
        {
            GenericBeanDefinition objectFactoryBeanDefinition = new GenericBeanDefinition();
            objectFactoryBeanDefinition.setBeanClass(EnrichExpressionPair.class);
            objectFactoryBeanDefinition.getPropertyValues().addPropertyValue("source",
                element.getAttribute("source"));
            objectFactoryBeanDefinition.getPropertyValues().addPropertyValue("target",
                element.getAttribute("target"));
            ManagedList<GenericBeanDefinition> list = new ManagedList<GenericBeanDefinition>();
            list.add(objectFactoryBeanDefinition);
            builder.addPropertyValue("enrichExpressionPairs", list);
        }

        super.parseChild(element, parserContext, builder);
    }
}
