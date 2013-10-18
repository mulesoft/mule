/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.store;

import org.mule.config.i18n.Message;

public class ObjectDoesNotExistException extends ObjectStoreException
{
    public ObjectDoesNotExistException()
    {
        super();
    }

    public ObjectDoesNotExistException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public ObjectDoesNotExistException(Message message)
    {
        super(message);
    }

    public ObjectDoesNotExistException(Throwable cause)
    {
        super(cause);
    }
}


