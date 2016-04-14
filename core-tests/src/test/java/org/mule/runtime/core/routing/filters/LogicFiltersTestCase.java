/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters;

import org.mule.DefaultMuleMessage;
import org.mule.routing.filters.logic.AndFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.routing.filters.logic.OrFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.LinkedList;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LogicFiltersTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testAndFilter()
    {
        AndFilter filter = new AndFilter();
        assertEquals(0, filter.getFilters().size());

        // both null
        assertFalse(filter.accept(new DefaultMuleMessage("foo", muleContext)));

        // only one filter set
        filter.getFilters().add(new EqualsFilter("foo"));
        assertTrue(filter.accept(new DefaultMuleMessage("foo", muleContext)));

        // another one set too, but does not accept
        filter.getFilters().add(new EqualsFilter("foo"));
        assertFalse(filter.accept(new DefaultMuleMessage("bar", muleContext)));

        // both accept
        assertTrue(filter.accept(new DefaultMuleMessage("foo", muleContext)));

        WildcardFilter left = new WildcardFilter("blah.blah.*");
        WildcardFilter right = new WildcardFilter("blah.*");
        filter = new AndFilter(left, right);
        assertEquals(2,filter.getFilters().size());

        assertTrue(filter.accept(new DefaultMuleMessage("blah.blah.blah", muleContext)));
        assertTrue(right.accept(new DefaultMuleMessage("blah.blah", muleContext)));
        assertTrue(!left.accept(new DefaultMuleMessage("blah.blah", muleContext)));
        assertTrue(!filter.accept(new DefaultMuleMessage("blah.blah", muleContext)));

        filter = new AndFilter();
        filter.getFilters().add(left);
        filter.getFilters().add(right);

        assertTrue(filter.accept(new DefaultMuleMessage("blah.blah.blah", muleContext)));
        assertTrue(!filter.accept(new DefaultMuleMessage("blah.blah", muleContext)));
    }

    @Test
    public void testOrFilter()
    {
        OrFilter filter = new OrFilter();
        assertEquals(0, filter.getFilters().size());
        assertFalse(filter.accept(new DefaultMuleMessage("foo", muleContext)));

        WildcardFilter left = new WildcardFilter("blah.blah.*");
        WildcardFilter right = new WildcardFilter("blah.b*");
        filter = new OrFilter(left, right);
        assertEquals(2, filter.getFilters().size());

        assertTrue(filter.accept(new DefaultMuleMessage("blah.blah.blah", muleContext)));
        assertTrue(right.accept(new DefaultMuleMessage("blah.blah", muleContext)));
        assertTrue(!left.accept(new DefaultMuleMessage("blah.blah", muleContext)));
        assertTrue(filter.accept(new DefaultMuleMessage("blah.blah", muleContext)));
        assertTrue(!filter.accept(new DefaultMuleMessage("blah.x.blah", muleContext)));

        filter = new OrFilter();
        LinkedList filters = new LinkedList();
        filters.addLast(left);
        filters.addLast(right);
        filter.setFilters(filters);

        assertTrue(filter.accept(new DefaultMuleMessage("blah.blah.blah", muleContext)));
        assertTrue(filter.accept(new DefaultMuleMessage("blah.blah", muleContext)));
        assertTrue(!filter.accept(new DefaultMuleMessage("blah.x.blah", muleContext)));
    }

    @Test
    public void testNotFilter()
    {
        NotFilter notFilter = new NotFilter();
        assertNull(notFilter.getFilter());
        assertFalse(notFilter.accept(new DefaultMuleMessage("foo", muleContext)));
        assertFalse(notFilter.accept(new DefaultMuleMessage(null, muleContext)));

        WildcardFilter filter = new WildcardFilter("blah.blah.*");
        notFilter = new NotFilter(filter);
        assertNotNull(notFilter.getFilter());

        assertTrue(filter.accept(new DefaultMuleMessage("blah.blah.blah", muleContext)));
        assertTrue(!notFilter.accept(new DefaultMuleMessage("blah.blah.blah", muleContext)));

        notFilter = new NotFilter();
        notFilter.setFilter(filter);
        assertTrue(filter.accept(new DefaultMuleMessage("blah.blah.blah", muleContext)));
        assertTrue(!notFilter.accept(new DefaultMuleMessage("blah.blah.blah", muleContext)));
    }

}
