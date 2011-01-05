/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

import org.mule.tck.AbstractMuleTestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PreferredComparatorTestCase extends AbstractMuleTestCase
{

    private PreferredComparator comparator;

    protected void doSetUp()
    {
        comparator = new PreferredComparator();
    }

    public void testCompareEqualInstances()
    {
        Preferred preferred1 = mock(Preferred.class);
        when(preferred1.weight()).thenReturn(1);

        Preferred preferred2 = mock(Preferred.class);
        when(preferred2.weight()).thenReturn(1);

        assertEquals(0, comparator.compare(preferred1, preferred2));
    }

    public void testCompareMinorThanInstance()
    {
        Preferred preferred1 = mock(Preferred.class);
        when(preferred1.weight()).thenReturn(1);

        Preferred preferred2 = mock(Preferred.class);
        when(preferred2.weight()).thenReturn(2);

        assertEquals(-1, comparator.compare(preferred1, preferred2));
    }

    public void testCompareGreaterThanInstance()
    {
        Preferred preferred1 = mock(Preferred.class);
        when(preferred1.weight()).thenReturn(2);

        Preferred preferred2 = mock(Preferred.class);
        when(preferred2.weight()).thenReturn(1);

        assertEquals(1, comparator.compare(preferred1, preferred2));
    }
}
