/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.store;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

/**
 * This exception class is thrown in cases when an exception occurs while operating on an 
 * {@link ObjectStore}.
 */
public class ObjectStoreException extends MuleException
{
    public ObjectStoreException()
    {
        super();
    }

    public ObjectStoreException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public ObjectStoreException(Message message)
    {
        super(message);
    }

    public ObjectStoreException(Throwable cause)
    {
        super(cause);
    }
}
