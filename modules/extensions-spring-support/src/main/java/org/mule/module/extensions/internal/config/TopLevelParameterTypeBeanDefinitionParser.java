/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.config;

import static org.mule.module.extensions.internal.config.XmlExtensionParserUtils.toElementDescriptorBeanDefinition;
import org.mule.extensions.introspection.DataType;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

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
    }
}
