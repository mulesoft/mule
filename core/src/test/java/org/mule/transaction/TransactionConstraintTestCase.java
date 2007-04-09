/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.transaction.constraints.ConstraintFilter;

public class TransactionConstraintTestCase extends AbstractMuleTestCase
{
    public void testConstraintFilter() throws Exception
    {
        ConstraintFilter filter = new ConstraintFilter();
        assertTrue(filter.accept(getTestEvent("test")));

        ConstraintFilter clone = (ConstraintFilter)filter.clone();
        assertNotNull(clone);
        assertNotSame(filter, clone);
    }
}
