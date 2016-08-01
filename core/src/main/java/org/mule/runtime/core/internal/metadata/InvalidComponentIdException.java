/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.api.metadata.ComponentId;
import org.mule.runtime.core.config.i18n.Message;

/**
 * Represents that the given {@link ComponentId} is invalid due that the Component
 * could not be found in the current {@link MuleContext}.
 *
 * @since 4.0
 */
public class InvalidComponentIdException extends MuleException
{

    /**
     * @param message the exception message
     */
    InvalidComponentIdException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    InvalidComponentIdException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
