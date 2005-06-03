/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.util.xa;

import java.util.HashMap;
import java.util.Map;

import javax.transaction.Status;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public abstract class AbstractXAResourceManager extends AbstractResourceManager
{

    protected Map suspendedContexts = new HashMap();
    protected Map activeContexts = new HashMap();

    public AbstractXAResourceManager()
    {
    }

    public class AbstractSession implements XAResource
    {
        protected AbstractTransactionContext localContext;
        protected Xid localXid;

        public AbstractSession()
        {
            this.localContext = null;
            this.localXid = null;
        }

        public XAResource getXAResource()
        {
            return this;
        }

        public Object getResourceManager()
        {
            return AbstractXAResourceManager.this;
        }

        //
        // Local transaction implementation
        //
        public void begin() throws ResourceManagerException
        {
            if (localXid != null) {
                throw new IllegalStateException("Cannot start local transaction. An XA transaction is already in progress.");
            }
            if (localContext != null) {
                throw new IllegalStateException("Cannot start local transaction. A local transaction already in progress.");
            }
            localContext = createTransactionContext(this);
            beginTransaction(localContext);
        }

        public void commit() throws ResourceManagerException
        {
            if (localXid != null) {
                throw new IllegalStateException("Cannot commit local transaction as an XA transaction is in progress.");
            }
            if (localContext == null) {
                throw new IllegalStateException("Cannot commit local transaction as no transaction was begun");
            }
            commitTransaction(localContext);
            localContext = null;
        }

        public void rollback() throws ResourceManagerException
        {
            if (localXid != null) {
                throw new IllegalStateException("Cannot rollback local transaction as an XA transaction is in progress.");
            }
            if (localContext == null) {
                throw new IllegalStateException("Cannot commit local transaction as no transaction was begun");
            }
            rollbackTransaction(localContext);
            localContext = null;
        }

        //
        // XAResource implementation
        //

        public boolean isSameRM(XAResource xares) throws XAException
        {
            return xares instanceof AbstractSession
                    && ((AbstractSession) xares).getResourceManager() == AbstractXAResourceManager.this;
        }

        public Xid[] recover(int flag) throws XAException
        {
            return null;
        }

        public void start(Xid xid, int flags) throws XAException
        {
            if (logger.isDebugEnabled()) {
                logger.debug(new StringBuffer(128).append("Thread ")
                                                  .append(Thread.currentThread())
                                                  .append(flags == TMNOFLAGS ? " starts" : flags == TMJOIN ? " joins"
                                                          : " resumes")
                                                  .append(" work on behalf of transaction branch ")
                                                  .append(xid)
                                                  .toString());
            }
            // A local transaction is already begun
            if (this.localContext != null) {
                throw new XAException(XAException.XAER_PROTO);
            }
            // This session has already been associated with an xid
            if (this.localXid != null) {
                throw new XAException(XAException.XAER_PROTO);
            }
            switch (flags) {
            // a new transaction
            case TMNOFLAGS:
            case TMJOIN:
            default:
                try {
                    localContext = createTransactionContext(this);
                    beginTransaction(localContext);
                } catch (Exception e) {
                    logger.error("Could not create new transactional resource", e);
                    throw new XAException(e.getMessage());
                }
                break;
            case TMRESUME:
                localContext = getSuspendedTransactionalResource(xid);
                if (localContext == null) {
                    throw new XAException(XAException.XAER_NOTA);
                }
                // TODO: resume context
                removeSuspendedTransactionalResource(xid);
                break;
            }
            localXid = xid;
            addActiveTransactionalResource(localXid, localContext);
        }

        public void end(Xid xid, int flags) throws XAException
        {
            if (logger.isDebugEnabled()) {
                logger.debug(new StringBuffer(128).append("Thread ")
                                                  .append(Thread.currentThread())
                                                  .append(flags == TMSUSPEND ? " suspends" : flags == TMFAIL ? " fails"
                                                          : " ends")
                                                  .append(" work on behalf of transaction branch ")
                                                  .append(xid)
                                                  .toString());
            }
            // No transaction is already begun
            if (localContext == null) {
                throw new XAException(XAException.XAER_NOTA);
            }
            // This session has already been associated with an xid
            if (localXid == null || !localXid.equals(xid)) {
                throw new XAException(XAException.XAER_PROTO);
            }

            try {
                switch (flags) {
                case TMSUSPEND:
                    // TODO: suspend context
                    addSuspendedTransactionalResource(localXid, localContext);
                    removeActiveTransactionalResource(localXid);
                    break;
                case TMFAIL:
                    setTransactionRollbackOnly(localContext);
                    break;
                case TMSUCCESS:
                    break;
                }
            } catch (ResourceManagerException e) {
                throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
            }
            localXid = null;
            localContext = null;
        }

        public void commit(Xid xid, boolean onePhase) throws XAException
        {
            if (xid == null) {
                throw new XAException(XAException.XAER_PROTO);
            }
            AbstractTransactionContext context = getActiveTransactionalResource(xid);
            if (context == null) {
                throw new XAException(XAException.XAER_NOTA);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Committing transaction branch " + xid);
            }
            if (context.status == Status.STATUS_MARKED_ROLLBACK) {
                throw new XAException(XAException.XA_RBROLLBACK);
            }

            try {
                if (context.status != Status.STATUS_PREPARED) {
                    if (onePhase) {
                        prepareTransaction(context);
                    } else {
                        throw new XAException(XAException.XAER_PROTO);
                    }
                }
                commitTransaction(context);
            } catch (ResourceManagerException e) {
                throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
            }
            removeActiveTransactionalResource(xid);
            removeSuspendedTransactionalResource(xid);
        }

        public void rollback(Xid xid) throws XAException
        {
            if (xid == null) {
                throw new XAException(XAException.XAER_PROTO);
            }
            AbstractTransactionContext context = getActiveTransactionalResource(xid);
            if (context == null) {
                throw new XAException(XAException.XAER_NOTA);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Rolling back transaction branch " + xid);
            }
            try {
                rollbackTransaction(context);
            } catch (ResourceManagerException e) {
                throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
            }
            removeActiveTransactionalResource(xid);
            removeSuspendedTransactionalResource(xid);
        }

        public int prepare(Xid xid) throws XAException
        {
            if (xid == null) {
                throw new XAException(XAException.XAER_PROTO);
            }
            AbstractTransactionContext context = getTransactionalResource(xid);
            if (context == null) {
                throw new XAException(XAException.XAER_NOTA);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Preparing transaction branch " + xid);
            }
            if (context.status == Status.STATUS_MARKED_ROLLBACK) {
                throw new XAException(XAException.XA_RBROLLBACK);
            }
            try {
                int result = prepareTransaction(context);
                return result;
            } catch (ResourceManagerException e) {
                throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
            }
        }

        public void forget(Xid xid) throws XAException
        {
            if (logger.isDebugEnabled()) {
                logger.debug("Forgetting transaction branch " + xid);
            }
            AbstractTransactionContext context = getTransactionalResource(xid);
            if (context == null) {
                throw new XAException(XAException.XAER_NOTA);
            }
            removeActiveTransactionalResource(xid);
            removeSuspendedTransactionalResource(xid);
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.transaction.xa.XAResource#getTransactionTimeout()
         */
        public int getTransactionTimeout() throws XAException
        {
            return (int) (getDefaultTransactionTimeout() / 1000);
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.transaction.xa.XAResource#setTransactionTimeout(int)
         */
        public boolean setTransactionTimeout(int timeout) throws XAException
        {
            setDefaultTransactionTimeout(timeout * 1000);
            return false;
        }

    }

    protected boolean includeBranchInXid()
    {
        return true;
    }

    protected AbstractTransactionContext getTransactionalResource(Xid xid)
    {
        AbstractTransactionContext context = getActiveTransactionalResource(xid);
        if (context != null) {
            return context;
        } else {
            return getSuspendedTransactionalResource(xid);
        }
    }

    protected AbstractTransactionContext getActiveTransactionalResource(Xid xid)
    {
        return (AbstractTransactionContext) activeContexts.get(xid);
    }

    protected AbstractTransactionContext getSuspendedTransactionalResource(Xid xid)
    {
        return (AbstractTransactionContext) suspendedContexts.get(xid);
    }

    protected void addActiveTransactionalResource(Xid xid, AbstractTransactionContext context)
    {
        activeContexts.put(xid, context);
    }

    protected void addSuspendedTransactionalResource(Xid xid, AbstractTransactionContext context)
    {
        suspendedContexts.put(xid, context);
    }

    protected void removeActiveTransactionalResource(Xid xid)
    {
        activeContexts.remove(xid);
    }

    protected void removeSuspendedTransactionalResource(Xid xid)
    {
        suspendedContexts.remove(xid);
    }

}
