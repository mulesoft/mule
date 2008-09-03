/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.retry;

/**
 * This is the main Retry SPI. Any code executed in the {@link #doWork} method will be subject to any Retry Policies
 * associated with the {@link org.mule.api.retry.RetryTemplate}. If {@link #doWork} throws an exception the operation
 * will be attempted again until the Retry Policy has been exhausted. Note that Retries can be wrapped in a transaction 
 * to ensure the work is atomic.
 *
 * @see org.mule.api.retry.RetryTemplate
 * @see org.mule.api.retry.RetryPolicy
 *
 */

public interface RetryCallback
{
    public void doWork(RetryContext context) throws Exception;

    public String getWorkDescription();
}
