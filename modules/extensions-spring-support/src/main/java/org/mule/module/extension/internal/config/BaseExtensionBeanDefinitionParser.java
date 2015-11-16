/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.setNoRecurseOnDefinition;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
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
     * Creates a new instance which will generate instances of {@code type}
     *
     * @param type a {@link Class}
     */
    BaseExtensionBeanDefinitionParser(Class<?> type)
    {
        this.type = type;
    }

    /**
     * Creates and returns a singleton {@link BeanDefinition}. Actual parsing
     * is delegated to the {@link #doParse(BeanDefinitionBuilder, Element, ParserContext)}
     * method
     *
     * @param element       a {@link Element}
     * @param parserContext the current {@link ParserContext}
     * @return a {@link BeanDefinition}
     */
    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext)
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(type);
        builder.setScope(BeanDefinition.SCOPE_SINGLETON);
        doParse(builder, element, parserContext);

        BeanDefinition definition = builder.getBeanDefinition();
        setNoRecurseOnDefinition(definition);

        return definition;
    }

    /**
     * Performs component specific parsing logic.
     *
     * @param builder       a {@link BeanDefinitionBuilder}
     * @param element       the {@link Element} being parsed
     * @param parserContext the current{@link ParserContext}
     */
    protected abstract void doParse(BeanDefinitionBuilder builder, Element element, ParserContext parserContext);
}
