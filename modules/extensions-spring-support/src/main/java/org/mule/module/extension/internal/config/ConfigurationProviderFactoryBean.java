/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.getResolverSet;
import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.extension.api.connection.ConnectionProvider;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.runtime.ConfigurationProvider;
import org.mule.extension.api.runtime.ExpirationPolicy;
import org.mule.module.extension.internal.runtime.DynamicConfigPolicy;
import org.mule.module.extension.internal.runtime.ImmutableExpirationPolicy;
import org.mule.module.extension.internal.runtime.config.ConfigurationProviderFactory;
import org.mule.module.extension.internal.runtime.config.DefaultConfigurationProviderFactory;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.time.TimeSupplier;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} which returns a {@link ConfigurationProvider} that provides the actual instances
 * that implement a given {@link ConfigurationModel}. Subsequent invokations to {@link #getObject()} method
 * returns always the same {@link ConfigurationProvider}.
 *
 * @since 3.7.0
 */
final class ConfigurationProviderFactoryBean implements FactoryBean<ConfigurationProvider<Object>>
{
    private final ConfigurationProvider<Object> configurationProvider;
    private final ConfigurationProviderFactory configurationProviderFactory = new DefaultConfigurationProviderFactory();
    private final TimeSupplier timeSupplier;

    ConfigurationProviderFactoryBean(String name,
                                     ConfigurationModel configurationModel,
                                     ElementDescriptor element,
                                     MuleContext muleContext,
                                     TimeSupplier timeSupplier,
                                     ValueResolver<ConnectionProvider> connectionProviderResolver) throws ConfigurationException
    {
        this.timeSupplier = timeSupplier;
        ResolverSet resolverSet = getResolverSet(element, configurationModel.getParameterModels());
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
    }

    @Override
    public ConfigurationProvider<Object> getObject() throws Exception
    {
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
