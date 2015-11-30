/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;
import static org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mule.util.ExceptionUtils.containsType;
import static org.mule.util.ExceptionUtils.getDeepestOccurenceOfType;
import static org.mule.util.ExceptionUtils.getFullStackTraceWithoutMessages;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;

import org.junit.Test;

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

    @Test
    public void testFullStackTraceWithoutMessage() throws Exception
    {
        final String mainMessage = "main message 112312 [][] ''' ... sdfsd blah";
        final String causeMessage = "cause message 2342998n  fwefoskjdcas  sdcasdhfsadjgsadkgasd \t\nsdfsllki";

        Exception e = new RuntimeException(mainMessage, new RuntimeException(causeMessage));
        String withoutMessage = getFullStackTraceWithoutMessages(e);
        String fullStackTrace = getFullStackTrace(e);

        String[] linesWithoutMessage = withoutMessage.split(LINE_SEPARATOR);
        String[] lines = fullStackTrace.split(LINE_SEPARATOR);

        assertEquals(lines.length, linesWithoutMessage.length);

        for (int i = 0; i < lines.length; i++)
        {
            assertTrue(lines[i].contains(linesWithoutMessage[i]));
            assertFalse(linesWithoutMessage[i].contains(mainMessage));
            assertFalse(linesWithoutMessage[i].contains(causeMessage));
        }
    }
}
