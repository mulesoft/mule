/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.config;

import static org.mule.module.extensions.internal.config.XmlExtensionParserUtils.parseConfigName;
import static org.mule.module.extensions.internal.config.XmlExtensionParserUtils.toElementDescriptorBeanDefinition;
import org.mule.extensions.introspection.Configuration;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * Generic implementation of {@link org.springframework.beans.factory.xml.BeanDefinitionParser}
 * capable of parsing any {@link Configuration}
 * <p/>
 * It supports simple attributes, pojos, lists/sets of simple attributes, list/sets of beans,
 * and maps of simple attributes
 * <p/>
 * It the given config doesn't provide a name, then one will be automatically generated in order to register the config
 * in the {@link org.mule.api.registry.Registry}
 *
 * @since 3.7.0
 */
final class ConfigurationBeanDefinitionParser extends BaseExtensionBeanDefinitionParser
{

    private final Configuration configuration;

    ConfigurationBeanDefinitionParser(Configuration configuration)
    {
        super(ConfigurationFactoryBean.class);
        this.configuration = configuration;
    }

    @Override
    protected void doParse(BeanDefinitionBuilder builder, Element element)
    {
        parseConfigName(element, builder);
        builder.addConstructorArgValue(configuration);
        builder.addConstructorArgValue(toElementDescriptorBeanDefinition(element));
    }
}
