/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.interceptor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.InterceptingMessageProcessor;

/**
 * <code>Interceptor</code> is based on a similar concept of servlet filters and
 * works much the same way. This method is more commonally known as the interceptor
 * pattern and it allows for pre and processing of invocations on the object being
 * intercepted.
 */
public interface Interceptor extends InterceptingMessageProcessor
{

    /**
     * Invoked when the component should be called. The implementation can call
     * next.process(event) to call the component or the next filer in the chain.
     * 
     * @param event the event containing info about the current message and service
     * @return A result message that may have been altered by this invocation
     * @throws org.mule.api.MuleException if the invocation fails
     */
    MuleEvent process(MuleEvent event) throws MuleException;

}
