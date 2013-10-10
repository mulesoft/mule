/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
