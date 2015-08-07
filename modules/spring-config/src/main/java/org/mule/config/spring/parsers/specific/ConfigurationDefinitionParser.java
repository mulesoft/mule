/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import static org.mule.config.spring.util.ProcessingStrategyUtils.DEFAULT_PROCESSING_STRATEGY;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.spring.util.ProcessingStrategyUtils;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parses the <mule:configuration> element. If this element appears in multiple Xml config files each will its configuration
 * to a single {@link MuleConfiguration} object.
 *
 * @see MuleConfiguration
 */
public class ConfigurationDefinitionParser extends NamedDefinitionParser
{

    public static final String DEFAULT_EXCEPTION_STRATEGY_ATTRIBUTE = "defaultExceptionStrategy-ref";
    private static final String DEFAULT_OBJECT_SERIALIZER_ATTRIBUTE = "defaultObjectSerializer-ref";

    public ConfigurationDefinitionParser()
    {
        super(MuleProperties.OBJECT_MULE_CONFIGURATION);
        addIgnored(DEFAULT_EXCEPTION_STRATEGY_ATTRIBUTE);
        singleton=true;
    }

    protected Class getBeanClass(Element element)
    {
        return MuleConfiguration.class;
    }

    @Override
    protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder)
    {
        parseExceptionStrategy(element, builder);
        parseObjectSerializer(element, builder);
        ProcessingStrategyUtils.configureProcessingStrategy(element, builder, DEFAULT_PROCESSING_STRATEGY);

        super.doParse(element,context,builder);
    }

    private void parseExceptionStrategy(Element element, BeanDefinitionBuilder builder)
    {
        if (element.hasAttribute(DEFAULT_EXCEPTION_STRATEGY_ATTRIBUTE))
        {
            builder.addPropertyValue("defaultExceptionStrategyName", element.getAttribute(DEFAULT_EXCEPTION_STRATEGY_ATTRIBUTE));
        }
    }

    private void parseObjectSerializer(Element element, BeanDefinitionBuilder builder)
    {
        if (element.hasAttribute(DEFAULT_OBJECT_SERIALIZER_ATTRIBUTE))
        {
            builder.addPropertyReference("defaultObjectSerializer", element.getAttribute(DEFAULT_OBJECT_SERIALIZER_ATTRIBUTE));
        }
    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException
    {
        return MuleProperties.OBJECT_MULE_CONFIGURATION;
    }

}
