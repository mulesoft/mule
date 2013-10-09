/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
