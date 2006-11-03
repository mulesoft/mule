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

import java.io.IOException;
import java.io.ObjectInputStream;

import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultMuleConnectionFactory</code> an implementation of the
 * MuleconnectionFactory interface used by clients of this ResourceAdapter to obtain
 * a connection to Mule resources.
 */
public class DefaultMuleConnectionFactory implements MuleConnectionFactory
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1552386015565975623L;

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(this.getClass());

    private transient ConnectionManager manager;
    private transient MuleManagedConnectionFactory factory;
    private Reference reference;
    private MuleConnectionRequestInfo info;

    public DefaultMuleConnectionFactory(MuleManagedConnectionFactory factory,
                                        ConnectionManager manager,
                                        MuleConnectionRequestInfo info)
    {
        this.factory = factory;
        this.manager = manager;
        this.info = info;
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException
    {
        ois.defaultReadObject();
        // TODO this is incomplete:
        // MuleManagedConnectionFactory is Serializable but marked transient?!
        this.logger = LogFactory.getLog(this.getClass());
    }

    public MuleConnection createConnection() throws ResourceException
    {
        return createConnection(info);
    }

    public MuleConnection createConnection(MuleConnectionRequestInfo info) throws ResourceException
    {
        // TODO try {
        return (MuleConnection)manager.allocateConnection(factory, info);
        // }
        // catch (ResourceException e) {
        //            
        // logger.warn("Connection could not be created: " + e.getMessage(), e);
        // throw new UMOException(e.getMessage());
        // }
    }

    public ConnectionManager getManager()
    {
        return manager;
    }

    public void setManager(ConnectionManager manager)
    {
        this.manager = manager;
    }

    public MuleManagedConnectionFactory getFactory()
    {
        return factory;
    }

    public void setFactory(MuleManagedConnectionFactory factory)
    {
        this.factory = factory;
    }

    public Reference getReference()
    {
        return reference;
    }

    public void setReference(Reference reference)
    {
        this.reference = reference;
    }

    public MuleConnectionRequestInfo getInfo()
    {
        return info;
    }

    public void setInfo(MuleConnectionRequestInfo info)
    {
        this.info = info;
    }
}
