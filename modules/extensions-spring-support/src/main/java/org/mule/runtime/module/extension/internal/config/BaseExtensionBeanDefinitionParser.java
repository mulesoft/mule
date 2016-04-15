/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

import com.google.common.collect.ImmutableList;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Base class for {@link BeanDefinitionParser}s that process elements
 * of components developed using the Extensions API
 *
 * @since 3.7.0
 */
abstract class BaseExtensionBeanDefinitionParser implements BeanDefinitionParser
{

    /**
     * The type of the instance to be generated
     */
    private final Class<?> type;

    /**
     * Delegate used for handling XML parsing
     */
    protected XmlExtensionParserDelegate parserDelegate;

    /**
     * An ordered {@link List} of {@link InfrastructureParserDelegate}.
     * For any given {@link Element}, only the first matching delegate
     * will be used, hence order is important
     */
    private final List<InfrastructureParserDelegate> infrastructureParsers;

    /**
     * Creates a new instance which will generate instances of {@code type}
     *
     * @param type a {@link Class}
     */
    BaseExtensionBeanDefinitionParser(Class<?> type)
    {
        this.type = type;
        infrastructureParsers = ImmutableList.of(new PoolingProfileInfrastructureParser(),
                                                 new RetryPolicyInfrastructureParser(),
                                                 new TlsContextInfrastructureParser(),
                                                 new ThreadingProfileInfrastructureParser());
    }

    /**
     * Creates and returns a singleton {@link BeanDefinition}. Actual parsing
     * is delegated to the {@link #doParse(BeanDefinitionBuilder, Element, XmlExtensionParserDelegate, ParserContext)}
     * method
     *
     * @param element       a {@link Element}
     * @param parserContext the current {@link ParserContext}
     * @return a {@link BeanDefinition}
     */
    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext)
    {
        XmlExtensionParserDelegate parserDelegate = new XmlExtensionParserDelegate();
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(type);
        builder.setScope(SCOPE_SINGLETON);
        doParse(builder, element, parserDelegate, parserContext);

        BeanDefinition definition = builder.getBeanDefinition();
        parserDelegate.setNoRecurseOnDefinition(definition);
        injectParseDelegate(builder, element, parserContext);

        return definition;
    }

    /**
     * Performs component specific parsing logic.
     *
     * @param builder        a {@link BeanDefinitionBuilder}
     * @param element        the {@link Element} being parsed
     * @param parserDelegate A {@link XmlExtensionParserDelegate} to assist with the parsing
     * @param parserContext  the current{@link ParserContext}
     */
    protected abstract void doParse(BeanDefinitionBuilder builder, Element element, XmlExtensionParserDelegate parserDelegate, ParserContext parserContext);

    private void injectParseDelegate(BeanDefinitionBuilder builder, Element element, ParserContext parserContext)
    {
        if (HasExtensionParserDelegate.class.isAssignableFrom(type))
        {
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(XmlExtensionParserDelegate.class);
            beanDefinitionBuilder.addPropertyValue("infrastructureParameters", getInfrastructureChilds(element, parserContext));

            builder.addPropertyValue("parserDelegate", beanDefinitionBuilder.getBeanDefinition());
        }
    }

    private ManagedMap<Class<?>, BeanDefinition> getInfrastructureChilds(Element element, ParserContext parserContext)
    {
        ManagedMap<Class<?>, BeanDefinition> parameters = new ManagedMap<>();

        for (Element childElement : DomUtils.getChildElements(element))
        {
            infrastructureParsers.stream()
                    .filter(parser -> parser.accepts(childElement))
                    .findFirst()
                    .ifPresent(parser -> parser.parse(childElement, parameters, parserContext));
        }

        return parameters;
    }
}
