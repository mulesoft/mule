/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.transaction.*;
import org.mule.config.i18n.CoreMessages;

public class TransactionTemplate<T>
{

    private static final Log logger = LogFactory.getLog(TransactionTemplate.class);

    private final TransactionConfig config;
    private final MuleContext context;

    public TransactionTemplate(TransactionConfig config, MuleContext context)
    {
        this.config = config;
        this.context = context;
    }

    public T execute(TransactionCallback<T> callback) throws Exception
    {
        //if we want to skip TT
        if (config == null)
        {
            return callback.doInTransaction();
        }

        Transaction joinedExternal = null;
        byte action = (config != null) ? config.getAction() : TransactionConfig.ACTION_DEFAULT;
        Transaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx == null && context != null && config != null && config.isInteractWithExternal())
        {

            TransactionFactory tmFactory = config.getFactory();
            if (tmFactory instanceof ExternalTransactionAwareTransactionFactory)
            {
                ExternalTransactionAwareTransactionFactory extmFactory =
                    (ExternalTransactionAwareTransactionFactory) tmFactory;
                joinedExternal = tx = extmFactory.joinExternalTransaction(context);
            }
        }

        Transaction suspendedXATx = null;

        if (action == TransactionConfig.ACTION_NEVER && tx != null)
        {
            throw new IllegalTransactionStateException(
                CoreMessages.transactionAvailableButActionIs("Never"));
        }
        else if ((action == TransactionConfig.ACTION_NONE || action == TransactionConfig.ACTION_ALWAYS_BEGIN)
                   && tx != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(action + ", " + "current TX: " + tx);
            }

            if (tx.isXA())
            {
                // suspend current transaction
                suspendedXATx = tx;
                suspendXATransaction(suspendedXATx);
            }
            else
            {
                // commit/rollback
                resolveTransaction(tx);
            }
            //transaction will be started below
            tx = null;
        }
        else if (action == TransactionConfig.ACTION_ALWAYS_JOIN && tx == null)
        {
            throw new IllegalTransactionStateException(
                CoreMessages.transactionNotAvailableButActionIs("Always Join"));
        }

        if (action == TransactionConfig.ACTION_ALWAYS_BEGIN
            || (action == TransactionConfig.ACTION_BEGIN_OR_JOIN && tx == null))
        {
            logger.debug("Beginning transaction");
            tx = config.getFactory().beginTransaction(context);
            logger.debug("Transaction successfully started: " + tx);
        }
        else
        {
            tx = null;
        }

        T result = callback.doInTransaction();
        if (tx != null)
        {
            //verify that transaction is still active
            tx = TransactionCoordination.getInstance().getTransaction();
        }
        if (tx != null)
        {
            resolveTransaction(tx);
        }
        if (suspendedXATx != null)
        {
            resumeXATransaction(suspendedXATx);
            tx = suspendedXATx;
        }
        if (joinedExternal != null)
        {
            TransactionCoordination.getInstance().unbindTransaction(joinedExternal);
        }
        return result;
    }

    protected void resolveTransaction(Transaction tx) throws TransactionException
    {
        if (tx.isRollbackOnly())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Transaction has been marked rollbackOnly, rolling it back: " + tx);
            }
            tx.rollback();
        } else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Committing transaction " + tx);
            }
            tx.commit();
        }
    }

    protected void suspendXATransaction(Transaction tx) throws TransactionException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Suspending " + tx);
        }

        tx.suspend();

        if (logger.isDebugEnabled())
        {
            logger.debug("Successfully suspended " + tx);
            logger.debug("Unbinding the following TX from the current context: " + tx);
        }

        TransactionCoordination.getInstance().unbindTransaction(tx);
    }

    protected void resumeXATransaction(Transaction tx) throws TransactionException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Re-binding and Resuming " + tx);
        }

        TransactionCoordination.getInstance().bindTransaction(tx);
        tx.resume();
    }

}

