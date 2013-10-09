/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transaction;

import org.mule.api.MuleEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transaction.constraints.BatchConstraint;
import org.mule.transaction.constraints.ConstraintFilter;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class BatchConstraintTestCase extends AbstractMuleTestCase
{
    @Test
    public void testConstraintFilter() throws Exception
    {
        MuleEvent testEvent = Mockito.mock(MuleEvent.class);
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
