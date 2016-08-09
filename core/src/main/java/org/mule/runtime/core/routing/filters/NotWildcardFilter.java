/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters;

import org.mule.runtime.core.api.MuleMessage;

/**
 * Negative version of {@link org.mule.runtime.core.routing.filters.WildcardFilter}
 *
 * @since 4.0
 */
public class NotWildcardFilter extends WildcardFilter {

  @Override
  public boolean accept(Object object) {
    return !super.accept(object);
  }

  @Override
  public boolean accept(MuleMessage message) {
    return !super.accept(message);
  }
}
