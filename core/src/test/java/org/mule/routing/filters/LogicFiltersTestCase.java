/*
 * $Id:LogicFiltersTestCase.java 5937 2007-04-09 22:35:04Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters;

import org.mule.impl.MuleMessage;
import org.mule.routing.filters.logic.AndFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.routing.filters.logic.OrFilter;
import org.mule.tck.AbstractMuleTestCase;

public class LogicFiltersTestCase extends AbstractMuleTestCase
{

    public void testAndFilter()
    {
        AndFilter filter = new AndFilter();
        assertEquals(0, filter.getFilters().size());

        // both null
        assertFalse(filter.accept(new MuleMessage("foo")));

        // only one filter set
        filter.getFilters().add(new EqualsFilter("foo"));
        assertTrue(filter.accept(new MuleMessage("foo")));

        // another one set too, but does not accept
        filter.getFilters().add(new EqualsFilter("foo"));
        assertFalse(filter.accept(new MuleMessage("bar")));

        // both accept
        assertTrue(filter.accept(new MuleMessage("foo")));

        WildcardFilter left = new WildcardFilter("blah.blah.*");
        WildcardFilter right = new WildcardFilter("blah.*");
        filter = new AndFilter(left, right);
        assertEquals(2,filter.getFilters().size());

        assertTrue(filter.accept(new MuleMessage("blah.blah.blah")));
        assertTrue(right.accept(new MuleMessage("blah.blah")));
        assertTrue(!left.accept(new MuleMessage("blah.blah")));
        assertTrue(!filter.accept(new MuleMessage("blah.blah")));

        filter = new AndFilter();
        filter.getFilters().add(left);
        filter.getFilters().add(right);

        assertTrue(filter.accept(new MuleMessage("blah.blah.blah")));
        assertTrue(!filter.accept(new MuleMessage("blah.blah")));
    }

    public void testOrFilter()
    {
        OrFilter filter = new OrFilter();
        assertNull(filter.getLeftFilter());
        assertNull(filter.getRightFilter());
        assertFalse(filter.accept(new MuleMessage("foo")));

        WildcardFilter left = new WildcardFilter("blah.blah.*");
        WildcardFilter right = new WildcardFilter("blah.b*");
        filter = new OrFilter(left, right);
        assertNotNull(filter.getLeftFilter());
        assertNotNull(filter.getRightFilter());

        assertTrue(filter.accept(new MuleMessage("blah.blah.blah")));
        assertTrue(right.accept(new MuleMessage("blah.blah")));
        assertTrue(!left.accept(new MuleMessage("blah.blah")));
        assertTrue(filter.accept(new MuleMessage("blah.blah")));
        assertTrue(!filter.accept(new MuleMessage("blah.x.blah")));

        filter = new OrFilter();
        filter.setLeftFilter(left);
        filter.setRightFilter(right);

        assertTrue(filter.accept(new MuleMessage("blah.blah.blah")));
        assertTrue(filter.accept(new MuleMessage("blah.blah")));
        assertTrue(!filter.accept(new MuleMessage("blah.x.blah")));
    }

    public void testNotFilter()
    {
        NotFilter notFilter = new NotFilter();
        assertNull(notFilter.getFilter());
        assertFalse(notFilter.accept(new MuleMessage("foo")));
        assertFalse(notFilter.accept(new MuleMessage(null)));

        WildcardFilter filter = new WildcardFilter("blah.blah.*");
        notFilter = new NotFilter(filter);
        assertNotNull(notFilter.getFilter());

        assertTrue(filter.accept(new MuleMessage("blah.blah.blah")));
        assertTrue(!notFilter.accept(new MuleMessage("blah.blah.blah")));

        notFilter = new NotFilter();
        notFilter.setFilter(filter);
        assertTrue(filter.accept(new MuleMessage("blah.blah.blah")));
        assertTrue(!notFilter.accept(new MuleMessage("blah.blah.blah")));
    }

}
