/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.store;

import org.mule.config.i18n.Message;

/**
 * This exception is thrown when the underlying to an {@link ObjectStore}'s system fails.
 */
public class ObjectStoreNotAvaliableException extends ObjectStoreException
{
    public ObjectStoreNotAvaliableException()
    {
        super();
    }

    public ObjectStoreNotAvaliableException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public ObjectStoreNotAvaliableException(Message message)
    {
        super(message);
    }

    public ObjectStoreNotAvaliableException(Throwable cause)
    {
        super(cause);
    }
}


