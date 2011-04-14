/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;

import java.util.regex.Pattern;

public class RegExFilterTestCase extends AbstractMuleTestCase
{
    private static final String PATTERN = "(.*) brown fox";

    public void testRegexFilterNoPattern()
    {
        // start with default
        RegExFilter filter = new RegExFilter();
        assertNull(filter.getPattern());
        assertFalse(filter.accept("No tengo dinero"));

        // activate a pattern
        filter.setPattern("(.*) brown fox");
        assertTrue(filter.accept("The quick brown fox"));

        // remove pattern again, i.e. block all
        filter.setPattern(null);
        assertFalse(filter.accept("oh-oh"));
    }

    public void testRegexFilter()
    {
        RegExFilter filter = new RegExFilter("The quick (.*)");
        assertNotNull(filter.getPattern());

        assertTrue(filter.accept("The quick brown fox"));
        assertTrue(filter.accept("The quick "));

        assertFalse(filter.accept("The quickbrown fox"));
        assertFalse(filter.accept("he quick brown fox"));

        filter.setPattern("(.*) brown fox");
        assertTrue(filter.accept("The quick brown fox"));
        assertTrue(filter.accept(" brown fox"));

        assertFalse(filter.accept("The quickbrown fox"));
        assertFalse(filter.accept("The quick brown fo"));

        filter.setPattern("(.*) brown (.*)");
        assertTrue(filter.accept("The quick brown fox"));
        assertTrue(filter.accept("(.*) brown fox"));

        assertFalse(filter.accept("The quickbrown fox"));
        assertTrue(filter.accept("The quick brown fo"));

        filter.setPattern("(.*)");
        assertTrue(filter.accept("The quick brown fox"));
    }

    public void testNullInput()
    {
        RegExFilter filter = new RegExFilter("The quick (.*)");
        assertNotNull(filter.getPattern());
        assertFalse(filter.accept((Object) null));
    }

    public void testMuleMessageInput()
    {
        RegExFilter filter = new RegExFilter("The quick (.*)");
        assertNotNull(filter.getPattern());

        MuleMessage message = new DefaultMuleMessage("The quick brown fox", muleContext);
        assertTrue(filter.accept(message));
    }

    public void testByteArrayInput()
    {
        RegExFilter filter = new RegExFilter("The quick (.*)");
        assertNotNull(filter.getPattern());

        byte[] bytes = "The quick brown fox".getBytes();
        assertTrue(filter.accept(bytes));
    }

    public void testCharArrayInput()
    {
        RegExFilter filter = new RegExFilter("The quick (.*)");
        assertNotNull(filter.getPattern());

        char[] chars = "The quick brown fox".toCharArray();
        assertTrue(filter.accept(chars));
    }

    public void testEqualsWithSamePattern()
    {
        RegExFilter filter1 = new RegExFilter(PATTERN);
        RegExFilter filter2 = new RegExFilter(PATTERN);
        assertEquals(filter1, filter2);
    }

    public void testEqualsWithDifferentPattern()
    {
        RegExFilter filter1 = new RegExFilter("foo");
        RegExFilter filter2 = new RegExFilter("bar");
        assertFalse(filter1.equals(filter2));
    }

    public void testEqualsWithEqualPatternAndDifferentFlags()
    {
        RegExFilter filter1 = new RegExFilter(PATTERN, Pattern.DOTALL);
        RegExFilter filter2 = new RegExFilter(PATTERN, Pattern.CASE_INSENSITIVE);
        assertFalse(filter1.equals(filter2));

        filter1 = new RegExFilter(PATTERN, Pattern.DOTALL);
        filter2 = new RegExFilter(PATTERN, Pattern.DOTALL);
        assertEquals(filter1, filter2);
    }
}
