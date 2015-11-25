/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributesAndChildren;
import org.mule.config.spring.parsers.processors.CheckRequiredAttributesWhenNoChildren;
import org.mule.expression.ExpressionConfig;
import org.mule.expression.transformers.ExpressionArgument;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This allows a message processor to be defined globally, or embedded within an
 * endpoint. (as either a normal or response processor).
 */
public class ExpressionTransformerDefinitionParser extends ParentContextDefinitionParser
{

    public ExpressionTransformerDefinitionParser(Class messageProcessor)
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new ExpressionTransformerOrphanDefinitionParser(
            messageProcessor, false));
        otherwise(new ExpressionTransformerChildDefinitionParser("messageProcessor", messageProcessor));

        registerPreProcessor(new CheckRequiredAttributesWhenNoChildren(new String[][]{{"expression"}},
                "return-argument", "http://www.mulesoft.org/schema/mule/core")).registerPreProcessor(
            new CheckExclusiveAttributesAndChildren(new String[]{"expression"},
                new String[]{"return-argument"}))
            .addIgnored("evaluator")
            .addIgnored("expression")
            .addIgnored("custom-evaluator");
    }

    protected static void addExpressionArgumentFromAttributes(Element element, BeanDefinitionBuilder builder)
    {
        if (element.getAttributeNode("expression") != null)
        {
            GenericBeanDefinition objectFactoryBeanDefinition = new GenericBeanDefinition();
            objectFactoryBeanDefinition.setBeanClass(ExpressionArgument.class);
            objectFactoryBeanDefinition.getPropertyValues().addPropertyValue("name", "single");
            objectFactoryBeanDefinition.getPropertyValues().addPropertyValue("optional", false);
            GenericBeanDefinition objectFactoryBeanDefinition2 = new GenericBeanDefinition();
            objectFactoryBeanDefinition2.setBeanClass(ExpressionConfig.class);
            objectFactoryBeanDefinition2.getPropertyValues().addPropertyValue("evaluator",
                element.getAttribute("evaluator"));
            objectFactoryBeanDefinition2.getPropertyValues().addPropertyValue("customEvaluator",
                element.getAttribute("custom-evaluator"));
            objectFactoryBeanDefinition2.getPropertyValues().addPropertyValue("expression",
                element.getAttribute("expression"));
            objectFactoryBeanDefinition.getPropertyValues().addPropertyValue("expressionConfig",
                objectFactoryBeanDefinition2);
            ManagedList list = new ManagedList<ExpressionArgument>(1);
            list.add(objectFactoryBeanDefinition2);
            builder.getBeanDefinition().getPropertyValues().addPropertyValue("arguments",
                objectFactoryBeanDefinition);
        }
    }

    static class ExpressionTransformerChildDefinitionParser extends ChildDefinitionParser
    {

        public ExpressionTransformerChildDefinitionParser(String string, Class messageProcessor)
        {
            super(string, messageProcessor);
        }

        @Override
        protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
        {
            addExpressionArgumentFromAttributes(element, builder);
            super.parseChild(element, parserContext, builder);
        }
    }

    static class ExpressionTransformerOrphanDefinitionParser extends MuleOrphanDefinitionParser
    {

        public ExpressionTransformerOrphanDefinitionParser(Class<?> beanClass, boolean singleton)
        {
            super(beanClass, singleton);
        }

        @Override
        protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder)
        {
            addExpressionArgumentFromAttributes(element, builder);
            super.doParse(element, context, builder);
        }
    }
}
