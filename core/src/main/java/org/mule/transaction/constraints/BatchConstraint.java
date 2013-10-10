/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transaction.constraints;

import org.mule.api.MuleEvent;

/**
 * <code>BatchConstraint</code> is a filter that counts on every execution and
 * returns true when the batch size value equals the execution count.
 */
// @ThreadSafe
public class BatchConstraint extends ConstraintFilter
{
    // @GuardedBy(this)
    private int batchSize = 1;
    // @GuardedBy(this)
    private int batchCount = 0;

    public boolean accept(MuleEvent event)
    {
        synchronized (this)
        {
            batchCount++;
            return batchCount == batchSize;
        }
    }

    public int getBatchSize()
    {
        synchronized (this)
        {
            return batchSize;
        }
    }

    public synchronized void setBatchSize(int batchSize)
    {
        synchronized (this)
        {
            this.batchSize = batchSize;
        }
    }

    public Object clone() throws CloneNotSupportedException
    {
        synchronized (this)
        {
            BatchConstraint clone = (BatchConstraint) super.clone();
            clone.setBatchSize(batchSize);
            for (int i = 0; i < batchCount; i++)
            {
                clone.accept(null);
            }
            return clone;
        }
    }

}
