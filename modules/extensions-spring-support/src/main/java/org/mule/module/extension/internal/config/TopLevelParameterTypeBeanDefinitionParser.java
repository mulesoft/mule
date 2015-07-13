/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.toElementDescriptorBeanDefinition;
import org.mule.extension.introspection.DataType;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * An implementation of {@link BaseExtensionBeanDefinitionParser} that parses
 * objects that are to be used as a parameter of a configuration or operation
 * and for which we want support for defining them as top level elementes.
 * <p/>
 * It produces a {@link TopLevelParameterTypeFactoryBean} which will produce the
 * actual instances. The type of the parameter to produce is represented by
 * a {@link #dataType}
 *
 * @since 3.7.0
 */
final class TopLevelParameterTypeBeanDefinitionParser extends BaseExtensionBeanDefinitionParser
{
    private final DataType dataType;

    TopLevelParameterTypeBeanDefinitionParser(DataType dataType)
    {
        super(TopLevelParameterTypeFactoryBean.class);
        this.dataType = dataType;
    }

    @Override
    protected void doParse(BeanDefinitionBuilder builder, Element element)
    {
        builder.addConstructorArgValue(toElementDescriptorBeanDefinition(element));
        builder.addConstructorArgValue(dataType);
        builder.addConstructorArgReference(OBJECT_MULE_CONTEXT);
    }
}
