/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class PreferredComparatorTestCase extends AbstractMuleTestCase
{

    private PreferredComparator comparator;

    @Before
    public void setUpComparator()
    {
        comparator = new PreferredComparator();
    }

    @Test
    public void testCompareEqualInstances()
    {
        Preferred preferred1 = mock(Preferred.class);
        when(preferred1.weight()).thenReturn(1);

        Preferred preferred2 = mock(Preferred.class);
        when(preferred2.weight()).thenReturn(1);

        assertEquals(0, comparator.compare(preferred1, preferred2));
    }

    @Test
    public void testCompareMinorThanInstance()
    {
        Preferred preferred1 = mock(Preferred.class);
        when(preferred1.weight()).thenReturn(1);

        Preferred preferred2 = mock(Preferred.class);
        when(preferred2.weight()).thenReturn(2);

        assertEquals(-1, comparator.compare(preferred1, preferred2));
    }

    @Test
    public void testCompareGreaterThanInstance()
    {
        Preferred preferred1 = mock(Preferred.class);
        when(preferred1.weight()).thenReturn(2);

        Preferred preferred2 = mock(Preferred.class);
        when(preferred2.weight()).thenReturn(1);

        assertEquals(1, comparator.compare(preferred1, preferred2));
    }
}
