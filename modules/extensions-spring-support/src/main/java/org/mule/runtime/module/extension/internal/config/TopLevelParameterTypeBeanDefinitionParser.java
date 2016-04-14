/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import static org.mule.runtime.config.spring.parsers.specific.NameConstants.MULE_NAMESPACE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * An implementation of {@link BaseExtensionBeanDefinitionParser} that parses
 * objects that are to be used as a parameter of a configuration or operation
 * and for which we want support for defining them as top level elementes.
 * <p/>
 * It produces a {@link TopLevelParameterTypeFactoryBean} which will produce the
 * actual instances. The type of the parameter to produce is represented by
 * a {@link #metadataType}
 *
 * @since 3.7.0
 */
final class TopLevelParameterTypeBeanDefinitionParser extends BaseExtensionBeanDefinitionParser
{
    private final MetadataType metadataType;

    TopLevelParameterTypeBeanDefinitionParser(MetadataType metadataType)
    {
        super(TopLevelParameterTypeFactoryBean.class);
        this.metadataType = metadataType;
    }

    @Override
    protected void doParse(BeanDefinitionBuilder builder, Element element, XmlExtensionParserDelegate parserDelegate, ParserContext parserContext)
    {
        if (StringUtils.isBlank(element.getAttribute("name")) && !element.getNamespaceURI().equals(MULE_NAMESPACE))
        {
            throw new IllegalModelDefinitionException(String.format("Element %s must should have a [name] attribute", element.getTagName()));
        }
        builder.addConstructorArgValue(parserDelegate.toElementDescriptorBeanDefinition(element));
        builder.addConstructorArgValue(metadataType);
        builder.addConstructorArgReference(OBJECT_MULE_CONTEXT);
    }
}
