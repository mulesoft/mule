/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.api.config.PoolingProfile;
import org.mule.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.extension.api.introspection.connection.RuntimeConnectionProviderModel;
import org.mule.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBuilderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import javax.inject.Inject;

import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} which returns a {@link ValueResolver} that yields instances
 * of {@link ConnectionProvider} which are compliant with a {@link ConnectionProviderModel}.
 * <p>
 * Subsequent invokations to {@link #getObject()} method
 * returns always the same {@link ValueResolver}.
 *
 * @since 4.0
 */
public class ConnectionProviderFactoryBean extends ExtensionComponentFactoryBean<ValueResolver>
{

    private final ElementDescriptor element;
    private final RuntimeConnectionProviderModel providerModel;
    private boolean disableValidation = false;

    @Inject
    private ConnectionManagerAdapter connectionManager;

    public ConnectionProviderFactoryBean(RuntimeExtensionModel extensionModel, RuntimeConnectionProviderModel providerModel, ElementDescriptor element)
    {
        this.providerModel = providerModel;
        this.element = element;
        this.extensionModel = extensionModel;
    }

    /**
     * @return A {@link ValueResolver} which yields instances of {@link ConnectionProvider}
     */
    @Override
    public synchronized ValueResolver getObject() throws Exception
    {
        ResolverSet resolverSet;
        try
        {
            resolverSet = parserDelegate.getResolverSet(element, providerModel.getParameterModels());
        }
        catch (ConfigurationException e)
        {
            throw new MuleRuntimeException(createStaticMessage("Could not parse connection provider"), e);
        }

        PoolingProfile poolingProfile = parserDelegate.getInfrastructureParameter(PoolingProfile.class);
        RetryPolicyTemplate retryPolicyTemplate = parserDelegate.getInfrastructureParameter(RetryPolicyTemplate.class);

        return new ObjectBuilderValueResolver<>(new ConnectionProviderObjectBuilder(providerModel, resolverSet, poolingProfile, disableValidation, retryPolicyTemplate, connectionManager));
    }

    /**
     * @return {@link ValueResolver}
     */
    @Override
    public Class<?> getObjectType()
    {
        return ValueResolver.class;
    }

    /**
     * @return {@code true}
     */
    @Override
    public boolean isSingleton()
    {
        return true;
    }

    public void setDisableValidation(boolean disableValidation)
    {
        this.disableValidation = disableValidation;
    }
}
