/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.runtime.core.api.execution.ExecutionCallback;

import javax.jms.Message;

public abstract class MessageProcessingCallback<T> implements ExecutionCallback<T>
{
    protected final Message message;

    public MessageProcessingCallback(Message message)
    {
        this.message = message;
    }

}
