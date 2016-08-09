/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters.logic;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.routing.filter.ObjectFilter;

import java.util.List;

/**
 * <code>OrFilter</code> accepts if any of the filters accept the message
 */

public class OrFilter extends AbstractFilterCollection {

  public OrFilter() {
    super();
  }

  public OrFilter(Filter... filters) {
    super(filters);
  }

  public OrFilter(List<Filter> filters) {
    super(filters);
  }

  @Override
  public boolean accept(MuleMessage message) {
    for (Filter filter : getFilters()) {
      if (filter.accept(message)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean accept(Object object) {
    for (Filter filter : getFilters()) {
      if (((ObjectFilter) filter).accept(object)) {
        return true;
      }
    }
    return false;
  }
}
