/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction;

import org.mule.api.MuleEvent;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transaction.constraints.BatchConstraint;
import org.mule.transaction.constraints.ConstraintFilter;

import com.mockobjects.dynamic.Mock;

public class BatchConstraintTestCase extends AbstractMuleTestCase
{

    public void testConstraintFilter() throws Exception
    {
        MuleEvent testEvent = (MuleEvent)new Mock(MuleEvent.class).proxy();
        BatchConstraint filter = new BatchConstraint();
        filter.setBatchSize(3);
        assertEquals(3, filter.getBatchSize());
        assertTrue(!filter.accept(testEvent));

        ConstraintFilter clone = (ConstraintFilter)filter.clone();
        assertNotNull(clone);
        assertNotSame(filter, clone);

        assertTrue(!filter.accept(testEvent));
        assertTrue(filter.accept(testEvent));
    }

}
