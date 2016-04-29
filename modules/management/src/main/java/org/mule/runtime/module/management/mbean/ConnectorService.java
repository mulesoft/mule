/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.management.mbean;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.util.ObjectNameHelper;

public class ConnectorService implements ConnectorServiceMBean
{
    private final Connector connector;
    private final String name;

    public ConnectorService(final Connector connector)
    {
        this.connector = connector;
        name = new ObjectNameHelper(connector.getMuleContext()).getConnectorName(connector);
    }

    @Override
    public boolean isStarted()
    {
        return connector.isStarted();
    }

    @Override
    public boolean isDisposed()
    {
        return connector.isDisposed();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getProtocol()
    {
        return connector.getProtocol();
    }

    @Override
    public void startConnector() throws MuleException
    {
        connector.start();
    }

    @Override
    public void stopConnector() throws MuleException
    {
        connector.stop();
    }

    @Override
    public void dispose()
    {
        connector.dispose();
    }

    @Override
    public void initialise() throws InitialisationException
    {
        connector.initialise();
    }

}
