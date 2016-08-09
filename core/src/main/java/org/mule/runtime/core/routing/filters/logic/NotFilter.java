/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters.logic;

import static org.mule.runtime.core.util.ClassUtils.equal;
import static org.mule.runtime.core.util.ClassUtils.hash;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.routing.filter.ObjectFilter;

/**
 * <code>NotFilter</code> accepts if the filter does not accept.
 */

public class NotFilter implements Filter, ObjectFilter {

  private Filter filter;

  public NotFilter() {
    super();
  }

  public NotFilter(Filter filter) {
    this.filter = filter;
  }

  public Filter getFilter() {
    return filter;
  }

  public void setFilter(Filter filter) {
    this.filter = filter;
  }

  @Override
  public boolean accept(MuleMessage message) {
    return (filter != null ? !filter.accept(message) : false);
  }

  @Override
  public boolean accept(Object object) {
    return (filter != null ? !((ObjectFilter) filter).accept(object) : false);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;

    final NotFilter other = (NotFilter) obj;
    return equal(filter, other.filter);
  }

  @Override
  public int hashCode() {
    return hash(new Object[] {this.getClass(), filter});
  }
}
