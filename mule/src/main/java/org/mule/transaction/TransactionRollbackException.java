/*
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 05-Feb-2004
 * Time: 18:39:43
 */
package org.mule.transaction;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.TransactionException;

public class TransactionRollbackException extends TransactionException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3247455113055556221L;

    /**
     * @param message the exception message
     */
    public TransactionRollbackException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public TransactionRollbackException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public TransactionRollbackException(Throwable cause)
    {
        super(new Message(Messages.TX_ROLLBACK_FAILED), cause);
    }

}
