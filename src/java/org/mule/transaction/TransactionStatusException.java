/*
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 08-Feb-2004
 * Time: 14:53:05
 */
package org.mule.transaction;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.TransactionException;

public class TransactionStatusException extends TransactionException
{
    /**
     * @param message the exception message
     */
    public TransactionStatusException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public TransactionStatusException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public TransactionStatusException(Throwable cause)
    {
        super(new Message(Messages.TX_CANT_READ_STATE), cause);
    }
}
