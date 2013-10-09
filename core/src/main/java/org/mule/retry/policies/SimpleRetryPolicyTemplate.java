/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.retry.policies;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.retry.RetryPolicy;

/**
 * This policy allows the user to configure how many times a retry should be attempted and how long to wait between
 * retries.
 */
public class SimpleRetryPolicyTemplate extends AbstractPolicyTemplate
{

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(SimpleRetryPolicyTemplate.class);

    public static final int DEFAULT_FREQUENCY = 2000;
    public static final int DEFAULT_RETRY_COUNT = 2;
    public static final int RETRY_COUNT_FOREVER = -1;

    protected volatile int count = DEFAULT_RETRY_COUNT;
    protected volatile long frequency = DEFAULT_FREQUENCY;

    public SimpleRetryPolicyTemplate()
    {
        super();
    }

    public SimpleRetryPolicyTemplate(long frequency, int retryCount)
    {
        this.frequency = frequency;
        this.count = retryCount;
    }

    public long getFrequency()
    {
        return frequency;
    }

    public int getCount()
    {
        return count;
    }

    public void setFrequency(long frequency)
    {
        this.frequency = frequency;
    }

    public void setCount(int count)
    {
        this.count = count;
    }

    public RetryPolicy createRetryInstance()
    {
        return new SimpleRetryPolicy(frequency, count);
    }

    @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append("SimpleRetryPolicy");
        sb.append("{frequency=").append(frequency);
        sb.append(", retryCount=").append(count);
        sb.append('}');

        return sb.toString();
    }
}
