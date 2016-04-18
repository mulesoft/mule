/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.module.extension.internal.runtime.DynamicConfigPolicy;
import org.mule.runtime.module.extension.internal.runtime.ImmutableExpirationPolicy;
import org.mule.runtime.module.extension.internal.runtime.config.ConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.core.time.TimeSupplier;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} which returns a {@link ConfigurationProvider} that provides the actual instances
 * that implement a given {@link ConfigurationModel}. Subsequent invokations to {@link #getObject()} method
 * returns always the same {@link ConfigurationProvider}.
 *
 * @since 3.7.0
 */
final class ConfigurationProviderFactoryBean extends ExtensionComponentFactoryBean<ConfigurationProvider<Object>>
{

    private final String name;
    private final RuntimeConfigurationModel configurationModel;
    private final ElementDescriptor element;
    private final ConfigurationProviderFactory configurationProviderFactory = new DefaultConfigurationProviderFactory();
    private final TimeSupplier timeSupplier;
    private final ValueResolver<ConnectionProvider> connectionProviderResolver;
    private final MuleContext muleContext;

    ConfigurationProviderFactoryBean(String name,
                                     RuntimeConfigurationModel configurationModel,
                                     ElementDescriptor element,
                                     MuleContext muleContext,
                                     TimeSupplier timeSupplier,
                                     ValueResolver<ConnectionProvider> connectionProviderResolver) throws ConfigurationException
    {
        this.name = name;
        this.configurationModel = configurationModel;
        this.element = element;
        this.connectionProviderResolver = connectionProviderResolver;
        this.timeSupplier = timeSupplier;
        this.muleContext = muleContext;
        this.extensionModel = configurationModel.getExtensionModel();
    }

    @Override
    public ConfigurationProvider<Object> getObject() throws Exception
    {
        ResolverSet resolverSet = parserDelegate.getResolverSet(element, configurationModel.getParameterModels());
        ConfigurationProvider<Object> configurationProvider;
        try
        {
            if (resolverSet.isDynamic() || connectionProviderResolver.isDynamic())
            {
                configurationProvider = configurationProviderFactory.createDynamicConfigurationProvider(
                        name,
                        configurationModel,
                        resolverSet,
                        connectionProviderResolver,
                        getDynamicConfigPolicy(element));
            }
            else
            {
                configurationProvider = configurationProviderFactory.createStaticConfigurationProvider(
                        name,
                        configurationModel,
                        resolverSet,
                        connectionProviderResolver,
                        muleContext);
            }

            muleContext.getInjector().inject(configurationProvider);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return configurationProvider;
    }

    /**
     * @return {@link ConfigurationProvider}
     */
    @Override
    public Class<ConfigurationProvider> getObjectType()
    {
        return ConfigurationProvider.class;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }

    private DynamicConfigPolicy getDynamicConfigPolicy(ElementDescriptor element)
    {
        ElementDescriptor policyElement = element.getChildByName("dynamic-config-policy");
        return policyElement == null ? DynamicConfigPolicy.getDefault(timeSupplier)
                                     : new DynamicConfigPolicy(getExpirationPolicy(policyElement));
    }

    private ExpirationPolicy getExpirationPolicy(ElementDescriptor dynamicConfigPolicyElement)
    {
        ElementDescriptor expirationPolicyElement = dynamicConfigPolicyElement.getChildByName("expiration-policy");
        if (expirationPolicyElement == null)
        {
            return ImmutableExpirationPolicy.getDefault(timeSupplier);
        }

        return new ImmutableExpirationPolicy(
                Long.valueOf(expirationPolicyElement.getAttribute("maxIdleTime")),
                TimeUnit.valueOf(expirationPolicyElement.getAttribute("timeUnit")),
                timeSupplier);
    }
}
