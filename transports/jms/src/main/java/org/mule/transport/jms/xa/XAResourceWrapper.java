/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.xa;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class XAResourceWrapper implements XAResource
{
    private XAResource xaResource;
    private SessionInvocationHandler sessionInvocationHandler;
    private Boolean sameRMOverrideValue;


    public XAResourceWrapper(XAResource xaResource, SessionInvocationHandler sessionInvocationHandler, Boolean sameRMOverrideValue)
    {
        this.xaResource = xaResource;
        this.sessionInvocationHandler = sessionInvocationHandler;
        this.sameRMOverrideValue = sameRMOverrideValue;
    }

    public int getTransactionTimeout() throws XAException
    {
        return xaResource.getTransactionTimeout();
    }

    public boolean setTransactionTimeout(int i) throws XAException
    {
        return xaResource.setTransactionTimeout(i);
    }

    public boolean isSameRM(XAResource other) throws XAException
    {
        if (sameRMOverrideValue != null)
        {
            return sameRMOverrideValue;
        }

        if (other instanceof XAResourceWrapper)
        {
            other = ((XAResourceWrapper) other).xaResource;
        }
        return this.xaResource.isSameRM(other);
    }

    public Xid[] recover(int i) throws XAException
    {
        return xaResource.recover(i);
    }

    public int prepare(Xid xid) throws XAException
    {
        return xaResource.prepare(xid);
    }

    public void forget(Xid xid) throws XAException
    {
        xaResource.forget(xid);
    }

    public void rollback(Xid xid) throws XAException
    {
        xaResource.rollback(xid);
    }

    public void end(Xid xid, int i) throws XAException
    {
        xaResource.end(xid, i);
        sessionInvocationHandler.setEnlisted(false);
    }

    public void start(Xid xid, int i) throws XAException
    {
        xaResource.start(xid, i);
    }

    public void commit(Xid xid, boolean b) throws XAException
    {
        xaResource.commit(xid, b);
    }

    @Override
    public String toString()
    {
        return xaResource.toString();
    }
}
