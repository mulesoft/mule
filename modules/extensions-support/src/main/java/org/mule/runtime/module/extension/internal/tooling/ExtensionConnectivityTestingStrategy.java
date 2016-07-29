/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.tooling;

import static org.mule.runtime.module.tooling.api.connectivity.ConnectionResult.createFailureResult;
import static org.mule.runtime.module.tooling.api.connectivity.ConnectionResult.createSuccessResult;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.tooling.api.connectivity.ConnectionResult;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingStrategy;
import org.mule.runtime.module.tooling.api.connectivity.MultipleConnectivityTestingObjectsFoundException;

import javax.inject.Inject;

/**
 * Implementation of {@code ConnectivityTestingStrategy} that can do connectivity testing over
 * components creates with extensions API.
 *
 * @since 4.0
 */
public class ExtensionConnectivityTestingStrategy implements ConnectivityTestingStrategy
{

    @Inject
    private MuleContext muleContext;

    void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionResult testConnectivity()
    {
        try
        {
            ConnectionProvider connectionProvider = (ConnectionProvider) muleContext.getRegistry().lookupObject(ConfigurationProvider.class).get(null).getConnectionProvider().get();
            Object connection = connectionProvider.connect();
            ConnectionValidationResult validationResult = connectionProvider.validate(connection);
            if (!validationResult.isValid())
            {
                return createFailureResult("Invalid connection");
            }
            return createSuccessResult();
        }
        catch (Exception e)
        {
            return createFailureResult(e.getMessage(), e);
        }
    }

    /**
     * @return true whenever there's a {@code ConfigurationProvider} in the configuration, false otherwise.
     * @throws MultipleConnectivityTestingObjectsFoundException when there's more than one {@code ConfigurationProvider} in the configuration
     */
    @Override
    public boolean connectionTestingObjectIsPresent()
    {
        return lookupConnectionTestingObject() != null;
    }

    private ConfigurationProvider lookupConnectionTestingObject()
    {
        try
        {
            return muleContext.getRegistry().lookupObject(ConfigurationProvider.class);
        }
        catch (RegistrationException e)
        {
            throw new MultipleConnectivityTestingObjectsFoundException(e);
        }
    }

}
