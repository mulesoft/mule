/*
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 05-Feb-2004
 * Time: 22:53:23
 */
package org.mule.providers;

import org.mule.transaction.*;
import org.mule.umo.*;
import org.mule.umo.endpoint.UMOEndpoint;

public abstract class TransactionEnabledConnector extends AbstractServiceEnabledConnector
{
    public TransactionProxy beginTransaction(UMOEndpoint endpoint) throws UMOTransactionException
    {
        try
        {
            return beginTransaction(endpoint, getSession(endpoint));
        }
        catch (Exception e)
        {
            throw new IllegalTransactionStateException("Cannot obtains session for transaction");
        }
    }

    public TransactionProxy beginTransaction(UMOEndpoint endpoint, Object session) throws UMOTransactionException
    {
        TransactionProxy trans = TransactionCoordination.getInstance().getTransactionProxy();
        byte action = endpoint.getTransactionConfig().getBeginAction();
        UMOTransactionFactory factory = endpoint.getTransactionConfig().getFactory();

        if ((action == UMOTransactionConfig.ACTION_NONE ||
                action == UMOTransactionConfig.ACTION_JOIN_IF_POSSIBLE)
                && trans == null)
        {
            return null;
        }
        else if (action == UMOTransactionConfig.ACTION_JOIN_IF_POSSIBLE && trans != null)
        {
            return trans;
        }
        else if (action == UMOTransactionConfig.ACTION_NONE && trans != null)
        {
            throw new IllegalTransactionStateException("A transaction is available for this session, but transaction action is none");
        }
        else if ((action == UMOTransactionConfig.ACTION_BEGIN_OR_JOIN && trans == null) ||
                action == UMOTransactionConfig.ACTION_ALWAYS_BEGIN)
        {
            try
            {
                UMOTransaction tx = factory.beginTransaction(session);
                return TransactionCoordination.getInstance().bindTransaction(tx, endpoint.getTransactionConfig().getConstraint());
            }
            catch (Exception e)
            {
                throw new TransactionNotInProgressException("Failed to obtain endpoint session", e);
            }
        }
        else
        {
            throw new TransactionNotInProgressException("Cannot join transaction on an event received.");
        }
    }

    public void commitTransaction(UMOEvent event) throws UMOTransactionException
    {
        TransactionProxy trans = TransactionCoordination.getInstance().getTransactionProxy();

        if (trans != null && trans.isRollbackOnly())
        {
            rollbackTransaction(event);
            return;
        }

        UMOEndpoint endpoint = event.getEndpoint();
        byte action = endpoint.getTransactionConfig().getCommitAction();

        if ((action == UMOTransactionConfig.ACTION_NONE ||
                action == UMOTransactionConfig.ACTION_COMMIT_IF_POSSIBLE) && trans == null)
        {
            return;
        }
        else if (action == UMOTransactionConfig.ACTION_NONE && trans != null)
        {
            throw new IllegalTransactionStateException("A transaction is available for this session, but transaction action is NONE");
        }
        else if ((action == UMOTransactionConfig.ACTION_ALWAYS_COMMIT && trans == null))
        {
            throw new IllegalTransactionStateException("There isn't a transaction is available for this session, but transaction action is ALWAYS_COMMIT");
        }
        else if (trans == null && (action != UMOTransactionConfig.ACTION_NONE))
        {
            throw new TransactionNotInProgressException("There is no transaction associated with the session");
        }
        else if (trans != null && trans.isCommitted())
        {
            throw new TransactionNotInProgressException("The current transaction has already been committed");
        }
        else
        {
            trans.commit(event);
        }
    }

    public void rollbackTransaction(UMOEvent event) throws TransactionRollbackException, TransactionStatusException
    {
        UMOSession session = event.getSession();
        UMOTransaction trans = TransactionCoordination.getInstance().unbindTransaction();

        if (trans != null && trans.isRolledBack())
        {
            throw new TransactionRollbackException("Transaction for session: " + session.getId() + " has already been rolled back");
        }
        else if (trans == null)
        {
            throw new TransactionNotInProgressException("Cannot rollback transaction as there is no transaction in progress");
        }
        else
        {
            trans.rollback();
        }
    }

    public abstract Object getSession(UMOEndpoint endpoint) throws Exception;
}