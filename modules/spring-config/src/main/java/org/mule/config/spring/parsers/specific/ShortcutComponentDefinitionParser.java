/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.object.AbstractObjectFactory;
import org.mule.object.PrototypeObjectFactory;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class ShortcutComponentDefinitionParser extends ComponentDefinitionParser
{

    private static Class OBJECT_FACTORY_TYPE = PrototypeObjectFactory.class;

    public ShortcutComponentDefinitionParser(Class clazz)
    {
        super(clazz);
    }

    @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        String className = element.getAttributeNode(AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS).getValue();

        GenericBeanDefinition objectFactoryBeanDefinition = new GenericBeanDefinition();
        objectFactoryBeanDefinition.setBeanClass(OBJECT_FACTORY_TYPE);
        objectFactoryBeanDefinition.getPropertyValues().addPropertyValue(AbstractObjectFactory.ATTRIBUTE_OBJECT_CLASS_NAME, className);

        builder.addPropertyValue("objectFactory", objectFactoryBeanDefinition);
        super.parseChild(element, parserContext, builder);
    }
}
