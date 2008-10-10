/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.retry.policies;

import org.mule.api.retry.RetryPolicy;
import org.mule.retry.PolicyStatus;

/**
 * This policy does what it says on the tin.  It will allow a {@link RetryTemplate} to execute
 * once and then stop.
 */
public class NoRetryPolicyTemplate extends AbstractPolicyTemplate
{
    public RetryPolicy createRetryInstance()
    {
        return new NoRetryPolicy();
    }

    protected static class NoRetryPolicy implements RetryPolicy
    {
        public PolicyStatus applyPolicy(Throwable cause)
        {
            return PolicyStatus.policyExhausted(cause);
        }
    }

    public String toString()
    {
        return "NoRetryPolicy{}";
    }
}
