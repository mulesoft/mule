/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import org.mule.api.transaction.TransactionCallback;

import javax.jms.Message;

public abstract class MessageTransactionCallback<T> implements TransactionCallback<T>
{
    protected final Message message;

    public MessageTransactionCallback(Message message)
    {
        this.message = message;
    }

}
