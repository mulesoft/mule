/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.mbean;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connector;
import org.mule.util.ObjectNameHelper;

public class ConnectorService implements ConnectorServiceMBean
{
    private final Connector connector;
    private final String name;

    public ConnectorService(final Connector connector)
    {
        this.connector = connector;
        name = new ObjectNameHelper(connector.getMuleContext()).getConnectorName(connector);
    }

    public boolean isStarted()
    {
        return connector.isStarted();
    }

    public boolean isDisposed()
    {
        return connector.isDisposed();
    }

    public String getName()
    {
        return name;
    }

    public String getProtocol()
    {
        return connector.getProtocol();
    }

    public void startConnector() throws MuleException
    {
        connector.start();
    }

    public void stopConnector() throws MuleException
    {
        connector.stop();
    }

    public void dispose()
    {
        connector.dispose();
    }

    public void initialise() throws InitialisationException
    {
        connector.initialise();
    }

}
