/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
