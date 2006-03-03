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

import org.apache.commons.logging.Log;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;

import javax.transaction.Status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * This code is based on code coming from the <a
 * href="http://jakarta.apache.org/commons/transaction/">commons-transaction</a>
 * project.
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public abstract class AbstractResourceManager
{

    /**
     * Shutdown mode: Wait for all transactions to complete
     */
    public static final int SHUTDOWN_MODE_NORMAL = 0;

    /**
     * Shutdown mode: Try to roll back all active transactions
     */
    public static final int SHUTDOWN_MODE_ROLLBACK = 1;

    /**
     * Shutdown mode: Try to stop active transaction <em>NOW</em>, do no
     * rollbacks
     */
    public static final int SHUTDOWN_MODE_KILL = 2;

    protected static final int OPERATION_MODE_STOPPED = 0;
    protected static final int OPERATION_MODE_STOPPING = 1;
    protected static final int OPERATION_MODE_STARTED = 2;
    protected static final int OPERATION_MODE_STARTING = 3;
    protected static final int OPERATION_MODE_RECOVERING = 4;

    protected static final int DEFAULT_TIMEOUT_MSECS = 5000;
    protected static final int DEFAULT_COMMIT_TIMEOUT_FACTOR = 2;

    protected Collection globalTransactions = Collections.synchronizedCollection(new ArrayList());
    protected int operationMode = OPERATION_MODE_STOPPED;
    protected long defaultTimeout = DEFAULT_TIMEOUT_MSECS;
    protected Log logger = getLogger();
    protected boolean dirty = false;

    protected abstract Log getLogger();

    public synchronized void start() throws ResourceManagerSystemException
    {
        logger.info("Starting ResourceManager");
        operationMode = OPERATION_MODE_STARTING;
        // TODO: recover and sync
        doStart();
        recover();
        // sync();
        operationMode = OPERATION_MODE_STARTED;
        if (dirty) {
            logger.warn("Started ResourceManager, but in dirty mode only (Recovery of pending transactions failed)");
        } else {
            logger.info("Started ResourceManager");
        }
    }

    protected void doStart() throws ResourceManagerSystemException
    {
    }

    protected void recover() throws ResourceManagerSystemException
    {
    }

    public synchronized void stop() throws ResourceManagerSystemException
    {
        stop(SHUTDOWN_MODE_NORMAL);
    }

    public synchronized boolean stop(int mode) throws ResourceManagerSystemException
    {
        return stop(mode, getDefaultTransactionTimeout() * DEFAULT_COMMIT_TIMEOUT_FACTOR);
    }

    public synchronized boolean stop(int mode, long timeOut) throws ResourceManagerSystemException
    {
        logger.info("Stopping ResourceManager");
        operationMode = OPERATION_MODE_STOPPING;
        // TODO: sync
        // sync();
        boolean success = shutdown(mode, timeOut);
        // TODO: release
        // releaseGlobalOpenResources();
        if (success) {
            operationMode = OPERATION_MODE_STOPPED;
            logger.info("Stopped ResourceManager");
        } else {
            logger.warn("Failed to stop ResourceManager");
        }

        return success;
    }

    protected boolean shutdown(int mode, long timeoutMSecs)
    {
        switch (mode) {
        case SHUTDOWN_MODE_NORMAL:
            return waitForAllTxToStop(timeoutMSecs);
        case SHUTDOWN_MODE_ROLLBACK:
            throw new UnsupportedOperationException();
        // return rollBackOrForward();
        case SHUTDOWN_MODE_KILL:
            return true;
        default:
            return false;
        }
    }

    /**
     * Gets the default transaction timeout in <em>milliseconds</em>.
     */
    public long getDefaultTransactionTimeout()
    {
        return defaultTimeout;
    }

    /**
     * Sets the default transaction timeout.
     * 
     * @param timeout timeout in <em>milliseconds</em>
     */
    public void setDefaultTransactionTimeout(long timeout)
    {
        defaultTimeout = timeout;
    }

    /**
     * Starts a new transaction and associates it with the current thread. All
     * subsequent changes in the same thread made to the map are invisible from
     * other threads until {@link #commitTransaction()} is called. Use
     * {@link #rollbackTransaction()} to discard your changes. After calling
     * either method there will be no transaction associated to the current
     * thread any longer. <br>
     * <br>
     * <em>Caution:</em> Be careful to finally call one of those methods, as
     * otherwise the transaction will lurk around for ever.
     * 
     * @see #prepareTransaction()
     * @see #commitTransaction()
     * @see #rollbackTransaction()
     */
    public AbstractTransactionContext startTransaction(Object session) throws ResourceManagerException
    {
        AbstractTransactionContext context = createTransactionContext(session);
        return context;
    }

    public void beginTransaction(AbstractTransactionContext context) throws ResourceManagerException
    {
        assureStarted(); // can only start a new transaction when not already
        // stopping
        synchronized (context) {
            if (logger.isDebugEnabled()) {
                logger.debug("Beginning transaction " + context);
            }
            doBegin(context);
            context.status = Status.STATUS_ACTIVE;
            if (logger.isDebugEnabled()) {
                logger.debug("Began transaction " + context);
            }
        }
        globalTransactions.add(context);
    }

    public int prepareTransaction(AbstractTransactionContext context) throws ResourceManagerException
    {
        assureReady();
        synchronized (context) {
            if (logger.isDebugEnabled()) {
                logger.debug("Preparing transaction " + context);
            }
            context.status = Status.STATUS_PREPARING;
            int status = doPrepare(context);
            context.status = Status.STATUS_PREPARED;
            if (logger.isDebugEnabled()) {
                logger.debug("Prepared transaction " + context);
            }
            return status;
        }
    }

    public void rollbackTransaction(AbstractTransactionContext context) throws ResourceManagerException
    {
        assureReady();
        synchronized (context) {
            if (logger.isDebugEnabled()) {
                logger.debug("Rolling back transaction " + context);
            }
            try {
                context.status = Status.STATUS_ROLLING_BACK;
                doRollback(context);
                context.status = Status.STATUS_ROLLEDBACK;
            } catch (Error e) {
                setDirty(context, e);
                throw e;
            } catch (RuntimeException e) {
                setDirty(context, e);
                throw e;
            } catch (ResourceManagerSystemException e) {
                setDirty(context, e);
                throw e;
            } finally {
                globalTransactions.remove(context);
                context.finalCleanUp();
                // tell shutdown thread this tx is finished
                context.notifyFinish();
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Rolled back transaction " + context);
            }
        }
    }

    public void setTransactionRollbackOnly(AbstractTransactionContext context) throws ResourceManagerException
    {
        context.status = Status.STATUS_MARKED_ROLLBACK;
    }

    public void commitTransaction(AbstractTransactionContext context) throws ResourceManagerException
    {
        assureReady();
        if (context.status == Status.STATUS_MARKED_ROLLBACK) {
            throw new ResourceManagerException(new Message(Messages.TX_MARKED_FOR_ROLLBACK));
        }
        synchronized (context) {
            if (logger.isDebugEnabled()) {
                logger.debug("Committing transaction " + context);
            }
            try {
                context.status = Status.STATUS_COMMITTING;
                doCommit(context);
                context.status = Status.STATUS_COMMITTED;
            } catch (Error e) {
                setDirty(context, e);
                throw e;
            } catch (RuntimeException e) {
                setDirty(context, e);
                throw e;
            } catch (ResourceManagerSystemException e) {
                setDirty(context, e);
                throw e;
            } catch (ResourceManagerException e) {
                logger.warn("Could not commit tx " + context + ", rolling back instead", e);
                doRollback(context);
            } finally {
                globalTransactions.remove(context);
                context.finalCleanUp();
                // tell shutdown thread this tx is finished
                context.notifyFinish();
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Committed transaction " + context);
            }
        }
    }

    protected abstract AbstractTransactionContext createTransactionContext(Object session);

    protected abstract void doBegin(AbstractTransactionContext context);

    protected abstract int doPrepare(AbstractTransactionContext context);

    protected abstract void doCommit(AbstractTransactionContext context) throws ResourceManagerException;

    protected abstract void doRollback(AbstractTransactionContext context) throws ResourceManagerException;

    // TODO
    // protected boolean rollBackOrForward() {
    // }

    protected boolean waitForAllTxToStop(long timeoutMSecs)
    {
        long startTime = System.currentTimeMillis();

        // be sure not to lock globalTransactions for too long, as we need to
        // give
        // txs the chance to complete (otherwise deadlocks are very likely to
        // occur)
        // instead iterate over a copy as we can be sure no new txs will be
        // registered
        // after operation level has been set to stopping

        Collection transactionsToStop;
        synchronized (globalTransactions) {
            transactionsToStop = new ArrayList(globalTransactions);
        }
        for (Iterator it = transactionsToStop.iterator(); it.hasNext();) {
            long remainingTimeout = startTime - System.currentTimeMillis() + timeoutMSecs;

            if (remainingTimeout <= 0) {
                return false;
            }

            AbstractTransactionContext context = (AbstractTransactionContext) it.next();
            synchronized (context) {
                if (!context.finished) {
                    logger.info("Waiting for tx " + context + " to finish for " + remainingTimeout + " milli seconds");
                }
                while (!context.finished && remainingTimeout > 0) {
                    try {
                        context.wait(remainingTimeout);
                    } catch (InterruptedException e) {
                        return false;
                    }
                    remainingTimeout = startTime - System.currentTimeMillis() + timeoutMSecs;
                }
                if (context.finished) {
                    logger.info("Tx " + context + " finished");
                } else {
                    logger.warn("Tx " + context + " failed to finish in given time");
                }
            }
        }

        return (globalTransactions.size() == 0);
    }

    /**
     * Flag this resource manager as dirty. No more operations will be allowed
     * until a recovery has been successfully performed.
     * 
     * @param context
     * @param t
     */
    protected void setDirty(AbstractTransactionContext context, Throwable t)
    {
        logger.error("Fatal error during critical commit/rollback of transaction " + context
                + ", setting resource manager to dirty.", t);
        dirty = true;
    }

    /**
     * Check that the FileManager is started.
     * 
     * @throws FileManagerSystemException if the FileManager is not started.
     */
    protected void assureStarted() throws ResourceManagerSystemException
    {
        if (operationMode != OPERATION_MODE_STARTED) {
            throw new ResourceManagerSystemException(new Message(Messages.RESOURCE_MANAGER_NOT_STARTED));
        }
        // do not allow any further writing or commit or rollback when db is
        // corrupt
        if (dirty) {
            throw new ResourceManagerSystemException(new Message(Messages.RESOURCE_MANAGER_DIRTY));
        }
    }

    /**
     * Check that the FileManager is ready.
     * 
     * @throws FileManagerSystemException if the FileManager is neither started
     *             not stopping.
     */
    protected void assureReady() throws ResourceManagerSystemException
    {
        if (operationMode != OPERATION_MODE_STARTED && operationMode != OPERATION_MODE_STOPPING) {
            throw new ResourceManagerSystemException(new Message(Messages.RESOURCE_MANAGER_NOT_READY));
        }
        // do not allow any further writing or commit or rollback when db is
        // corrupt
        if (dirty) {
            throw new ResourceManagerSystemException(new Message(Messages.RESOURCE_MANAGER_DIRTY));
        }
    }

}
