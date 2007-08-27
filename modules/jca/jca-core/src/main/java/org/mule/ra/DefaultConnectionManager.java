/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.ra;

import java.io.IOException;
import java.io.ObjectInputStream;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultConnectionManager</code> TODO
 */
public class DefaultConnectionManager implements ConnectionManager, ConnectionEventListener
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1967602190602154760L;

    private transient Log logger = LogFactory.getLog(this.getClass());

    public DefaultConnectionManager()
    {
        super();
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException
    {
        ois.defaultReadObject();
        this.logger = LogFactory.getLog(this.getClass());
    }

    /**
     * @see javax.resource.spi.ConnectionManager#allocateConnection(javax.resource.spi.ManagedConnectionFactory,
     *      javax.resource.spi.ConnectionRequestInfo)
     */
    public Object allocateConnection(ManagedConnectionFactory connectionFactory, ConnectionRequestInfo info)
        throws ResourceException
    {
        Subject subject = null;
        ManagedConnection connection = connectionFactory.createManagedConnection(subject, info);
        connection.addConnectionEventListener(this);
        return connection.getConnection(subject, info);
    }

    /**
     * @see javax.resource.spi.ConnectionEventListener#connectionClosed(javax.resource.spi.ConnectionEvent)
     */
    public void connectionClosed(ConnectionEvent event)
    {
        try
        {
            ((ManagedConnection)event.getSource()).cleanup();
        }
        catch (ResourceException e)
        {
            logger.warn("Error occured during the cleanup of a managed connection: ", e);
        }
        try
        {
            ((ManagedConnection)event.getSource()).destroy();
        }
        catch (ResourceException e)
        {
            logger.warn("Error occured during the destruction of a managed connection: ", e);
        }
    }

    /**
     * @see javax.resource.spi.ConnectionEventListener#localTransactionStarted(javax.resource.spi.ConnectionEvent)
     */
    public void localTransactionStarted(ConnectionEvent event)
    {
        // TODO maybe later?
    }

    /**
     * @see javax.resource.spi.ConnectionEventListener#localTransactionCommitted(javax.resource.spi.ConnectionEvent)
     */
    public void localTransactionCommitted(ConnectionEvent event)
    {
        // TODO maybe later?
    }

    /**
     * @see javax.resource.spi.ConnectionEventListener#localTransactionRolledback(javax.resource.spi.ConnectionEvent)
     */
    public void localTransactionRolledback(ConnectionEvent event)
    {
        // TODO maybe later?
    }

    /**
     * @see javax.resource.spi.ConnectionEventListener#connectionErrorOccurred(javax.resource.spi.ConnectionEvent)
     */
    public void connectionErrorOccurred(ConnectionEvent event)
    {
        logger.warn("Managed connection experiened an error: ", event.getException());
        try
        {
            ((ManagedConnection)event.getSource()).cleanup();
        }
        catch (ResourceException e)
        {
            logger.warn("Error occured during the cleanup of a managed connection: ", e);
        }
        try
        {
            ((ManagedConnection)event.getSource()).destroy();
        }
        catch (ResourceException e)
        {
            logger.warn("Error occured during the destruction of a managed connection: ", e);
        }
    }

}
