/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.api.MuleEvent;

/**
 * Callback to be used to notify of the result of an asynchronous response writing task.
 */
public interface ResponseCompletionCallback
{

    /**
     * Notifies that the response was written successfully
     */
    void responseSentSuccessfully();

    /**
     * Notifies that the response writing failed
     *
     * @param e the exception thrown during response writing
     * @param event the event that was the source for the response.
     */
    void responseSentWithFailure(Exception e, MuleEvent event);

}
