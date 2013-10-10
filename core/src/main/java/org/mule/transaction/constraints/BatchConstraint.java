/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
