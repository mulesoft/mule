/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import org.mule.api.connection.ConnectionProvider;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.module.extension.internal.runtime.ParameterGroupAwareObjectBuilder;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;

/**
 * Implementation of {@link ParameterGroupAwareObjectBuilder} which produces instances
 * of {@link ConnectionProviderModel}
 *
 * @since 4.0
 */
public final class ConnectionProviderObjectBuilder extends ParameterGroupAwareObjectBuilder<ConnectionProvider>
{

    private final ConnectionProviderModel providerModel;

    /**
     * Creates a new instances which produces instances based on the given {@code providerModel} and
     * {@code resolverSet}
     *
     * @param providerModel the {@link ConnectionProviderModel} which describes the instances to be produced
     * @param resolverSet   a {@link ResolverSet} to populate the values
     */
    public ConnectionProviderObjectBuilder(ConnectionProviderModel providerModel, ResolverSet resolverSet)
    {
        super(providerModel.getConnectionProviderFactory().getObjectType(), providerModel, resolverSet);
        this.providerModel = providerModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConnectionProvider instantiateObject()
    {
        return providerModel.getConnectionProviderFactory().newInstance();
    }
}
