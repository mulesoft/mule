/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.parseConfigName;
import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.toElementDescriptorBeanDefinition;
import org.mule.api.registry.Registry;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * Implementation of {@link BaseExtensionBeanDefinitionParser} capable of parsing instances
 * which are compliant with the model defined in a {@link Configuration}. The outcome of
 * this parser will be a {@link ConfigurationInstanceProviderFactoryBean}.
 * <p/>
 * It supports simple attributes, pojos, lists/sets of simple attributes, list/sets of beans,
 * and maps of simple attributes
 * <p/>
 * It the given config doesn't provide a name, then one will be automatically generated in order to register the config
 * in the {@link Registry}
 *
 * @since 3.7.0
 */
final class ConfigurationBeanDefinitionParser extends BaseExtensionBeanDefinitionParser
{

    private final Extension extension;
    private final Configuration configuration;

    ConfigurationBeanDefinitionParser(Extension extension, Configuration configuration)
    {
        super(ConfigurationInstanceProviderFactoryBean.class);
        this.extension = extension;
        this.configuration = configuration;
    }

    @Override
    protected void doParse(BeanDefinitionBuilder builder, Element element)
    {
        parseConfigName(element, builder);

        builder.addConstructorArgValue(extension);
        builder.addConstructorArgValue(configuration);
        builder.addConstructorArgValue(toElementDescriptorBeanDefinition(element));
        builder.addConstructorArgReference(OBJECT_MULE_CONTEXT);
    }
}
