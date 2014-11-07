/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.connector;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.client.Options;
import org.mule.api.connector.ConnectorMessageProcessor;
import org.mule.api.connector.ConnectorMessageProcessorLocator;
import org.mule.api.connector.ConnectorMessageProcessorProvider;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;

import java.util.Collection;

public class MuleConnectorMessageProcessorLocator implements ConnectorMessageProcessorLocator, MuleContextAware, Initialisable
{

    private MuleContext muleContext;
    private Collection<ConnectorMessageProcessorProvider> connectorMessageProcessorProviders;

    @Override
    public void initialise() throws InitialisationException
    {
        this.connectorMessageProcessorProviders = muleContext.getRegistry().lookupObjects(ConnectorMessageProcessorProvider.class);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public ConnectorMessageProcessor locateConnectorOperation(String url, Options options) throws MuleException
    {
        for (ConnectorMessageProcessorProvider connectorMessageProcessorProvider : connectorMessageProcessorProviders)
        {
            if (connectorMessageProcessorProvider.supportsUrl(url))
            {
                return connectorMessageProcessorProvider.getMessageProcessor(url, options);
            }
        }
        return null;
    }

    public ConnectorMessageProcessor locateConnectorOperation(String url) throws MuleException
    {
        for (ConnectorMessageProcessorProvider connectorMessageProcessorProvider : connectorMessageProcessorProviders)
        {
            if (connectorMessageProcessorProvider.supportsUrl(url))
            {
                return connectorMessageProcessorProvider.getMessageProcessor(url);
            }
        }
        return null;
    }

    @Override
    public ConnectorMessageProcessor locateFireAndForgetConnectorOperation(String url, Options options) throws MuleException
    {
        for (ConnectorMessageProcessorProvider connectorMessageProcessorProvider : connectorMessageProcessorProviders)
        {
            if (connectorMessageProcessorProvider.supportsUrl(url))
            {
                return connectorMessageProcessorProvider.getFireAndForgetMessageProcessor(url, options);
            }
        }
        return null;
    }

    @Override
    public ConnectorMessageProcessor locateFireAndForgetConnectorOperation(String url) throws MuleException
    {
        for (ConnectorMessageProcessorProvider connectorMessageProcessorProvider : connectorMessageProcessorProviders)
        {
            if (connectorMessageProcessorProvider.supportsUrl(url))
            {
                return connectorMessageProcessorProvider.getFireAndForgetMessageProcessor(url);
            }
        }
        return null;
    }
}
