/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.ra;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.security.MuleCredentials;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * <code>MuleManagedConnection</code> TODO
 * 
 * @version $Revision$
 */
public class MuleManagedConnection implements ManagedConnection
{
    /**
     * logger used by this class
     */
    private static final transient Log logger = LogFactory.getLog(MuleManagedConnection.class);

    private MuleManagedConnectionFactory mcf;
    private List listeners = new ArrayList();
    private Set connectionSet;
    private PrintWriter logWriter;
    private boolean destroyed;

    private PasswordCredential passCred;

    /**
     * Constructor.
     * 
     * @param mcf the ManagedConnectionFactory that created this instance
     * @param subject security context as JAAS subject
     * @param cxRequestInfo ConnectionRequestInfo instance
     * @throws javax.resource.ResourceException in case of any error
     */

    MuleManagedConnection(MuleManagedConnectionFactory mcf,
                          Subject subject,
                          ConnectionRequestInfo cxRequestInfo) throws ResourceException
    {
        this.mcf = mcf;

        // Note: this will select the credential that matches this MC's MCF.
        // The credential's MCF is set by the application server.
        this.passCred = RaHelper.getPasswordCredential(mcf, subject, cxRequestInfo);

        connectionSet = new HashSet();
    }

    /**
     * Creates a new connection handle to the Mail Server represented by the
     * ManagedConnection instance. This connection handle is used by the application
     * code to refer to the underlying physical connection.
     * 
     * @param subject security context as JAAS subject
     * @param connectionRequestInfo ConnectionRequestInfo instance
     * @return Connection instance representing the connection handle
     * @throws ResourceException if the method fails to get a connection
     */

    public Object getConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo)
        throws ResourceException
    {

        checkIfDestroyed();

        PasswordCredential pc = RaHelper.getPasswordCredential(mcf, subject, connectionRequestInfo);

        if (!passCred.equals(pc))
        {
            // TODO change the message, we are not dealing with an endpoint here
            throw new javax.resource.spi.SecurityException(new Message(Messages.AUTH_DENIED_ON_ENDPOINT_X,
                this).getMessage());
        }

        String user;
        String password;
        MuleConnectionRequestInfo info = (MuleConnectionRequestInfo)connectionRequestInfo;

        user = info.getUserName();
        password = info.getPassword();
        if (user == null)
        {
            // Use default values
            user = mcf.getUsername();
            password = mcf.getPassword();
        }
        MuleCredentials creds = null;
        if (user != null)
        {
            if (password == null)
            {
                password = "";
            }
            creds = new MuleCredentials(user, password.toCharArray());
        }

        MuleConnection connection = new DefaultMuleConnection(this, info.getManager(), creds);
        addConnection(connection);
        return connection;
    }

    /**
     * Destroys the physical connection.
     * 
     * @throws ResourceException if the method fails to destroy the connection
     */

    public void destroy() throws ResourceException
    {
        if (destroyed)
        {
            return;
        }
        destroyed = true;

        invalidateConnections();
    }

    /**
     * Initiates a cleanup of the client-specific state maintained by a
     * ManagedConnection instance. The cleanup should invalidate all connection
     * handles created using this ManagedConnection instance.
     * 
     * @throws ResourceException if the cleanup fails
     */

    public void cleanup() throws ResourceException
    {
        checkIfDestroyed();

        invalidateConnections();
    }

    private void invalidateConnections()
    {
        Iterator it = connectionSet.iterator();
        while (it.hasNext())
        {
            DefaultMuleConnection connection = (DefaultMuleConnection)it.next();
            connection.invalidate();
        }
        connectionSet.clear();
    }

    /**
     * Used by the container to change the association of an application-level
     * connection handle with a ManagedConnection instance. The container should find
     * the right ManagedConnection instance and call the associateConnection method.
     * 
     * @param connection application-level connection handle
     * @throws ResourceException if the attempt to change the association fails
     */

    public void associateConnection(Object connection) throws ResourceException
    {
        checkIfDestroyed();

        if (connection instanceof MuleConnection)
        {
            MuleConnection cnn = (MuleConnection)connection;
            cnn.associateConnection(this);
        }
        else
        {
            throw new IllegalStateException(new Message(Messages.OBJECT_X_MARKED_INVALID,
                DefaultMuleConnection.class.getName() + ": "
                                + (connection == null ? "null" : connection.getClass().getName())).toString());
        }
    }

    /**
     * Adds a connection event listener to the ManagedConnection instance. The
     * registered ConnectionEventListener instances are notified of connection close
     * and error events as well as local-transaction-related events on the Managed
     * Connection.
     * 
     * @param listener a new ConnectionEventListener to be registered
     */

    public void addConnectionEventListener(ConnectionEventListener listener)
    {
        listeners.add(listener);
    }

    /**
     * Removes an already registered connection event listener from the
     * ManagedConnection instance.
     * 
     * @param listener already registered connection event listener to be removed
     */

    public void removeConnectionEventListener(ConnectionEventListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * Returns a javax.transaction.xa.XAresource instance. An application server
     * enlists this XAResource instance with the Transaction Manager if the
     * ManagedConnection instance is being used in a JTA transaction that is being
     * coordinated by the Transaction Manager. <p/> Because this implementation does
     * not support transactions, the method throws an exception.
     * 
     * @return the XAResource instance
     * @throws ResourceException if transactions are not supported
     */
    // TODO
    public XAResource getXAResource() throws ResourceException
    {
        throw new NotSupportedException("getXAResource");
    }

    /**
     * Returns a javax.resource.spi.LocalTransaction instance. The LocalTransaction
     * interface is used by the container to manage local transactions for a RM
     * instance. <p/> Because this implementation does not support transactions, the
     * method throws an exception.
     * 
     * @return javax.resource.spi.LocalTransaction instance
     * @throws ResourceException if transactions are not supported
     */

    public javax.resource.spi.LocalTransaction getLocalTransaction() throws ResourceException
    {
        throw new NotSupportedException("getLocalTransaction");
    }

    /**
     * Gets the metadata information for this connection's underlying EIS resource
     * manager instance. The ManagedConnectionMetaData interface provides information
     * about the underlying EIS instance associated with the ManagedConnection
     * instance.
     * 
     * @return ManagedConnectionMetaData ManagedConnectionMetaData instance
     * @throws ResourceException if the metadata cannot be retrieved
     */

    public ManagedConnectionMetaData getMetaData() throws ResourceException
    {
        checkIfDestroyed();
        return new MuleManagedConnectionMetaData(this);
    }

    /**
     * Sets the log writer for this ManagedConnection instance. The log writer is a
     * character output stream to which all logging and tracing messages for this
     * ManagedConnection instance will be printed.
     * 
     * @param out character output stream to be associated
     * @throws ResourceException if the method fails
     */

    public void setLogWriter(PrintWriter out) throws ResourceException
    {
        this.logWriter = out;
    }

    /**
     * Gets the log writer for this ManagedConnection instance.
     * 
     * @return the character output stream associated with this ManagedConnection
     *         instance
     * @throws ResourceException if the method fails
     */

    public PrintWriter getLogWriter() throws ResourceException
    {
        return logWriter;
    }

    /**
     * Gets the user name of the user associated with the ManagedConnection instance.
     * 
     * @return the username for this connection
     */

    public String getUsername()
    {
        if (passCred != null)
        {
            return passCred.getUserName();
        }
        else
        {
            return null;
        }
    }

    /**
     * Gets the password for the user associated with the ManagedConnection instance.
     * 
     * @return the password for this connection
     */

    public PasswordCredential getPasswordCredential()
    {
        return passCred;
    }

    /**
     * Associate connection handle with the physical connection.
     * 
     * @param connection connection handle
     */

    public void addConnection(MuleConnection connection)
    {
        connectionSet.add(connection);
    }

    /**
     * Check validation of the physical connection.
     * 
     * @throws ResourceException if the connection has been destroyed
     */

    private void checkIfDestroyed() throws ResourceException
    {
        if (destroyed)
        {
            throw new ResourceException(
                new Message(Messages.X_IS_DISPOSED, "MuleManagedConnection").toString());
        }
    }

    /**
     * Removes the associated connection handle from the connections set to the
     * physical connection.
     * 
     * @param connection the connection handle
     */

    public void removeConnection(MuleConnection connection)
    {
        connectionSet.remove(connection);
    }

    /**
     * Checks validation of the physical connection.
     * 
     * @return true if the connection has been destroyed; false otherwise
     */

    boolean isDestroyed()
    {
        return destroyed;
    }

    /**
     * Returns the ManagedConnectionFactory that created this instance of
     * ManagedConnection.
     * 
     * @return the ManagedConnectionFactory for this connection
     */

    public MuleManagedConnectionFactory getManagedConnectionFactory()
    {
        return this.mcf;
    }

    void fireBeginEvent()
    {
        ConnectionEvent event = new ConnectionEvent(MuleManagedConnection.this,
            ConnectionEvent.LOCAL_TRANSACTION_STARTED);
        Iterator iterator = listeners.iterator();
        while (iterator.hasNext())
        {
            ConnectionEventListener l = (ConnectionEventListener)iterator.next();
            l.localTransactionStarted(event);
        }
    }

    void fireCommitEvent()
    {
        ConnectionEvent event = new ConnectionEvent(MuleManagedConnection.this,
            ConnectionEvent.LOCAL_TRANSACTION_COMMITTED);
        Iterator iterator = listeners.iterator();
        while (iterator.hasNext())
        {
            ConnectionEventListener l = (ConnectionEventListener)iterator.next();
            l.localTransactionCommitted(event);
        }
    }

    void fireRollbackEvent()
    {
        ConnectionEvent event = new ConnectionEvent(MuleManagedConnection.this,
            ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK);
        Iterator iterator = listeners.iterator();
        while (iterator.hasNext())
        {
            ConnectionEventListener l = (ConnectionEventListener)iterator.next();
            l.localTransactionRolledback(event);
        }
    }

    void fireCloseEvent(MuleConnection connection)
    {
        ConnectionEvent event = new ConnectionEvent(MuleManagedConnection.this,
            ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(connection);

        Iterator iterator = listeners.iterator();
        while (iterator.hasNext())
        {
            ConnectionEventListener l = (ConnectionEventListener)iterator.next();
            l.connectionClosed(event);
        }
    }

    void fireErrorOccurredEvent(Exception error)
    {
        ConnectionEvent event = new ConnectionEvent(MuleManagedConnection.this,
            ConnectionEvent.CONNECTION_ERROR_OCCURRED, error);
        Iterator iterator = listeners.iterator();
        while (iterator.hasNext())
        {
            ConnectionEventListener l = (ConnectionEventListener)iterator.next();
            l.connectionErrorOccurred(event);
        }
    }

}
