/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.routing;

import static java.lang.System.lineSeparator;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.core.api.processor.MessageRouter;
import org.mule.runtime.core.internal.config.ExceptionHelper;
import org.mule.runtime.core.exception.MessagingException;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This is a {@link MessagingException} used to aggregate exceptions thrown by several routes in the context of a single
 * {@link MessageRouter} Exceptions are correlated to each route through a sequential id
 * 
 * @since 3.5.0
 */
public class CompositeRoutingException extends MuleException {

  private static final String MESSAGE_TITLE = "Exception(s) were found for route(s): ";

  private static final long serialVersionUID = -4421728527040579607L;

  private final Map<Integer, Throwable> exceptions;

  /**
   * Constructs a new {@link CompositeRoutingException}
   * 
   * @param message message describing the failure
   * @param exceptions a {@link Map} in which the key is an {@link Integer} describing the index of the route that generated the
   *        error and the value is the {@link Throwable} itself
   */
  public CompositeRoutingException(I18nMessage message, Map<Integer, Throwable> exceptions) {
    super(message);
    this.exceptions = Collections.unmodifiableMap(exceptions);
  }

  public CompositeRoutingException(Map<Integer, Throwable> exceptions) {
    this(buildExceptionMessage(exceptions), exceptions);
  }

  /**
   * Returns the {@link Exception} for the given route index
   * 
   * @param index the index of a failing route
   * @return an {@link Exception} or <code>null</code> if no {@link Exception} was found for that index
   */
  public Throwable getExceptionForRouteIndex(Integer index) {
    return this.exceptions.get(index);
  }

  /**
   * @return a {@link Map} in which the key is an {@link Integer} describing the number of the route that generated the error and
   *         the value is the {@link Exception} itself
   */
  public Map<Integer, Throwable> getExceptions() {
    return this.exceptions;
  }

  @Override
  public String getDetailedMessage() {
    StringBuilder builder = new StringBuilder();
    builder.append(MESSAGE_TITLE).append(lineSeparator());

    for (Entry<Integer, Throwable> entry : getExceptions().entrySet()) {
      String routeSubtitle = String.format("Route %d:", entry.getKey());
      MuleException muleException = ExceptionHelper.getRootMuleException(entry.getValue());
      if (muleException != null) {
        builder.append(routeSubtitle).append(muleException.getDetailedMessage());
      } else {
        builder.append(routeSubtitle).append("Caught exception in Exception Strategy: " + entry.getValue().getMessage());
      }
    }
    return builder.toString();
  }

  private static I18nMessage buildExceptionMessage(Map<Integer, Throwable> exceptions) {
    StringBuilder builder = new StringBuilder();
    for (Integer route : exceptions.keySet()) {
      Throwable routeException = exceptions.get(route);
      builder.append(lineSeparator() + "\t").append(route).append(": ")
          .append(routeException.getCause() != null ? routeException.getCause().getMessage() : routeException.getMessage());
    }

    builder.insert(0, MESSAGE_TITLE);
    return I18nMessageFactory.createStaticMessage(builder.toString());
  }

}
