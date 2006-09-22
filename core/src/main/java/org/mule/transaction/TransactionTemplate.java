/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionConfig;

import java.beans.ExceptionListener;

/**
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class TransactionTemplate
{

    private static final transient Log logger = LogFactory.getLog(TransactionTemplate.class);

    private UMOTransactionConfig config;

    private ExceptionListener exceptionListener;

    public TransactionTemplate(UMOTransactionConfig config, ExceptionListener listener)
    {
        this.config = config;
        exceptionListener = listener;
    }

    public Object execute(TransactionCallback callback) throws Exception
    {
        if (config == null) {
            return callback.doInTransaction();
        } else {
            byte action = config.getAction();
            UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();

            if (action == UMOTransactionConfig.ACTION_NONE && tx != null) {
                throw new IllegalTransactionStateException(new Message(Messages.TX_AVAILABLE_BUT_ACTION_IS_X, "None"));
            } else if (action == UMOTransactionConfig.ACTION_ALWAYS_BEGIN && tx != null) {
                throw new IllegalTransactionStateException(new Message(Messages.TX_AVAILABLE_BUT_ACTION_IS_X,
                                                                       "Always Begin"));
            } else if (action == UMOTransactionConfig.ACTION_ALWAYS_JOIN && tx == null) {
                throw new IllegalTransactionStateException(new Message(Messages.TX_NOT_AVAILABLE_BUT_ACTION_IS_X,
                                                                       "Always Join"));
            }

            if (action == UMOTransactionConfig.ACTION_ALWAYS_BEGIN
                    || action == UMOTransactionConfig.ACTION_BEGIN_OR_JOIN) {
                logger.debug("Beginning transaction");
                tx = config.getFactory().beginTransaction();
                logger.debug("Transaction successfully started");
            } else {
                tx = null;
            }
            try {
                Object result = callback.doInTransaction();
                if (tx != null) {
                    if (tx.isRollbackOnly()) {
                        logger.debug("Transaction is marked for rollback");
                        tx.rollback();
                    } else {
                        logger.debug("Committing transaction");
                        tx.commit();
                    }
                }
                return result;
            } catch (Exception e) {
                if (exceptionListener != null) {
                    logger.info("Exception Caught in Transaction template.  Handing off to exception handler: "
                            + exceptionListener);
                    exceptionListener.exceptionThrown(e);
                } else {
                    logger.info("Exception Caught in Transaction template without any exception listeners defined, exception is rethrown.");
                    if (tx != null) {
                        tx.setRollbackOnly();
                    }
                }
                if (tx != null) {
                    // The exception strategy can choose to route exception
                    // messages
                    // as part of the current transaction. So only rollback the
                    // tx
                    // if it has been marked for rollback (which is the default
                    // case in the
                    // AbstractExceptionListener)
                    if (tx.isRollbackOnly()) {
                        logger.debug("Exception caught: rollback transaction", e);
                        tx.rollback();
                    } else {
                        tx.commit();
                    }
                }
                // we've handled this exception above. just return null now
                if (exceptionListener != null) {
                    return null;
                } else {
                    throw e;
                }
            } catch (Error e) {
                if (tx != null) {
                    logger.info("Error caught: rollback transaction", e);
                    tx.rollback();
                }
                throw e;
            }
        }
    }

}
