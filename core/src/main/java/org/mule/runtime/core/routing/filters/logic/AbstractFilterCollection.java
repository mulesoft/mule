/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters.logic;

import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.routing.filter.ObjectFilter;
import org.mule.runtime.core.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a filter collection. Used as the base clas for the Or and AND filters
 */

public abstract class AbstractFilterCollection implements Filter, ObjectFilter {

  private List<Filter> filters;

  public AbstractFilterCollection() {
    filters = new ArrayList<>();
  }

  public AbstractFilterCollection(List<Filter> filters) {
    this();
    this.filters = filters;
  }

  public AbstractFilterCollection(Filter... filters) {
    this();
    for (Filter filter : filters) {
      this.filters.add(filter);
    }
  }

  public List<Filter> getFilters() {
    return filters;
  }

  public void setFilters(List<Filter> filters) {
    this.filters = filters;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;

    final AbstractFilterCollection other = (AbstractFilterCollection) obj;
    return ClassUtils.equal(filters, other.filters);
  }

  @Override
  public int hashCode() {
    return ClassUtils.hash(new Object[] {this.getClass(), filters});
  }

}
