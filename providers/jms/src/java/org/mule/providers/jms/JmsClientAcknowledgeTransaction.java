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
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionException;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * <p><code>JmsClientAcknowledgeTransaction</code> is a transaction implementation of performing
 * a message acknowledgement.  There is no notion of rollback with client acknowledgement, but this
 * transaction can be useful for controlling how messages are consumed from a destination.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class JmsClientAcknowledgeTransaction implements UMOTransaction
{
    /** logger used by this class */
    protected static final transient Log logger = LogFactory.getLog(JmsClientAcknowledgeTransaction.class);

    public static final int STATUS_NO_TRANSACTION = 6;
    public static final int STATUS_COMMITTED = 3;
    public static final int STATUS_ROLLED_BACK = 4;
    public static final int STATUS_ACTIVE = 0;

    private Message message;
    private SynchronizedBoolean started = new SynchronizedBoolean(false);
    private SynchronizedBoolean committed = new SynchronizedBoolean(false);

    /**
     *
     */
    public JmsClientAcknowledgeTransaction(Message message) throws IllegalTransactionStateException
    {
        this.message = message;
//        try
//        {
//            try
//            {
//                if (message.getJMSDeliveryMode() != Session.CLIENT_ACKNOWLEDGE)
//                {
//                    throw new IllegalTransactionStateException("Jms message is not in client acknowlegement mode.  Check the endpoint connector config");
//                }
//            }
//            catch (JMSException e)
//            {
//                throw new IllegalTransactionStateException("Failed to read Jms Delivery mode property: " + e.getMessage(), e);
//            }
//        }
//        catch (IllegalTransactionStateException e)
//        {
//            throw e;
//        }

    }

    public void setRollbackOnly()
    {
        throw new UnsupportedOperationException("Jms Client Acknowledge doesn't support rollback");
    }

    public Object getResource()
    {
        return message;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOTransaction#commit()
     */
    public void commit() throws UMOTransactionException
    {
        try
        {
            logger.debug("Committing transaction");

            message.acknowledge();
            committed.set(true);
        }
        catch (JMSException e)
        {
            throw new IllegalTransactionStateException("Failed to commit jms Client Acknowledge transaction: " + e.getMessage(), e);
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
//        try
//        {
//            if (message.getJMSDeliveryMode() != Session.CLIENT_ACKNOWLEDGE)
//            {
//                throw new IllegalTransactionStateException("Jms message is not in Client Acknowledgement mode.  Check the endpoint config");
//            }
            logger.debug("Beginning transaction");
            started.set(true);
//        }
//        catch (JMSException e)
//        {
//            throw new IllegalTransactionStateException("Failed to read Jms message Delivery mode property: " + e.getMessage(), e);
//        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOTransaction#isRolledBack()
     */
    public boolean isRolledBack() throws TransactionStatusException
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOTransaction#rollback()
     */
    public void rollback() throws TransactionRollbackException
    {
        throw new UnsupportedOperationException("Jms Client Acknowledge doesn't support rollback");
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
        return false;
    }
}
