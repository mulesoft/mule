/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
