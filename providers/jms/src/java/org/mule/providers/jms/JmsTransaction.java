/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.providers.jms;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transaction.IllegalTransactionStateException;
import org.mule.transaction.TransactionRollbackException;
import org.mule.transaction.TransactionStatusException;
import org.mule.transaction.constraints.ConstraintFilter;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionException;

import javax.jms.JMSException;
import javax.jms.Session;

/**
 * <p><code>JmsTransaction</code> is a wrapper for a Jms local transaction.  This object holds the
 * jms session and controls the when the transaction committed or rolled back.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class JmsTransaction implements UMOTransaction
{
    /** logger used by this class */
    protected static final transient Log logger = LogFactory.getLog(JmsTransaction.class);

    public static final int STATUS_NO_TRANSACTION = 6;
    public static final int STATUS_COMMITTED = 3;
    public static final int STATUS_ROLLED_BACK = 4;
    public static final int STATUS_ACTIVE = 0;

    private Session jmsSession;
    private SynchronizedBoolean started = new SynchronizedBoolean(false);
    private SynchronizedBoolean committed = new SynchronizedBoolean(false);
    private SynchronizedBoolean rolledBack = new SynchronizedBoolean(false);
    private SynchronizedBoolean rollbackOnly = new SynchronizedBoolean(false);

    /**
     * 
     */
    public JmsTransaction(Session session) throws IllegalTransactionStateException
    {
        jmsSession = session;
        try
        {
            try
            {
                if (jmsSession.getTransacted() == false)
                {
                    throw new IllegalTransactionStateException("Jms session is not transacted.  Check the endpoint config");
                }
            }
            catch (JMSException e)
            {
                throw new IllegalTransactionStateException("Failed to read Jms session transacted property: " + e.getMessage(), e);
            }
        }
        catch (IllegalTransactionStateException e)
        {
            throw e;
        }
        catch (UMOException e)
        {
            throw new IllegalTransactionStateException("Unable to obtain Jms session: " + e.getMessage(), e);
        }
    }

    public void setRollbackOnly()
    {
        rollbackOnly.set(true);
    }

    public Object getResource()
    {
        return jmsSession;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOTransaction#commit()
     */
    public void commit() throws UMOTransactionException
    {
        try
        {
            logger.debug("Committing transaction");
            if (rollbackOnly.get())
            {
                throw new TransactionRollbackException("Transaction is marked for rollback only");
            }
            jmsSession.commit();
            committed.commit(false, true);
        }
        catch (JMSException e)
        {
            throw new IllegalTransactionStateException("Failed to commit jms transaction: " + e.getMessage(), e);
        }
    }

    public void commit(UMOEvent event, ConstraintFilter constraint) throws UMOTransactionException
    {
        if(constraint==null || constraint.accept(event))
        {
            commit();
        } else {
            logger.debug("postponing transaction commit until contraints have been met");
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOTransaction#getStatus()
     */
    public int getStatus() throws TransactionStatusException
    {
        if (rolledBack.get()) return STATUS_ROLLED_BACK;
        if (committed.get()) return STATUS_COMMITTED;
        if (started.get()) return STATUS_ACTIVE;
        return STATUS_NO_TRANSACTION;
    }

    public boolean isBegun()
    {
        return started.get();
    }

    public void begin() throws UMOTransactionException
    {
        try
        {
            if (jmsSession.getTransacted() == false)
            {
                throw new IllegalTransactionStateException("Jms session is not transacted.  Check the endpoint config");
            }
            logger.debug("Beginning transaction");
            started.commit(false, true);
        }
        catch (JMSException e)
        {
            throw new IllegalTransactionStateException("Failed to read Jms session transacted property: " + e.getMessage(), e);
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOTransaction#isRolledBack()
     */
    public boolean isRolledBack() throws TransactionStatusException
    {
        return rolledBack.get();
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOTransaction#rollback()
     */
    public void rollback() throws TransactionRollbackException
    {
        try
        {
            logger.debug("Rolling back transaction");
            setRollbackOnly();
            jmsSession.rollback();
            rolledBack.commit(false, true);
        }
        catch (JMSException e)
        {
            throw new TransactionRollbackException("Failed to rollback jms transaction: " + e.getMessage(), e);
        }

    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOTransaction#isCommitted()
     */
    public boolean isCommitted() throws TransactionStatusException
    {
        return committed.get();
    }

    public boolean isRollbackOnly()
    {
        return rollbackOnly.get();
    }
}
