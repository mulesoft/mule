/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.errorhandler.exceptions;

import org.mule.api.MuleException;
import org.mule.config.i18n.MessageFactory;

/**
 * <code>FatalException</code> TODO (document class)
 */
public class FatalException extends MuleException
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = -5683879269751770579L;

    /**
     * @param message
     */
    public FatalException(String message)
    {
        this("FATAL EXCEPTION: " + message, null);
    }

    /**
     * @param message
     * @param cause
     */
    public FatalException(String message, Throwable cause)
    {
        super(MessageFactory.createStaticMessage(message), cause);
    }

}
