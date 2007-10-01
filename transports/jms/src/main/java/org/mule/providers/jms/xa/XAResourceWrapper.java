/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms.xa;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * Arjuna can't serialize proxy
 */
public class XAResourceWrapper implements XAResource
{

    private XAResource xaResource;
    private SessionInvocationHandler sessionInvocationHandler;

    public XAResourceWrapper(XAResource xaResource, SessionInvocationHandler sessionInvocationHandler)
    {
        this.xaResource = xaResource;
        this.sessionInvocationHandler = sessionInvocationHandler;
    }

    public int getTransactionTimeout()
            throws XAException
    {
        return xaResource.getTransactionTimeout();
    }

    public boolean setTransactionTimeout(int i)
            throws XAException
    {
        return xaResource.setTransactionTimeout(i);
    }

    public boolean isSameRM(XAResource xaResource)
            throws XAException
    {
        if (xaResource instanceof XAResourceWrapper){
            xaResource=((XAResourceWrapper)xaResource).xaResource;
        }
        return this.xaResource.isSameRM(xaResource);
    }

    public Xid[] recover(int i)
            throws XAException
    {
        return xaResource.recover(i);
    }

    public int prepare(Xid xid)
            throws XAException
    {
        return xaResource.prepare(xid);
    }

    public void forget(Xid xid)
            throws XAException
    {
        xaResource.forget(xid);
    }

    public void rollback(Xid xid)
            throws XAException
    {
        xaResource.rollback(xid);
    }

    public void end(Xid xid, int i)
            throws XAException
    {
        xaResource.end(xid, i);
        sessionInvocationHandler.setEnlisted(false);
    }

    public void start(Xid xid, int i)
            throws XAException
    {
        xaResource.start(xid, i);
    }

    public void commit(Xid xid, boolean b)
            throws XAException
    {
        xaResource.commit(xid, b);
    }

    public String toString()
    {
        return xaResource.toString();
    }
}
