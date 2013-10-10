/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
        final StringBuffer sb = new StringBuffer();
        sb.append("RetryForeverPolicy");
        sb.append("{frequency=").append(frequency);
        sb.append('}');
        return sb.toString();
    }
}
