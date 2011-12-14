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
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.transaction.*;
import org.mule.config.i18n.CoreMessages;
import org.mule.exception.AlreadyHandledMessagingException;

public class TransactionTemplate<T>
{
    private static final Log logger = LogFactory.getLog(TransactionTemplate.class);
    private final TransactionConfig config;
    private final MuleContext context;
    private TransactionInterceptor<T> transactionInterceptor;

    TransactionTemplate(MuleContext context, TransactionConfig config)
    {
        this.config = config;
        this.context = context;
    }

    /**
     * Use {@link org.mule.transaction.TransactionTemplateFactory} instead
     */
    @Deprecated
    public TransactionTemplate(TransactionConfig config, MuleContext context)
    {
        this(config, context, false);
    }

    /**
     * Use {@link org.mule.transaction.TransactionTemplateFactory} instead
     */
    @Deprecated
    public TransactionTemplate(TransactionConfig config, MuleContext context, boolean manageExceptions)
    {
        this.config = config;
        this.context = context;
        this.transactionInterceptor = new ExecuteCallbackInterceptor();
        if (manageExceptions)
        {
            this.transactionInterceptor = new HandleExceptionInterceptor(transactionInterceptor);
        }
        if (config != null && config.getAction() != TransactionConfig.ACTION_INDIFFERENT)
        {
            this.transactionInterceptor = new ExternalTransactionInterceptor(
                        new ValidateTransactionalStateInterceptor(
                                new SuspendXaTransactionInterceptor(
                                        new ResolveTransactionInterceptor(
                                                new BeginTransactionInterceptor(this.transactionInterceptor)))));
        }
    }

    TransactionTemplate(MuleContext muleContext)
    {
        this.context = muleContext;
        this.config = null;
    }

    void setTransactionInterceptor(TransactionInterceptor<T> transactionInterceptor)
    {
        this.transactionInterceptor = transactionInterceptor;
    }

    public T execute(TransactionCallback<T> callback) throws Exception
    {
        return transactionInterceptor.execute(callback);
    }

    public class HandleExceptionInterceptor implements TransactionInterceptor<T>
    {
        public TransactionInterceptor<T> next;

        public HandleExceptionInterceptor(TransactionInterceptor next)
        {
            this.next = next;
        }

        @Override
        public T execute(TransactionCallback<T> callback) throws Exception
        {
            try
            {
                return next.execute(callback);
            }
            catch (AlreadyHandledMessagingException e)
            {
                throw e;
            }
            catch (MessagingException e)
            {
                //TODO verify that always we get a MessagingException. In case of any other type of execution should the tx be mark as rollback?
                T result = (T) e.getEvent().getFlowConstruct().getExceptionListener().handleException(e, e.getEvent());
                //TODO uncomment this lines once exception re-thrown is in place
                /*MuleEvent exceptionListenerResult = (MuleEvent) result;
                if (exceptionListenerResult.getMessage().getExceptionPayload() != null)
                {
                    throw new AlreadyHandledMessagingException((MessagingException) exceptionListenerResult.getMessage().getExceptionPayload().getException());
                }*/
                return result;
            }
        }
    }

    public class UnwrapManagedExceptionInterceptor implements TransactionInterceptor<T>
    {
        public TransactionInterceptor<T> next;

        public UnwrapManagedExceptionInterceptor(TransactionInterceptor next)
        {
            this.next = next;
        }


        @Override
        public T execute(TransactionCallback<T> callback) throws Exception
        {
            try
            {
                return next.execute(callback);
            }
            catch (AlreadyHandledMessagingException messagingException)
            {
                throw (MessagingException)messagingException.getCause();
            }
        }
    }

    public class ExternalTransactionInterceptor implements TransactionInterceptor<T>
    {
        public TransactionInterceptor<T> next;

        public ExternalTransactionInterceptor(TransactionInterceptor next)
        {
            this.next = next;
        }

        @Override
        public T execute(TransactionCallback<T> callback) throws Exception
        {
            Transaction joinedExternal = null;
            Transaction tx = TransactionCoordination.getInstance().getTransaction();
            try
            {
                if (tx == null && context != null && config != null && config.isInteractWithExternal())
                {

                    TransactionFactory tmFactory = config.getFactory();
                    if (tmFactory instanceof ExternalTransactionAwareTransactionFactory)
                    {
                        ExternalTransactionAwareTransactionFactory externalTransactionFactory =
                            (ExternalTransactionAwareTransactionFactory) tmFactory;
                        joinedExternal = tx = externalTransactionFactory.joinExternalTransaction(context);
                    }
                }
                return next.execute(callback);
            }
            finally
            {
                if (joinedExternal != null)
                {
                    TransactionCoordination.getInstance().unbindTransaction(joinedExternal);
                }
            }
        }
    }

    public class SuspendXaTransactionInterceptor implements TransactionInterceptor<T>
    {
        public TransactionInterceptor<T> next;

        public SuspendXaTransactionInterceptor(TransactionInterceptor next)
        {
            this.next = next;
        }

        @Override
        public T execute(TransactionCallback<T> callback) throws Exception
        {
            Transaction suspendedXATx = null;
            Transaction tx = TransactionCoordination.getInstance().getTransaction();
            //TODO Uncomment try-finally once TransactionTemplate starts handling exceptions
            //try
            //{
                byte action = config.getAction();
                if ((action == TransactionConfig.ACTION_NONE || action == TransactionConfig.ACTION_ALWAYS_BEGIN)
                       && tx != null && tx.isXA())
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("suspending XA tx " + action + ", " + "current TX: " + tx);
                    }
                    suspendedXATx = tx;
                    suspendXATransaction(suspendedXATx);
                }
                T result = next.execute(callback);
            //}
            //finally
            //{
                //TODO rethink were it should be resumed - after resolve current tx maybe?
                if (suspendedXATx != null)
                {
                    resumeXATransaction(suspendedXATx);
                }
                return result;
            //}
        }
    }

    public class ValidateTransactionalStateInterceptor implements TransactionInterceptor<T>
    {
        public TransactionInterceptor<T> next;

        public ValidateTransactionalStateInterceptor(TransactionInterceptor next)
        {
            this.next = next;
        }

        @Override
        public T execute(TransactionCallback<T> callback) throws Exception
        {
            Transaction tx = TransactionCoordination.getInstance().getTransaction();
            if (config.getAction() == TransactionConfig.ACTION_NEVER && tx != null)
            {
                throw new IllegalTransactionStateException(
                    CoreMessages.transactionAvailableButActionIs("Never"));
            }
            else if (config.getAction() == TransactionConfig.ACTION_ALWAYS_JOIN && tx == null)
            {
                throw new IllegalTransactionStateException(
                    CoreMessages.transactionNotAvailableButActionIs("Always Join"));
            }
            return this.next.execute(callback);
        }
    }

    public class ResolveTransactionInterceptor implements TransactionInterceptor<T>
    {
        public TransactionInterceptor<T> next;

        public ResolveTransactionInterceptor(TransactionInterceptor next)
        {
            this.next = next;
        }

        @Override
        public T execute(TransactionCallback<T> callback) throws Exception
        {
            byte action = config.getAction();
            Transaction transactionBeforeTemplate = TransactionCoordination.getInstance().getTransaction();
            if ((action == TransactionConfig.ACTION_NONE || action == TransactionConfig.ACTION_ALWAYS_BEGIN)
                       && transactionBeforeTemplate != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(action + ", " + "current TX: " + transactionBeforeTemplate);
                }

                resolveTransaction(transactionBeforeTemplate);
            }
            T result = next.execute(callback);
            Transaction currentTransaction = TransactionCoordination.getInstance().getTransaction();
            if (currentTransaction != null && (config.getAction() == TransactionConfig.ACTION_ALWAYS_BEGIN || (config.getAction() == TransactionConfig.ACTION_BEGIN_OR_JOIN && transactionBeforeTemplate == null)))
            {
                resolveTransaction(currentTransaction);
            }
            return result;
        }
    }

    public class BeginTransactionInterceptor implements TransactionInterceptor<T>
    {
        public TransactionInterceptor<T> next;

        public BeginTransactionInterceptor(TransactionInterceptor next)
        {
            this.next = next;
        }

        @Override
        public T execute(TransactionCallback<T> callback) throws Exception
        {
            byte action = config.getAction();
            boolean transactionStarted = false;
            Transaction tx = TransactionCoordination.getInstance().getTransaction();
            if (action == TransactionConfig.ACTION_ALWAYS_BEGIN
                || (action == TransactionConfig.ACTION_BEGIN_OR_JOIN && tx == null))
            {
                logger.debug("Beginning transaction");
                tx = config.getFactory().beginTransaction(context);
                transactionStarted = true;
                logger.debug("Transaction successfully started: " + tx);
            }
            T result = next.execute(callback);
            Transaction transaction = TransactionCoordination.getInstance().getTransaction();
            if (transactionStarted && transaction != null)
            {
                resolveTransaction(transaction);
            }
            return result;
        }
    }

    public class ExecuteCallbackInterceptor implements TransactionInterceptor<T>
    {

        @Override
        public T execute(TransactionCallback<T> callback) throws Exception
        {
            return callback.doInTransaction();
        }
    }

    public interface TransactionInterceptor<T>
    {
        T execute(TransactionCallback<T> callback) throws Exception;
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
        }
        else
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

