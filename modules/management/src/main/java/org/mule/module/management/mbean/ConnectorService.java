/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
