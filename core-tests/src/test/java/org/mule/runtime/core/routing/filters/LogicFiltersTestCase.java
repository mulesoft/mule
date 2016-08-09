/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.routing.filters.logic.AndFilter;
import org.mule.runtime.core.routing.filters.logic.NotFilter;
import org.mule.runtime.core.routing.filters.logic.OrFilter;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.LinkedList;

import org.junit.Test;

public class LogicFiltersTestCase extends AbstractMuleTestCase {

  @Test
  public void testAndFilter() {
    AndFilter filter = new AndFilter();
    assertEquals(0, filter.getFilters().size());

    // both null
    assertFalse(filter.accept("foo"));

    // only one filter set
    filter.getFilters().add(new EqualsFilter("foo"));
    assertTrue(filter.accept("foo"));

    // another one set too, but does not accept
    filter.getFilters().add(new EqualsFilter("foo"));
    assertFalse(filter.accept("bar"));

    // both accept
    assertTrue(filter.accept("foo"));

    WildcardFilter left = new WildcardFilter("blah.blah.*");
    WildcardFilter right = new WildcardFilter("blah.*");
    filter = new AndFilter(left, right);
    assertEquals(2, filter.getFilters().size());

    assertTrue(filter.accept("blah.blah.blah"));
    assertTrue(right.accept("blah.blah"));
    assertTrue(!left.accept("blah.blah"));
    assertTrue(!filter.accept("blah.blah"));

    filter = new AndFilter();
    filter.getFilters().add(left);
    filter.getFilters().add(right);

    assertTrue(filter.accept("blah.blah.blah"));
    assertTrue(!filter.accept("blah.blah"));
  }

  @Test
  public void testOrFilter() {
    OrFilter filter = new OrFilter();
    assertEquals(0, filter.getFilters().size());
    assertFalse(filter.accept("foo"));

    WildcardFilter left = new WildcardFilter("blah.blah.*");
    WildcardFilter right = new WildcardFilter("blah.b*");
    filter = new OrFilter(left, right);
    assertEquals(2, filter.getFilters().size());

    assertTrue(filter.accept("blah.blah.blah"));
    assertTrue(right.accept("blah.blah"));
    assertTrue(!left.accept("blah.blah"));
    assertTrue(filter.accept("blah.blah"));
    assertTrue(!filter.accept("blah.x.blah"));

    filter = new OrFilter();
    LinkedList filters = new LinkedList();
    filters.addLast(left);
    filters.addLast(right);
    filter.setFilters(filters);

    assertTrue(filter.accept("blah.blah.blah"));
    assertTrue(filter.accept("blah.blah"));
    assertTrue(!filter.accept("blah.x.blah"));
  }

  @Test
  public void testNotFilter() {
    NotFilter notFilter = new NotFilter();
    assertNull(notFilter.getFilter());
    assertFalse(notFilter.accept("foo"));
    assertFalse(notFilter.accept((Object) null));

    WildcardFilter filter = new WildcardFilter("blah.blah.*");
    notFilter = new NotFilter(filter);
    assertNotNull(notFilter.getFilter());

    assertTrue(filter.accept("blah.blah.blah"));
    assertTrue(!notFilter.accept("blah.blah.blah"));

    notFilter = new NotFilter();
    notFilter.setFilter(filter);
    assertTrue(filter.accept("blah.blah.blah"));
    assertTrue(!notFilter.accept("blah.blah.blah"));
  }

}
