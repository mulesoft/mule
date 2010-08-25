/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp;

import org.mule.transport.tcp.TcpMessageReceiver.TcpWorker;

/**
 * An exception policy that is called when an exception is thrown while processing the next message 
 *
 * @since 2.2.6
 */
public interface NextMessageExceptionPolicy
{

    /**
     * Handles the exception thrown while processing the next message
     * 
     * @param exception the exception thrown
     * @param receiver the tcp message receiver
     * @param expirable the tcp worker that is processing the message
     * @throws Exception while processing the exception
     * @return the result of processing the exception
     */
    Object handleException(Exception exception, TcpMessageReceiver receiver, TcpWorker worker) throws Exception;

}
