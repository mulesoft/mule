/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.api.execution.CompletionHandler;

/**
 * Ignores the result of asynchronous processing
 *
 * @since 4.0
 */
public class NullCompletionHandler implements CompletionHandler
{

    @Override
    public void onCompletion(Object result)
    {
        //Nothing to do
    }

    @Override
    public void onFailure(Throwable exception)
    {
        //Nothing to do
    }
}
