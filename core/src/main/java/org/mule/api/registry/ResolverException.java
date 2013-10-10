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
 * An exception that is thrown by resolver classes responsible for finding objects in the registry based on particular
 * criteria
 */
public class ResolverException extends MuleException
{
    public ResolverException(Message message)
    {
        super(message);
    }

    public ResolverException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
