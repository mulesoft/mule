/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mule.util.ExceptionUtils.containsType;
import static org.mule.util.ExceptionUtils.getDeepestOccurenceOfType;

@SmallTest
public class ExceptionUtilsTestCase extends AbstractMuleTestCase
{
    @Test
    public void testContainsType()
    {
        assertTrue(containsType(new IllegalArgumentException(), IllegalArgumentException.class));

        assertTrue(containsType(new Exception(new IllegalArgumentException()), IllegalArgumentException.class));

        assertTrue(containsType(new Exception(new IllegalArgumentException(new NullPointerException())), NullPointerException.class));

        assertTrue(containsType(new Exception(new IllegalArgumentException(new NullPointerException())), RuntimeException.class));

        assertTrue(containsType(new Exception(new IllegalArgumentException(new NullPointerException())), Exception.class));

        assertFalse(containsType(new Exception(new IllegalArgumentException(new NullPointerException())), IOException.class));
    }

    @Test
    public void testLastIndexOfType_deepestIsTheOneWeWant() throws Exception
    {
        IllegalArgumentException expected = new IllegalArgumentException("something");
        assertExpectationsForDeepestOccurence(expected);
    }

    @Test
    public void testLastIndexOfType_theOneWeWantIsNotTheDeepest() throws Exception
    {
        IllegalArgumentException expected = new IllegalArgumentException("something", new NullPointerException("somenull"));
        assertExpectationsForDeepestOccurence(expected);

    }

    private void assertExpectationsForDeepestOccurence(IllegalArgumentException expected)
    {
        assertSame(expected, getDeepestOccurenceOfType(expected, IllegalArgumentException.class));

        assertSame(expected, getDeepestOccurenceOfType(new Exception(expected), IllegalArgumentException.class));

        assertSame(expected,
            getDeepestOccurenceOfType(new IllegalArgumentException(new Exception(expected)), IllegalArgumentException.class));

        assertNull(getDeepestOccurenceOfType(new IllegalArgumentException(new Exception(expected)), IOException.class));
    }

    @Test
    public void testLastIndexOfType_nullParameters() throws Exception
    {
        assertNull(getDeepestOccurenceOfType(null, null));

        assertNull(getDeepestOccurenceOfType(new Exception(), null));

        assertNull(getDeepestOccurenceOfType(null, Exception.class));
    }
}
