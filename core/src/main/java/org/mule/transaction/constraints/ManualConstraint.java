/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction.constraints;

import org.mule.umo.UMOEvent;

/**
 * <code>ManualConstraint</code> always returns false, meaning that the transaction
 * should be committed manually.
 */
// @ThreadSafe
public class ManualConstraint extends ConstraintFilter
{

    public boolean accept(UMOEvent event)
    {
        return false;
    }

}
