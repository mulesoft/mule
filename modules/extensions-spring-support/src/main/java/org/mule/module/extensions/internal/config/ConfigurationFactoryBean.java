/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.config;

import static org.mule.module.extensions.internal.config.XmlExtensionParserUtils.getResolverSet;
import org.mule.extensions.introspection.Configuration;
import org.mule.module.extensions.internal.runtime.resolver.ConfigurationValueResolver;

import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} which returns a {@link ConfigurationValueResolver} that provides the actual instances
 * that implement a given {@link Configuration}. Subsequent invokations to {@link #getObject()} method
 * returns always the same {@link ConfigurationValueResolver}.
 *
 * @since 3.7.0
 */
final class ConfigurationFactoryBean extends BaseResolverFactoryBean<ConfigurationValueResolver>
{

    private final Configuration configuration;

    ConfigurationFactoryBean(String name, Configuration configuration, ElementDescriptor element)
    {
        super(name, element);
        this.configuration = configuration;
        valueResolver = createValueResolver();
    }

    @Override
    protected ConfigurationValueResolver createValueResolver()
    {
        return new ConfigurationValueResolver(name, configuration, getResolverSet(element, configuration.getParameters()));
    }

    /**
     * @return {@link ConfigurationValueResolver}
     */
    @Override
    public Class<ConfigurationValueResolver> getObjectType()
    {
        return ConfigurationValueResolver.class;
    }
}
