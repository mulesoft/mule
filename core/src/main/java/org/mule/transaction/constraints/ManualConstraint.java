/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transaction.constraints;

import org.mule.api.MuleEvent;

/**
 * <code>ManualConstraint</code> always returns false, meaning that the transaction
 * should be committed manually.
 */
// @ThreadSafe
public class ManualConstraint extends ConstraintFilter
{

    public boolean accept(MuleEvent event)
    {
        return false;
    }

}
