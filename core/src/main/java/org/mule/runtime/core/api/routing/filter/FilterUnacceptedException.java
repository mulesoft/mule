/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.routing.filter;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.execution.LocationExecutionContextProvider;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.Message;


public class FilterUnacceptedException extends MessagingException {

  private static final long serialVersionUID = -1828111078295716525L;

  private transient Filter filter;

  public FilterUnacceptedException(Message message, MuleEvent event, Filter filter, MessageProcessor failingMessageProcessor) {
    super(message, event, failingMessageProcessor);
    this.filter = filter;
    addInfo("Filter", String.format("%s (%s)", filter.toString(), LocationExecutionContextProvider.getDocName(filter)));
  }

  public FilterUnacceptedException(Message message, MuleEvent event, Filter filter, Throwable cause) {
    super(message, event, cause);
    this.filter = filter;
    addInfo("Filter", String.format("%s (%s)", filter.toString(), LocationExecutionContextProvider.getDocName(filter)));
  }

  public FilterUnacceptedException(Message message, MuleEvent event, Throwable cause) {
    super(message, event, cause);
  }

  public FilterUnacceptedException(Message message, MuleEvent event, MessageProcessor failingMessageProcessor) {
    super(message, event, failingMessageProcessor);
  }

  public Filter getFilter() {
    return filter;
  }
}
