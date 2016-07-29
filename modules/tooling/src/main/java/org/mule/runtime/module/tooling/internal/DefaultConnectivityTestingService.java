/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.module.tooling.api.connectivity.ConnectionResult.createFailureResult;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.module.tooling.api.artifact.ToolingArtifact;
import org.mule.runtime.module.tooling.api.connectivity.ConnectionResult;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingStrategy;
import org.mule.runtime.module.tooling.api.connectivity.NoConnectivityTestingObjectFoundException;

import java.util.Collection;

/**
 * {@inheritDoc}
 */
public class DefaultConnectivityTestingService implements ConnectivityTestingService
{

    private ToolingArtifact toolingArtifact;

    /**
     * Creates a {@code DefaultConnectivityTestingService}.
     *
     * @param toolingArtifact tooling artifact used to do connectivity testing
     */
    public DefaultConnectivityTestingService(ToolingArtifact toolingArtifact)
    {
        this.toolingArtifact = toolingArtifact;
    }

    /**
     * {@inheritDoc}
     *
     * @throws MuleRuntimeException
     */
    @Override
    public ConnectionResult testConnection()
    {
        if (!toolingArtifact.isStarted())
        {
            try
            {
                toolingArtifact.start();
            }
            catch (InitialisationException e)
            {
                return createFailureResult(e.getMessage(), e);
            }
            catch (ConfigurationException e)
            {
                return createFailureResult(e.getMessage(), e);
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(e);
            }
        }
        Collection<ConnectivityTestingStrategy> connectivityTestingStrategies = toolingArtifact.getMuleContext().getRegistry().lookupObjects(ConnectivityTestingStrategy.class);
        for (ConnectivityTestingStrategy connectivityTestingStrategy : connectivityTestingStrategies)
        {
            if (connectivityTestingStrategy.connectionTestingObjectIsPresent())
            {
                try
                {
                    return connectivityTestingStrategy.testConnectivity();
                }
                catch (Exception e)
                {
                    return createFailureResult(e.getMessage(), e);
                }
            }
        }
        throw new NoConnectivityTestingObjectFoundException(createStaticMessage("It was not possible to find an object to do connectivity testing"));
    }
}
