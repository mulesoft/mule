/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
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
        objectFactoryBeanDefinition.getPropertyValues().addPropertyValue(
            AbstractObjectFactory.ATTRIBUTE_OBJECT_CLASS_NAME, className);
        //MArker for MULE-4813
        objectFactoryBeanDefinition.setInitMethodName(Initialisable.PHASE_NAME);
        objectFactoryBeanDefinition.setDestroyMethodName(Disposable.PHASE_NAME);

        builder.addPropertyValue("objectFactory", objectFactoryBeanDefinition);
        super.parseChild(element, parserContext, builder);
    }
}
