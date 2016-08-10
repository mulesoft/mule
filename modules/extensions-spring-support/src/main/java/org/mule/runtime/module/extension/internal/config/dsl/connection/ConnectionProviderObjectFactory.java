/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.connection;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.extension.api.introspection.connection.RuntimeConnectionProviderModel;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

/**
 * A {@link AbstractExtensionObjectFactory} that produces {@link ConnectionProviderResolver}
 * instances
 *
 * @since 4.0
 */
public class ConnectionProviderObjectFactory extends AbstractExtensionObjectFactory<ConnectionProviderResolver>
{
    private final RuntimeConnectionProviderModel providerModel;
    private MuleContext muleContext;

    private PoolingProfile poolingProfile = null;
    private RetryPolicyTemplate retryPolicyTemplate = null;
    private boolean disableValidation = false;

    public ConnectionProviderObjectFactory(RuntimeConnectionProviderModel providerModel, MuleContext muleContext)
    {
        this.providerModel = providerModel;
        this.muleContext = muleContext;
    }

    @Override
    public ConnectionProviderResolver getObject() throws Exception
    {
        ResolverSet resolverSet = getParametersAsResolverSet(providerModel);
        return new ConnectionProviderResolver(new ConnectionProviderObjectBuilder(providerModel, resolverSet, poolingProfile, disableValidation, retryPolicyTemplate, getConnectionManager()));
    }

    private ConnectionManagerAdapter getConnectionManager() throws ConfigurationException {
        try
        {
            return muleContext.getRegistry().lookupObject(ConnectionManagerAdapter.class);
        }
        catch (RegistrationException e)
        {
            throw new ConfigurationException(createStaticMessage("Could not obtain connection manager adapter form registry"), e);
        }
    }

    public void setPoolingProfile(PoolingProfile poolingProfile)
    {
        this.poolingProfile = poolingProfile;
    }

    public void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate)
    {
        this.retryPolicyTemplate = retryPolicyTemplate;
    }

    public void setDisableValidation(boolean disableValidation)
    {
        this.disableValidation = disableValidation;
    }
}
