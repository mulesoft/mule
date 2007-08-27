/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.xa;

import javax.transaction.Status;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO document
 */
public class DefaultXASession implements XAResource
{

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected AbstractTransactionContext localContext;
    protected Xid localXid;
    protected AbstractXAResourceManager resourceManager;

    public DefaultXASession(AbstractXAResourceManager resourceManager)
    {
        this.localContext = null;
        this.localXid = null;
        this.resourceManager = resourceManager;
    }

    public XAResource getXAResource()
    {
        return this;
    }

    public Object getResourceManager()
    {
        return resourceManager;
    }

    //
    // Local transaction implementation
    //
    public void begin() throws ResourceManagerException
    {
        if (localXid != null)
        {
            throw new IllegalStateException(
                "Cannot start local transaction. An XA transaction is already in progress.");
        }
        if (localContext != null)
        {
            throw new IllegalStateException(
                "Cannot start local transaction. A local transaction already in progress.");
        }
        localContext = resourceManager.createTransactionContext(this);
        resourceManager.beginTransaction(localContext);
    }

    public void commit() throws ResourceManagerException
    {
        if (localXid != null)
        {
            throw new IllegalStateException(
                "Cannot commit local transaction as an XA transaction is in progress.");
        }
        if (localContext == null)
        {
            throw new IllegalStateException("Cannot commit local transaction as no transaction was begun");
        }
        resourceManager.commitTransaction(localContext);
        localContext = null;
    }

    public void rollback() throws ResourceManagerException
    {
        if (localXid != null)
        {
            throw new IllegalStateException(
                "Cannot rollback local transaction as an XA transaction is in progress.");
        }
        if (localContext == null)
        {
            throw new IllegalStateException("Cannot commit local transaction as no transaction was begun");
        }
        resourceManager.rollbackTransaction(localContext);
        localContext = null;
    }

    //
    // XAResource implementation
    //

    public boolean isSameRM(XAResource xares) throws XAException
    {
        return xares instanceof DefaultXASession
               && ((DefaultXASession) xares).getResourceManager().equals(resourceManager);
    }

    public Xid[] recover(int flag) throws XAException
    {
        return null;
    }

    public void start(Xid xid, int flags) throws XAException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(new StringBuffer(128).append("Thread ").append(Thread.currentThread()).append(
                flags == TMNOFLAGS ? " starts" : flags == TMJOIN ? " joins" : " resumes").append(
                " work on behalf of transaction branch ").append(xid).toString());
        }
        // A local transaction is already begun
        if (this.localContext != null)
        {
            throw new XAException(XAException.XAER_PROTO);
        }
        // This session has already been associated with an xid
        if (this.localXid != null)
        {
            throw new XAException(XAException.XAER_PROTO);
        }
        switch (flags)
        {
            // a new transaction
            case TMNOFLAGS :
            case TMJOIN :
            default :
                try
                {
                    localContext = resourceManager.createTransactionContext(this);
                    resourceManager.beginTransaction(localContext);
                }
                catch (Exception e)
                {
                    // TODO MULE-863: Is logging necessary?
                    logger.error("Could not create new transactional resource", e);
                    throw (XAException) new XAException(e.getMessage()).initCause(e);
                }
                break;
            case TMRESUME :
                localContext = resourceManager.getSuspendedTransactionalResource(xid);
                if (localContext == null)
                {
                    throw new XAException(XAException.XAER_NOTA);
                }
                // TODO: resume context
                resourceManager.removeSuspendedTransactionalResource(xid);
                break;
        }
        localXid = xid;
        resourceManager.addActiveTransactionalResource(localXid, localContext);
    }

    public void end(Xid xid, int flags) throws XAException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(new StringBuffer(128).append("Thread ").append(Thread.currentThread()).append(
                flags == TMSUSPEND ? " suspends" : flags == TMFAIL ? " fails" : " ends").append(
                " work on behalf of transaction branch ").append(xid).toString());
        }
        // No transaction is already begun
        if (localContext == null)
        {
            throw new XAException(XAException.XAER_NOTA);
        }
        // This session has already been associated with an xid
        if (localXid == null || !localXid.equals(xid))
        {
            throw new XAException(XAException.XAER_PROTO);
        }

        try
        {
            switch (flags)
            {
                case TMSUSPEND :
                    // TODO: suspend context
                    resourceManager.addSuspendedTransactionalResource(localXid, localContext);
                    resourceManager.removeActiveTransactionalResource(localXid);
                    break;
                case TMFAIL :
                    resourceManager.setTransactionRollbackOnly(localContext);
                    break;
                case TMSUCCESS : // no-op
                default :        // no-op
                    break;
            }
        }
        catch (ResourceManagerException e)
        {
            throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
        }
        localXid = null;
        localContext = null;
    }

    public void commit(Xid xid, boolean onePhase) throws XAException
    {
        if (xid == null)
        {
            throw new XAException(XAException.XAER_PROTO);
        }
        AbstractTransactionContext context = resourceManager.getActiveTransactionalResource(xid);
        if (context == null)
        {
            throw new XAException(XAException.XAER_NOTA);
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Committing transaction branch " + xid);
        }
        if (context.status == Status.STATUS_MARKED_ROLLBACK)
        {
            throw new XAException(XAException.XA_RBROLLBACK);
        }

        try
        {
            if (context.status != Status.STATUS_PREPARED)
            {
                if (onePhase)
                {
                    resourceManager.prepareTransaction(context);
                }
                else
                {
                    throw new XAException(XAException.XAER_PROTO);
                }
            }
            resourceManager.commitTransaction(context);
        }
        catch (ResourceManagerException e)
        {
            throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
        }
        resourceManager.removeActiveTransactionalResource(xid);
        resourceManager.removeSuspendedTransactionalResource(xid);
    }

    public void rollback(Xid xid) throws XAException
    {
        if (xid == null)
        {
            throw new XAException(XAException.XAER_PROTO);
        }
        AbstractTransactionContext context = resourceManager.getActiveTransactionalResource(xid);
        if (context == null)
        {
            throw new XAException(XAException.XAER_NOTA);
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Rolling back transaction branch " + xid);
        }
        try
        {
            resourceManager.rollbackTransaction(context);
        }
        catch (ResourceManagerException e)
        {
            throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
        }
        resourceManager.removeActiveTransactionalResource(xid);
        resourceManager.removeSuspendedTransactionalResource(xid);
    }

    public int prepare(Xid xid) throws XAException
    {
        if (xid == null)
        {
            throw new XAException(XAException.XAER_PROTO);
        }

        AbstractTransactionContext context = resourceManager.getTransactionalResource(xid);
        if (context == null)
        {
            throw new XAException(XAException.XAER_NOTA);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Preparing transaction branch " + xid);
        }

        if (context.status == Status.STATUS_MARKED_ROLLBACK)
        {
            throw new XAException(XAException.XA_RBROLLBACK);
        }

        try
        {
            return resourceManager.prepareTransaction(context);
        }
        catch (ResourceManagerException e)
        {
            throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
        }
    }

    public void forget(Xid xid) throws XAException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Forgetting transaction branch " + xid);
        }
        AbstractTransactionContext context = resourceManager.getTransactionalResource(xid);
        if (context == null)
        {
            throw new XAException(XAException.XAER_NOTA);
        }
        resourceManager.removeActiveTransactionalResource(xid);
        resourceManager.removeSuspendedTransactionalResource(xid);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.transaction.xa.XAResource#getDefaultTransactionTimeout()
     */
    public int getTransactionTimeout() throws XAException
    {
        return (int)(resourceManager.getDefaultTransactionTimeout() / 1000);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.transaction.xa.XAResource#setDefaultTransactionTimeout(int)
     */
    public boolean setTransactionTimeout(int timeout) throws XAException
    {
        resourceManager.setDefaultTransactionTimeout(timeout * 1000);
        return false;
    }

}
