/*
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 08-Feb-2004
 * Time: 14:53:05
 */
package org.mule.transaction;

import org.mule.umo.UMOTransactionException;

public class TransactionStatusException extends UMOTransactionException
{
    public TransactionStatusException(String message)
    {
        super(message);
    }

    public TransactionStatusException(String message, Throwable cause)
    {
        super(message, cause);
    }
}