/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_EXTENSION_CONNECTION_PROVIDER_TYPE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_EXTENSION_NAMESPACE;
import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.parseConfigName;
import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.toElementDescriptorBeanDefinition;
import static org.w3c.dom.TypeInfo.DERIVATION_EXTENSION;
import org.mule.api.registry.Registry;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.module.extension.internal.runtime.resolver.ImplicitConnectionProviderValueResolver;
import org.mule.module.extension.internal.runtime.resolver.StaticValueResolver;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Implementation of {@link BaseExtensionBeanDefinitionParser} capable of parsing instances
 * which are compliant with the model defined in a {@link #configurationModel}. The outcome of
 * this parser will be a {@link ConfigurationProviderFactoryBean}.
 * <p>
 * It supports simple attributes, pojos, lists/sets of simple attributes, list/sets of beans,
 * and maps of simple attributes
 * <p>
 * It the given config doesn't provide a name, then one will be automatically generated in order to register the config
 * in the {@link Registry}
 *
 * @since 3.7.0
 */
final class ConfigurationBeanDefinitionParser extends BaseExtensionBeanDefinitionParser
{

    private static final String CONNECTION_PROVIDER_ATTRIBUTE_NAME = "provider";

    private final ConfigurationModel configurationModel;

    ConfigurationBeanDefinitionParser(ConfigurationModel configurationModel)
    {
        super(ConfigurationProviderFactoryBean.class);
        this.configurationModel = configurationModel;
    }

    @Override
    protected void doParse(BeanDefinitionBuilder builder, Element element, ParserContext parserContext)
    {
        String name = parseConfigName(element, builder);

        builder.addConstructorArgValue(configurationModel);
        builder.addConstructorArgValue(toElementDescriptorBeanDefinition(element));
        builder.addConstructorArgReference(OBJECT_MULE_CONTEXT);
        builder.addConstructorArgReference(OBJECT_TIME_SUPPLIER);

        parseConfigurationProvider(name, element, builder, parserContext);
    }

    private void parseConfigurationProvider(String configName, Element element, BeanDefinitionBuilder builder, ParserContext parserContext)
    {
        if (configurationModel.getExtensionModel().getConnectionProviders().isEmpty())
        {
            builder.addConstructorArgValue(new StaticValueResolver<>(null));
            return;
        }

        String providerReference = getConnectionProviderReference(element);
        if (!StringUtils.isEmpty(providerReference))
        {
            builder.addConstructorArgReference(providerReference);
            return;
        }

        for (Element childElement : DomUtils.getChildElements(element))
        {
            if (childElement.getSchemaTypeInfo().isDerivedFrom(MULE_EXTENSION_NAMESPACE, MULE_EXTENSION_CONNECTION_PROVIDER_TYPE.getLocalPart(), DERIVATION_EXTENSION))
            {
                NamespaceHandler namespaceHandler = parserContext.getReaderContext().getNamespaceHandlerResolver().resolve(parserContext.getDelegate().getNamespaceURI(element));
                if (!(namespaceHandler instanceof ExtensionNamespaceHandler))
                {
                    continue;
                }

                BeanDefinitionParser providerParser = ((ExtensionNamespaceHandler) namespaceHandler).getParser(parserContext.getDelegate().getLocalName(childElement));
                if (providerParser == null)
                {
                    continue;
                }

                builder.addConstructorArgValue(providerParser.parse(childElement, parserContext));
                return;
            }
        }

        builder.addConstructorArgValue(new ImplicitConnectionProviderValueResolver(configName, configurationModel.getExtensionModel()));
    }


    private String getConnectionProviderReference(Element element)
    {
        return element.hasAttribute(CONNECTION_PROVIDER_ATTRIBUTE_NAME)
               ? element.getAttribute(CONNECTION_PROVIDER_ATTRIBUTE_NAME)
               : null;
    }
}
