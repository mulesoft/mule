/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
