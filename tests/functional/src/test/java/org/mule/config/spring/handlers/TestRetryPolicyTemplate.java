/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.handlers;

import org.mule.api.retry.RetryPolicy;
import org.mule.retry.PolicyStatus;
import org.mule.retry.policies.AbstractPolicyTemplate;

import java.util.List;

public class TestRetryPolicyTemplate extends AbstractPolicyTemplate
{
    protected boolean fooBar = false;
    protected int revolutions = 200;
    protected List connectionUrls;

    public TestRetryPolicyTemplate()
    {
        super();
    }

    public TestRetryPolicyTemplate(boolean fooBar, int revolutions)
    {
        super();
        this.fooBar = fooBar;
        this.revolutions = revolutions;
    }

    public RetryPolicy createRetryInstance()
    {
        return new TestRetryPolicy(fooBar, revolutions);
    }

    protected static class TestRetryPolicy implements RetryPolicy
    {
        protected boolean fooBar;
        protected int revolutions;

        public TestRetryPolicy(boolean fooBar, int revolutions)
        {
            this.fooBar = fooBar;
            this.revolutions = revolutions;
        }
        
        public PolicyStatus applyPolicy(Throwable cause)
        {
            return PolicyStatus.policyExhausted(cause);
        }
    }

    public boolean isFooBar()
    {
        return fooBar;
    }

    public void setFooBar(boolean fooBar)
    {
        this.fooBar = fooBar;
    }

    public int getRevolutions()
    {
        return revolutions;
    }

    public void setRevolutions(int revolutions)
    {
        this.revolutions = revolutions;
    }

    public List getConnectionUrls()
    {
        return connectionUrls;
    }

    public void setConnectionUrls(List connectionUrls)
    {
        this.connectionUrls = connectionUrls;
    }
}
