/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.filters;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.message.ExceptionPayload;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.api.util.ClassUtils;

import java.util.Optional;

/**
 * A filter that accepts messages that have an exception payload. An Exception type can also be set on this filter to allow it to
 * accept Exception messages of a particular Exception class only.
 */
public class ExceptionTypeFilter extends PayloadTypeFilter {

  public ExceptionTypeFilter() {
    super();
  }


  public ExceptionTypeFilter(String expectedType) throws ClassNotFoundException {
    this(ClassUtils.loadClass(expectedType, ExceptionTypeFilter.class));
  }

  public ExceptionTypeFilter(Class expectedType) {
    super(expectedType);
  }

  /**
   * Check a given event against this filter.
   *
   * @param event a non null event to filter.
   * @return <code>true</code> if the event matches the filter
   */
  @Override
  public boolean accept(Event event, Event.Builder builder) {
    Optional<Error> errorOptional = event.getError();
    if (getExpectedType() == null) {
      return errorOptional.isPresent();
    } else if (errorOptional.isPresent()) {
      return getExpectedType().isAssignableFrom(errorOptional.get().getCause().getClass());
    } else {
      return accept(event.getMessage(), builder);
    }
  }

  /**
   * Check a given message against this filter.
   *
   * @param message a non null message to filter.
   * @return <code>true</code> if the message matches the filter
   */
  @Override
  public boolean accept(Message message, Event.Builder builder) {
    ExceptionPayload epl = ((InternalMessage) message).getExceptionPayload();

    if (getExpectedType() == null) {
      return epl != null;
    } else if (epl != null) {
      return getExpectedType().isAssignableFrom(epl.getException().getClass());
    } else {
      return false;
    }
  }
}
