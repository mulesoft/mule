/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import org.mule.api.MessagingException;

/**
 * A handler for handling the result of asynchronous processing.
 *
 * @since 3.7
 */
public interface CompletionHandler<T, E>
{

    /**
     * Invoked on sucessful completion of asynchronous processing
     * @param t the result of processing
     */
    public void onCompletion(T t);

    /**
     * Invoked when a failure occurs during asynchronous processing
     * @param t the exception thrown during processing
     */
    public void onFailure(E exception);

}
