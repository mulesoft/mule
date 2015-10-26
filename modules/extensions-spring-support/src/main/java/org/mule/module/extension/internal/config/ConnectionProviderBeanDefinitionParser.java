/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_NAMESPACE;
import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.parseConnectionProviderName;
import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.toElementDescriptorBeanDefinition;
import org.mule.extension.api.introspection.ConnectionProviderModel;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Implementation of {@link BaseExtensionBeanDefinitionParser} capable of parsing instances
 * which are compliant with the model defined in a {@link #providerModel}. The outcome of
 * this parser will be a {@link ConnectionProviderModel}.
 * <p>
 * It supports simple attributes, pojos, lists/sets of simple attributes, list/sets of beans,
 * and maps of simple attributes.
 *
 * @since 4.0
 */
final class ConnectionProviderBeanDefinitionParser extends BaseExtensionBeanDefinitionParser
{

    private final ConnectionProviderModel providerModel;

    public ConnectionProviderBeanDefinitionParser(ConnectionProviderModel providerModel)
    {
        super(ConnectionProviderFactoryBean.class);
        this.providerModel = providerModel;
    }

    @Override
    protected void doParse(BeanDefinitionBuilder builder, Element element, ParserContext parserContext)
    {
        if (element.getParentNode().getNamespaceURI().equals(MULE_NAMESPACE))
        {
            parseConnectionProviderName(element, builder);
        }

        builder.addConstructorArgValue(providerModel);
        builder.addConstructorArgValue(toElementDescriptorBeanDefinition(element));
    }
}
