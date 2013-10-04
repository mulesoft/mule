/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
