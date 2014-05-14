/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.config;

import static org.mule.module.extensions.internal.config.XmlExtensionParserUtils.applyLifecycle;
import static org.mule.module.extensions.internal.config.XmlExtensionParserUtils.setNoRecurseOnDefinition;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

abstract class BaseExtensionBeanDefinitionParser implements BeanDefinitionParser
{

    private final Class<?> type;

    BaseExtensionBeanDefinitionParser(Class<?> type)
    {
        this.type = type;
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext)
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(type);
        builder.setScope(BeanDefinition.SCOPE_SINGLETON);

        doParse(builder, element);

        applyLifecycle(builder);
        BeanDefinition definition = builder.getBeanDefinition();
        setNoRecurseOnDefinition(definition);

        return definition;
    }

    protected abstract void doParse(BeanDefinitionBuilder builder, Element element);
}
