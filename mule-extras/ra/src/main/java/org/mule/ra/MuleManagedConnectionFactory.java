/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.ra;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

/**
 * <code>MuleManagedConnectionFactory</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MuleManagedConnectionFactory implements ManagedConnectionFactory, Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1460847590293644271L;

    private transient PrintWriter out;
    private transient PropertyChangeSupport changes = new PropertyChangeSupport(this);

    // userName property value
    private String username = null;

    // password property value
    private String password = null;

    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(MuleManagedConnectionFactory.class);

    public MuleManagedConnectionFactory()
    {
        super();
    }

    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((password == null) ? 0 : password.hashCode());
        result = PRIME * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (this.getClass() != obj.getClass())
        {
            return false;
        }

        final MuleManagedConnectionFactory other = (MuleManagedConnectionFactory)obj;

        if (password == null)
        {
            if (other.password != null)
            {
                return false;
            }
        }
        else if (!password.equals(other.password))
        {
            return false;
        }

        if (username == null)
        {
            if (other.username != null)
            {
                return false;
            }
        }
        else if (!username.equals(other.username))
        {
            return false;
        }

        return true;
    }

    /**
     * Creates a Connection Factory instance. The ConnectionFactory instance is
     * initialized with the passed ConnectionManager. In the managed scenario,
     * ConnectionManager is provided by the application server.
     * 
     * @param cxManager
     *            ConnectionManager to be associated with created EIS connection
     *            factory instance
     * @return EIS-specific Connection Factory instance
     * @throws javax.resource.ResourceException
     *             if the attempt to create a connection factory fails
     */

    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException
    {
        MuleConnectionFactory cf = null;
        try
        {
            cf = new DefaultMuleConnectionFactory(this, cxManager, null);
        }
        catch (Exception e)
        {
            throw new ResourceException(e.getMessage());
        }
        return cf;
    }

    /**
     * Creates a Connection Factory instance. The Connection Factory instance is
     * initialized with a default ConnectionManager. In the non-managed scenario, the
     * ConnectionManager is provided by the resource adapter.
     * 
     * @return EIS-specific Connection Factory instance
     * @throws ResourceException
     *             if the attempt to create a connection factory fails
     */

    public Object createConnectionFactory() throws ResourceException
    {
        return new DefaultMuleConnectionFactory(this, null, null);
    }

    /**
     * ManagedConnectionFactory uses the security information (passed as Subject) and
     * additional ConnectionRequestInfo (which is specific to ResourceAdapter and
     * opaque to application server) to create this new connection.
     * 
     * @param subject
     *            caller's security information
     * @param cxRequestInfo
     *            additional resource adapter specific connection request information
     * @return ManagedConnection instance
     * @throws ResourceException
     *             if the attempt to create a connection fails
     */

    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException
    {
        MuleManagedConnection mc = new MuleManagedConnection(this, subject, cxRequestInfo);
        return mc;
    }

    /**
     * Returns a matched managed connection from the candidate set of connections.
     * ManagedConnectionFactory uses the security info (as in Subject) and
     * information provided through ConnectionRequestInfo and additional Resource
     * Adapter specific criteria to do matching. A MC that has the requested store is
     * returned as a match
     * 
     * @param connectionSet
     *            candidate connection set
     * @param subject
     *            caller's security information
     * @param cxRequestInfo
     *            additional resource adapter specific connection request information
     * @return ManagedConnection if resource adapter finds an acceptable match,
     *         otherwise null
     * @throws ResourceException
     *             if the match fails
     */

    public ManagedConnection matchManagedConnections(Set connectionSet,
            Subject subject,
            ConnectionRequestInfo cxRequestInfo) throws ResourceException
    {
        PasswordCredential pc = RaHelper.getPasswordCredential(this, subject, cxRequestInfo);

        Iterator it = connectionSet.iterator();
        while (it.hasNext())
        {
            Object obj = it.next();
            if (obj instanceof MuleManagedConnection)
            {
                MuleManagedConnection mc = (MuleManagedConnection)obj;
                PasswordCredential mcpc = mc.getPasswordCredential();
                if (mcpc != null && pc != null && mcpc.equals(pc))
                {
                    return mc;
                }
            }
        }
        return null;
    }

    /**
     * Sets the log writer for this ManagedConnectionFactory instance. The log writer
     * is a character output stream to which all logging and tracing messages for
     * this ManagedConnectionfactory instance will be printed.
     * 
     * @param out
     *            an output stream for error logging and tracing
     * @throws ResourceException
     *             if the method fails
     */

    public void setLogWriter(PrintWriter out) throws ResourceException
    {
        this.out = out;
    }

    /**
     * Gets the log writer for this ManagedConnectionFactory instance.
     * 
     * @return PrintWriter an output stream for error logging and tracing
     * @throws ResourceException
     *             if the method fails
     */

    public PrintWriter getLogWriter() throws ResourceException
    {
        return this.out;
    }

    /**
     * Associate PropertyChangeListener with the ManagedConnectionFactory, in order
     * to notify about properties changes.
     * 
     * @param lis
     *            the PropertyChangeListener to be associated with the
     *            ManagedConnectionFactory
     */

    public void addPropertyChangeListener(PropertyChangeListener lis)
    {
        changes.addPropertyChangeListener(lis);
    }

    /**
     * Delete association of PropertyChangeListener with the
     * ManagedConnectionFactory.
     * 
     * @param lis
     *            the PropertyChangeListener to be removed
     */

    public void removePropertyChangeListener(PropertyChangeListener lis)
    {
        changes.removePropertyChangeListener(lis);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        this.changes = new PropertyChangeSupport(this);
        this.out = null;
    }

    /**
     * Returns the value of the userName property.
     * 
     * @return the value of the userName property
     */

    public String getUsername()
    {
        return this.username;
    }

    /**
     * Sets the value of the userName property.
     * 
     * @param username
     *            String containing the value to be assigned to userName
     */

    public void setUsername(String username)
    {
        this.username = username;
    }

    /**
     * Returns the value of the password property.
     * 
     * @return the value of the password property
     */

    public String getPassword()
    {
        return this.password;
    }

    /**
     * Sets the value of the password property.
     * 
     * @param password
     *            String containing the value to be assigned to password
     */

    public void setPassword(String password)
    {
        this.password = password;
    }
}
