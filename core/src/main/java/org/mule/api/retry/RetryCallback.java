/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.retry;

/**
 * This is the main Retry SPI.  The code inside the {@link #doWork} method is what will actually get <u>retried</u> 
 * according to the {@link RetryPolicy} that has been configured.  Note that retries can be wrapped in a transaction 
 * to ensure the work is atomic.
 */
public interface RetryCallback
{
    public void doWork(RetryContext context) throws Exception;

    public String getWorkDescription();
}
