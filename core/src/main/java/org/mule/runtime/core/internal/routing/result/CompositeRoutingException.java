/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.result;

import static org.mule.runtime.api.exception.ExceptionHelper.getRootMuleException;
import static org.mule.runtime.api.message.Message.of;

import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.exception.ComposedErrorException;
import org.mule.runtime.api.exception.ErrorMessageAwareException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.privileged.processor.Router;
import org.mule.runtime.core.privileged.routing.RoutingResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link MuleException} used to aggregate exceptions thrown by several routes in the context of a single {@link Router}. This
 * exception implements {@link ComposedErrorException} so that a composite {@link Error} is created and also implements
 * {@link ErrorMessageAwareException} to provide an error message using {@link RoutingResult} that provides access to all route
 * results for use in error handlers.
 *
 * @since 3.5.0
 * @see RoutingResult
 */
public final class CompositeRoutingException extends MuleException implements ComposedErrorException, ErrorMessageAwareException {

  private static final String MESSAGE_TITLE = "Error(s) were found for route(s):";
  private static final String MESSAGE_SUB_TITLE = "Detailed Error(s) for route(s):";
  private static final String LEGACY_MESSAGE_TITLE = "Exception(s) were found for route(s): ";

  private static final long serialVersionUID = -4421728527040579605L;

  private final RoutingResult routingResult;
  private static final Logger LOGGER = LoggerFactory.getLogger(CompositeRoutingException.class);

  /**
   * Constructs a new {@link CompositeRoutingException}
   *
   * @param routingResult routing result object containing the results from all routes.
   */
  public CompositeRoutingException(RoutingResult routingResult) {
    super(buildExceptionMessage(routingResult));
    this.routingResult = routingResult;
  }

  @Override
  public String getDetailedMessage() {
    Map<String, Pair<Error, MuleException>> detailedFailures = getDetailedFailures();

    if (detailedFailures.isEmpty()) {
      return getLegacyDetailedMessage();
    } else {
      StringBuilder builder = new StringBuilder();
      // provide information about the composite exception itself
      builder.append(super.getDetailedMessage());
      // get detailed information about exceptions that make up composite exception
      builder.append(lineSeparator()).append(MESSAGE_SUB_TITLE).append(lineSeparator());
      for (Entry<String, Pair<Error, MuleException>> entry : detailedFailures.entrySet()) {
        MuleException muleException = entry.getValue().getSecond();
        Throwable exception = entry.getValue().getFirst().getCause();
        appendMessageForExceptions(builder, entry.getKey(), exception, muleException);
      }
      return builder.toString();
    }
  }

  private String getLegacyDetailedMessage() {
    StringBuilder builder = new StringBuilder();
    builder.append(LEGACY_MESSAGE_TITLE).append(lineSeparator());
    for (Entry<String, Error> entry : routingResult.getFailures().entrySet()) {
      MuleException muleException = getRootMuleException(entry.getValue().getCause());
      Throwable exception = entry.getValue().getCause();
      appendMessageForExceptions(builder, entry.getKey(), exception, muleException);
    }
    return builder.toString();
  }

  private void appendMessageForExceptions(StringBuilder builder, String route, Throwable exception, MuleException muleException) {
    String routeSubtitle = String.format("Route %s: ", route);
    builder.append(lineSeparator());
    if (muleException != null) {
      builder.append(routeSubtitle).append(muleException.getDetailedMessage());
    } else {
      builder.append(routeSubtitle)
          .append("Caught exception in Exception Strategy: " + exception.getMessage());
    }
  }

  private Map<String, Pair<Error, MuleException>> getDetailedFailures() {
    Method getDetailedFailuresMethod = null;
    Map<String, Pair<Error, MuleException>> detailedFailures = emptyMap();
    try {
      getDetailedFailuresMethod = RoutingResult.class.getMethod("getFailuresWithExceptionInfo");
      detailedFailures =
          (Map<String, Pair<Error, MuleException>>) getDetailedFailuresMethod.invoke(routingResult);
    } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
      LOGGER.warn("Invalid Invocation, Expected method {} doesn't exist", getDetailedFailuresMethod.getName());
    }
    return detailedFailures;
  }

  private static I18nMessage buildExceptionMessage(RoutingResult routingResult) {
    StringBuilder builder = new StringBuilder();
    for (Entry<String, Error> routeResult : routingResult.getFailures().entrySet()) {
      Throwable routeException = routeResult.getValue().getCause();
      builder.append(lineSeparator() + "\t").append("Route ").append(routeResult.getKey()).append(": ")
          .append(routeException.getClass().getName())
          .append(": ").append(routeException.getMessage());
    }
    if (!routingResult.getFailuresWithExceptionInfo().isEmpty()) {
      builder.insert(0, MESSAGE_TITLE);
    } else {
      builder.insert(0, LEGACY_MESSAGE_TITLE);
    }
    return I18nMessageFactory.createStaticMessage(builder.toString());
  }

  @Override
  public List<Error> getErrors() {
    if (!routingResult.getFailures().isEmpty()) {
      return routingResult.getFailures().values().stream().collect(toList());
    } else {
      return routingResult.getFailuresWithExceptionInfo().values().stream().map(pair -> pair.getFirst()).collect(toList());
    }
  }

  @Override
  public Message getErrorMessage() {
    return of(routingResult);
  }

}
