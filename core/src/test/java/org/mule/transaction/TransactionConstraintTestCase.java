/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transaction;

import org.mule.api.MuleEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transaction.constraints.ConstraintFilter;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class TransactionConstraintTestCase extends AbstractMuleTestCase
{
    @Test
    public void testConstraintFilter() throws Exception
    {
        ConstraintFilter filter = new ConstraintFilter();
        MuleEvent event = Mockito.mock(MuleEvent.class);
        assertTrue(filter.accept(event));

        ConstraintFilter clone = (ConstraintFilter)filter.clone();
        assertNotNull(clone);
        assertNotSame(filter, clone);
    }
}
