/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.retry.policies;

import org.mule.api.retry.RetryPolicy;
import org.mule.retry.PolicyStatus;

/**
 * This policy is basically a placeholder.  It does not attempt to retry at all.
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
