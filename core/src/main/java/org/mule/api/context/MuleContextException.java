/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.context;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

/**
 * <code>MuleContextException</code> is thrown when an exception occurs with Mule Context
 * objects
 */
public class MuleContextException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1526680893293714180L;

    /**
     * @param message the exception message
     */
    public MuleContextException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public MuleContextException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
