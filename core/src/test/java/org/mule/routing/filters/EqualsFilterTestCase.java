/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.filters;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SmallTest
public class EqualsFilterTestCase extends AbstractMuleTestCase
{

    @Test
    public void testEqualsFilterNoPattern()
    {
        EqualsFilter filter = new EqualsFilter();
        assertNull(filter.getPattern());
        assertFalse(filter.accept("foo"));

        filter.setPattern("foo");
        assertTrue(filter.accept("foo"));

        filter.setPattern(null);
        assertFalse(filter.accept("foo"));
    }

    @Test
    public void testEqualsFilter()
    {
        Exception obj = new Exception("test");
        EqualsFilter filter = new EqualsFilter(obj);
        assertNotNull(filter.getPattern());
        assertTrue(filter.accept(obj));
        assertTrue(!filter.accept(new Exception("tes")));

        filter.setPattern("Hello");
        assertTrue(filter.accept("Hello"));
        assertTrue(!filter.accept("Helo"));
    }

}
