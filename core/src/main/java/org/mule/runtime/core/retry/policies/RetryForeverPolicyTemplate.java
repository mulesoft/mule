/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.retry.policies;

import org.mule.api.retry.RetryPolicy;

/**
 * This policy is the same as {@link SimpleRetryPolicyTemplate} but will retry an infinite amount of times.
 */
public class RetryForeverPolicyTemplate extends SimpleRetryPolicyTemplate
{
    public RetryForeverPolicyTemplate()
    {
        super();
    }

    public RetryForeverPolicyTemplate(long frequency)
    {
        this.frequency = frequency;
    }

    @Override
    public RetryPolicy createRetryInstance()
    {
        return new SimpleRetryPolicy(frequency, RETRY_COUNT_FOREVER);
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("RetryForeverPolicy");
        sb.append("{frequency=").append(frequency);
        sb.append('}');
        return sb.toString();
    }
}
