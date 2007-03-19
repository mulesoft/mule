/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.filters;

import org.mule.routing.filters.RegExFilter;
import org.mule.tck.AbstractMuleTestCase;

public class RegExFilterTestCase extends AbstractMuleTestCase
{

    public void testRegexFilterNoPattern()
    {
        // start with default
        RegExFilter filter = new RegExFilter();
        assertNull(filter.getExpression());
        assertFalse(filter.accept("No tengo dinero"));

        // activate a pattern
        filter.setExpression("(.*) brown fox");
        assertTrue(filter.accept("The quick brown fox"));

        // remove pattern again, i.e. block all
        filter.setExpression(null);
        assertFalse(filter.accept("oh-oh"));
    }

    public void testRegexFilter()
    {

        RegExFilter filter = new RegExFilter("The quick (.*)");
        assertNotNull(filter.getExpression());

        assertTrue(filter.accept("The quick brown fox"));
        assertTrue(filter.accept("The quick "));

        assertTrue(!filter.accept("The quickbrown fox"));
        assertTrue(!filter.accept("he quick brown fox"));

        filter.setExpression("(.*) brown fox");
        assertTrue(filter.accept("The quick brown fox"));
        assertTrue(filter.accept(" brown fox"));

        assertTrue(!filter.accept("The quickbrown fox"));
        assertTrue(!filter.accept("The quick brown fo"));

        filter.setExpression("(.*) brown (.*)");
        assertTrue(filter.accept("The quick brown fox"));
        assertTrue(filter.accept("(.*) brown fox"));

        assertTrue(!filter.accept("The quickbrown fox"));
        assertTrue(filter.accept("The quick brown fo"));

        filter.setExpression("(.*)");
        assertTrue(filter.accept("The quick brown fox"));
    }
}
