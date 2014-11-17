/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.connector;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.client.OperationOptions;
import org.mule.api.connector.ConnectorOperationLocator;
import org.mule.api.connector.ConnectorOperationProvider;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;

import java.util.Collection;

/**
 * Default {@link org.mule.api.connector.ConnectorOperationLocator} that will search
 * in the mule registry for registered {@link org.mule.api.connector.ConnectorOperationLocator}
 * to later provider operations through the use of URLs.
 */
public class MuleConnectorOperationLocator implements ConnectorOperationLocator, MuleContextAware, Initialisable
{

    private MuleContext muleContext;
    private Collection<ConnectorOperationProvider> connectorOperationProviders;

    @Override
    public void initialise() throws InitialisationException
    {
        this.connectorOperationProviders = muleContext.getRegistry().lookupObjects(ConnectorOperationProvider.class);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public MessageProcessor locateRequestResponseConnectorOperation(String url, OperationOptions operationOptions) throws MuleException
    {
        for (ConnectorOperationProvider connectorOperationProvider : connectorOperationProviders)
        {
            if (connectorOperationProvider.supportsUrl(url))
            {
                return connectorOperationProvider.getRequestResponseMessageProcessor(url, operationOptions);
            }
        }
        return null;
    }

    @Override
    public MessageProcessor locateRequestResponseConnectorOperation(String url) throws MuleException
    {
        for (ConnectorOperationProvider connectorOperationProvider : connectorOperationProviders)
        {
            if (connectorOperationProvider.supportsUrl(url))
            {
                return connectorOperationProvider.getRequestResponseMessageProcessor(url);
            }
        }
        return null;
    }

    @Override
    public MessageProcessor locateOneWayConnectorOperation(String url, OperationOptions operationOptions) throws MuleException
    {
        for (ConnectorOperationProvider connectorOperationProvider : connectorOperationProviders)
        {
            if (connectorOperationProvider.supportsUrl(url))
            {
                return connectorOperationProvider.getOneWayMessageProcessor(url, operationOptions);
            }
        }
        return null;
    }

    @Override
    public MessageProcessor locateOneWayConnectorOperation(String url) throws MuleException
    {
        for (ConnectorOperationProvider connectorOperationProvider : connectorOperationProviders)
        {
            if (connectorOperationProvider.supportsUrl(url))
            {
                return connectorOperationProvider.getOneWayMessageProcessor(url);
            }
        }
        return null;
    }
}
