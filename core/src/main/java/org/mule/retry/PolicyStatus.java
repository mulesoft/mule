/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.retry;


/**
 * Indicates the current state of a RetryPolicy
 * <ul>
 * <li>ok: The policy is active</li>
 * <li>exhausted: The policy has run through the actions for the policy</li>
 * </ul>
 *
 * For example, a RetryPolicy may have a RetryCount - how many times the policy can be invoked.
 * Once the retryCount has been reached, the policy is exhausted and cannot be used again.
 */
public class PolicyStatus
{
    private boolean exhausted = false;
    private boolean ok = false;
    private Throwable throwable;

    public static PolicyStatus policyExhausted(Throwable t)
    {
        return new PolicyStatus(true, t);
    }

    public static PolicyStatus policyOk()
    {
        return new PolicyStatus();
    }

    protected PolicyStatus()
    {
        this.ok = true;
    }

    protected PolicyStatus(boolean exhausted, Throwable throwable)
    {
        this.exhausted = exhausted;
        this.throwable = throwable;
    }

    public boolean isExhausted()
    {
        return exhausted;
    }

    public boolean isOk()
    {
        return ok;
    }

    public Throwable getThrowable()
    {
        return throwable;
    }
}
