/*
 * $Id:ConfigurationDefinitionParser.java 5187 2007-02-16 18:00:42Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.api.MuleRuntimeException;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.spring.parsers.PostProcessor;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
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

    public ConfigurationDefinitionParser()
    {
        super(MuleProperties.OBJECT_MULE_CONFIGURATION);
        addIgnored(DEFAULT_EXCEPTION_STRATEGY_ATTRIBUTE);
        registerPostProcessor(new PostProcessor()
        {
            @Override
            public void postProcess(ParserContext context, BeanAssembler assembler, Element element)
            {
                AbstractBeanDefinition beanDefinition = assembler.getBean().getBeanDefinition();
                if (beanDefinition.hasAttribute(DEFAULT_EXCEPTION_STRATEGY_ATTRIBUTE))
                {
                    Object defaultExceptionStrategyBeanName = beanDefinition.getAttribute(DEFAULT_EXCEPTION_STRATEGY_ATTRIBUTE);
                    if (!context.getRegistry().containsBeanDefinition((String) defaultExceptionStrategyBeanName))
                    {
                        throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format("No global exception strategy defined with name %s.",defaultExceptionStrategyBeanName)));
                    }
                    BeanDefinition defaultExceptionStrategyBeanDefinition = context.getRegistry().getBeanDefinition(element.getAttribute(DEFAULT_EXCEPTION_STRATEGY_ATTRIBUTE));
                    if (defaultExceptionStrategyBeanDefinition.hasAttribute("expression"))
                    {
                        throw new MuleRuntimeException(CoreMessages.createStaticMessage("Default exception strategy must not have expression attribute. It must accept any message."));
                    }
                }
            }
        });
        singleton=true;
    }

    protected Class getBeanClass(Element element)
    {
        return MuleConfiguration.class;
    }


    @Override
    protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder)
    {
        if (element.hasAttribute(DEFAULT_EXCEPTION_STRATEGY_ATTRIBUTE))
        {
            builder.addPropertyValue("defaultExceptionStrategyName", element.getAttribute(DEFAULT_EXCEPTION_STRATEGY_ATTRIBUTE));
        }
        super.doParse(element,context,builder);
    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException
    {
        return MuleProperties.OBJECT_MULE_CONFIGURATION;
    }

}
