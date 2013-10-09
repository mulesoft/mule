/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transaction.constraints;

import org.mule.api.MuleEvent;

// @ThreadSafe
public class ConstraintFilter implements Cloneable
{
    public ConstraintFilter()
    {
        super();
    }

    public boolean accept(MuleEvent event)
    {
        return true;
    }

    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

}
