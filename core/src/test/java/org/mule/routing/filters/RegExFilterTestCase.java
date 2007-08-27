/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters;

import org.mule.tck.AbstractMuleTestCase;

public class RegExFilterTestCase extends AbstractMuleTestCase
{

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

        assertTrue(!filter.accept("The quickbrown fox"));
        assertTrue(!filter.accept("he quick brown fox"));

        filter.setPattern("(.*) brown fox");
        assertTrue(filter.accept("The quick brown fox"));
        assertTrue(filter.accept(" brown fox"));

        assertTrue(!filter.accept("The quickbrown fox"));
        assertTrue(!filter.accept("The quick brown fo"));

        filter.setPattern("(.*) brown (.*)");
        assertTrue(filter.accept("The quick brown fox"));
        assertTrue(filter.accept("(.*) brown fox"));

        assertTrue(!filter.accept("The quickbrown fox"));
        assertTrue(filter.accept("The quick brown fo"));

        filter.setPattern("(.*)");
        assertTrue(filter.accept("The quick brown fox"));
    }
}
