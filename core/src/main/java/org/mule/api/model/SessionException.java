/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.model;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

/**
 * <code>SessionException</code> is thrown when errors occur in the DefaultMuleSession or
 * Seession Manager
 */
public class SessionException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6751481096543965553L;

    /**
     * @param message the exception message
     */
    public SessionException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public SessionException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
