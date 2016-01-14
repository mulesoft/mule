/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.parseConfigRef;
import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.toElementDescriptorBeanDefinition;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;
import org.mule.api.source.MessageSource;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.SourceModel;
import org.mule.extension.api.runtime.source.Source;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * A {@link BeanDefinitionParser} to parse {@link MessageSource}
 * which execute instances of {@link Source}.
 * <p/>
 * It defines an {@link ExtensionMessageSourceFactoryBean} which in turn builds
 * the actual {@link MessageSource}
 *
 * @since 4.0
 */
final class SourceBeanDefinitionParser extends BaseExtensionBeanDefinitionParser
{

    private final ExtensionModel extensionModel;
    private final SourceModel sourceModel;

    SourceBeanDefinitionParser(ExtensionModel extensionModel, SourceModel sourceModel)
    {
        super(ExtensionMessageSourceFactoryBean.class);
        this.extensionModel = extensionModel;
        this.sourceModel = sourceModel;
    }

    @Override
    protected void doParse(BeanDefinitionBuilder builder, Element element, ParserContext parserContext)
    {
        builder.setScope(SCOPE_PROTOTYPE);

        builder.addConstructorArgValue(toElementDescriptorBeanDefinition(element))
                .addConstructorArgValue(extensionModel)
                .addConstructorArgValue(sourceModel);

        parseConfigRef(element, builder);
        builder.addConstructorArgReference(OBJECT_MULE_CONTEXT);
        attachSourceDefinition(parserContext, builder.getBeanDefinition());
    }

    private void attachSourceDefinition(ParserContext parserContext, BeanDefinition definition)
    {
        MutablePropertyValues propertyValues = parserContext.getContainingBeanDefinition().getPropertyValues();
        propertyValues.addPropertyValue("messageSource", definition);
    }
}
