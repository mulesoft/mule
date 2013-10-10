/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.registry;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

/**
 * Any service-related exception: service not found, service lookup error, etc.
 */
public class ServiceException extends MuleException
{
    public ServiceException(Message message)
    {
        super(message);
    }

    public ServiceException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
