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

import org.mule.transport.FatalConnectException;

/**
 * A factory responsible for creating a retry policy.  Custom policies should
 * Implement this factory and provide a private class which implements the 
 * {@link org.mule.api.retry.RetryPolicy} interface.  
 * The factory is the object that actually gets configured then new
 * {@link org.mule.api.retry.RetryPolicy} objects are created each time using the configuration
 * on the factory. 
 */
public interface RetryPolicyTemplate
{
    RetryPolicy createRetryInstance();

    RetryNotifier getNotifier();

    void setNotifier(RetryNotifier retryNotifier);

    RetryContext execute(RetryCallback callback) throws FatalConnectException;
}
