/*
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 05-Feb-2004
 * Time: 18:39:43
 */
package org.mule.transaction;

import org.mule.umo.UMOTransactionException;

public class TransactionRollbackException extends UMOTransactionException
{
    public TransactionRollbackException(String message)
    {
        super(message);
    }

    public TransactionRollbackException(String message, Throwable cause)
    {
        super(message, cause);
    }
}