/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.polling;

import org.mule.api.MuleException;

/**
 * Defines a task to be executed inside a poll.
 *
 * @since 4.0
 */
public interface PollingTask
{

    /**
     * @return true if it's runnable, false otherwise
     */
    boolean isStarted();

    /**
     * Task execution logic
     *
     * @throws Exception
     */
    void run() throws Exception;

    /**
     * Stops the current task. <p/> This may be called by the task runner in case of a thread interruption.
     *
     * @throws MuleException
     */
    void stop() throws MuleException;
}
