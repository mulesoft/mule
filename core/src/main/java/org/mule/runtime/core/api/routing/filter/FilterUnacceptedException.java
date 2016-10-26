/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.routing.filter;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.execution.LocationExecutionContextProvider;
import org.mule.runtime.api.i18n.I18nMessage;


public class FilterUnacceptedException extends MuleException {

  private static final long serialVersionUID = -1828111078295716525L;

  private transient Filter filter;

  public FilterUnacceptedException(I18nMessage message, Filter filter) {
    super(message);
    this.filter = filter;
    addInfo("Filter", String.format("%s (%s)", filter.toString(), LocationExecutionContextProvider.getDocName(filter)));
  }

  public FilterUnacceptedException(I18nMessage message, Filter filter, Throwable cause) {
    super(message, cause);
    this.filter = filter;
    addInfo("Filter", String.format("%s (%s)", filter.toString(), LocationExecutionContextProvider.getDocName(filter)));
  }

  public FilterUnacceptedException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

  public FilterUnacceptedException(I18nMessage message) {
    super(message);
  }

  public Filter getFilter() {
    return filter;
  }
}
