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
 * <code>BusinessException</code> TODO (document class)
 */
public class BusinessException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3650171245608111071L;

    /**
     * @param message
     */
    public BusinessException(String message)
    {
        this("BUSINESS EXCEPTION: " + message, null);
    }

    /**
     * @param message
     * @param cause
     */
    public BusinessException(String message, Throwable cause)
    {
        super(MessageFactory.createStaticMessage(message), cause);
    }

}
