package org.mule.management.mbeans;

import org.mule.umo.MessagingException;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.provider.UMOMessageDispatcherFactory;

import java.beans.ExceptionListener;

/**
 * $Id$
 */
public class ConnectorService implements ConnectorServiceMBean
{
    private UMOConnector connector;

    public ConnectorService(final UMOConnector connector)
    {
        this.connector = connector;
    }

    public boolean isStarted()
    {
        return connector.isStarted();
    }

    public boolean isDisposed()
    {
        return connector.isDisposed();
    }

    public boolean isDisposing()
    {
        return connector.isDisposing();
    }

    public String getName()
    {
        return connector.getName();
    }

    public String getProtocol()
    {
        return connector.getProtocol();
    }

    public ExceptionListener getExceptionListener()
    {
        return connector.getExceptionListener();
    }

    public UMOMessageDispatcherFactory getDispatcherFactory()
    {
        return connector.getDispatcherFactory();
    }

    public void startConnector()
            throws UMOException
    {
        connector.startConnector();
    }

    public void stopConnector()
            throws UMOException
    {
        connector.stopConnector();
    }

    public void dispose()
    {
        connector.dispose();
    }

    public void initialise()
            throws InitialisationException, RecoverableException
    {
        connector.initialise();
    }
}
