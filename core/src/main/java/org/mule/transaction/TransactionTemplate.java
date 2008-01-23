/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction;

import org.mule.api.MuleContext;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionCallback;
import org.mule.api.transaction.TransactionConfig;
import org.mule.config.i18n.CoreMessages;

import java.beans.ExceptionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TransactionTemplate
{
    private static final Log logger = LogFactory.getLog(TransactionTemplate.class);

    private final TransactionConfig config;
    private final ExceptionListener exceptionListener;
    private final MuleContext context;

    public TransactionTemplate(TransactionConfig config, ExceptionListener listener, MuleContext context)
    {
        this.config = config;
        exceptionListener = listener;
        this.context = context;
    }

    public Object execute(TransactionCallback callback) throws Exception
    {
        if (config == null)
        {
            return callback.doInTransaction();
        }
        else
        {
            byte action = config.getAction();
            Transaction tx = TransactionCoordination.getInstance().getTransaction();

            if (action == TransactionConfig.ACTION_NONE && tx != null)
            {
                //TODO RM*: I'm not sure there is any value in throwing an exection here, since
                //there may be a transaction in progress but has nothing to to with this invocation
                //so maybe we just process outside the tx. Not sure yet
                //return callback.doInTransaction();

                /*
                    Reply from AP: There is value at the moment, at least in having fewer surprises
                    with a more explicit config. Current behavior is that of 'Never' TX attribute
                    in Java EE parlance.

                    What you refer to, however, is the 'Not Supported' TX behavior. A SUSPEND is performed
                    in this case with (optional) RESUME later.

                    Revamping/enhancing the TX attributes in Mule is coming next on my action list for
                    transactions in Mule after bringing Atomikos & ArjunaTS on-board and ditching a broken JOTM.
                 */

                throw new IllegalTransactionStateException(
                    CoreMessages.transactionAvailableButActionIs("None"));
            }
            else if (action == TransactionConfig.ACTION_ALWAYS_BEGIN && tx != null)
            {
                throw new IllegalTransactionStateException(
                    CoreMessages.transactionAvailableButActionIs("Always Begin"));
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
                logger.debug("Transaction successfully started");
            }
            else
            {
                tx = null;
            }
            try
            {
                Object result = callback.doInTransaction();
                if (tx != null)
                {
                    if (tx.isRollbackOnly())
                    {
                        logger.debug("Transaction is marked for rollback");
                        tx.rollback();
                    }
                    else
                    {
                        logger.debug("Committing transaction " + tx);
                        tx.commit();
                    }
                }
                return result;
            }
            catch (Exception e)
            {
                if (exceptionListener != null)
                {
                    logger
                        .info("Exception Caught in Transaction template.  Handing off to exception handler: "
                                        + exceptionListener);
                    exceptionListener.exceptionThrown(e);
                }
                else
                {
                    logger
                        .info("Exception Caught in Transaction template without any exception listeners defined, exception is rethrown.");
                    if (tx != null)
                    {
                        tx.setRollbackOnly();
                    }
                }
                if (tx != null)
                {
                    // The exception strategy can choose to route exception
                    // messages
                    // as part of the current transaction. So only rollback the
                    // tx
                    // if it has been marked for rollback (which is the default
                    // case in the
                    // AbstractExceptionListener)
                    if (tx.isRollbackOnly())
                    {
                        logger.debug("Exception caught: rollback transaction", e);
                        tx.rollback();
                    }
                    else
                    {
                        tx.commit();
                    }
                }
                // we've handled this exception above. just return null now
                if (exceptionListener != null)
                {
                    return null;
                }
                else
                {
                    throw e;
                }
            }
            catch (Error e)
            {
                if (tx != null)
                {
                    logger.info("Error caught: rollback transaction", e);
                    tx.rollback();
                }
                throw e;
            }
        }
    }

}
