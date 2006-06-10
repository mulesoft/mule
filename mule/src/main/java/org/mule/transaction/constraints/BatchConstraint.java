/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.transaction.constraints;

import org.mule.umo.UMOEvent;

/**
 * <code>BatchConstraint</code> is a filter that counts on every execution and
 * returns true when the batch size value equals the execution count.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class BatchConstraint extends ConstraintFilter
{
    private int batchSize = 1;

    private int batchCount = 0;

    public boolean accept(UMOEvent event)
    {
        batchCount++;
        return batchCount == batchSize;
    }

    public int getBatchSize()
    {
        return batchSize;
    }

    public void setBatchSize(int batchSize)
    {
        this.batchSize = batchSize;
    }

    public Object clone() throws CloneNotSupportedException
    {
        BatchConstraint clone = new BatchConstraint();
        clone.setBatchSize(batchSize);
        for (int i = 0; i < batchCount; i++) {
            clone.accept(null);
        }
        return clone;
    }

}
