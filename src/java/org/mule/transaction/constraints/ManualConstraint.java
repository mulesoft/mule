/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.transaction.constraints;

import org.mule.umo.UMOEvent;

/**
 * <code>ManualConstraint</code> always returns false meaning that the transaction
 * should be commited manually.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class ManualConstraint extends ConstraintFilter
{
    protected boolean accept(UMOEvent event)
    {
        return false;
    }
}
