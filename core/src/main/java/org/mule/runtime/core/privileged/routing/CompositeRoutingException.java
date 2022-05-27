/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.privileged.routing;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.message.Message.of;

import org.mule.runtime.api.exception.ComposedErrorException;
import org.mule.runtime.api.exception.ErrorMessageAwareException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.internal.config.ExceptionHelper;
import org.mule.runtime.core.privileged.exception.EventProcessingException;
import org.mule.runtime.core.privileged.processor.Router;

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

  private static final String MESSAGE_TITLE = "Exception/Error(s) were found for route(s):";
  private static final String MESSAGE_SUB_TITLE = "Detailed Exception/Error(s) for route(s):";

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
    StringBuilder builder = new StringBuilder();
    // provide information about the composite exception itself
    builder.append(super.getDetailedMessage());

    // get detailed information about the exceptions that make up the composite exception
    builder.append(lineSeparator()).append(MESSAGE_SUB_TITLE).append(lineSeparator());

    Method getDetailedFailuresMethod = null;
    Map<String, Pair<Error, EventProcessingException>> detailedFailures = null;
    try {
      getDetailedFailuresMethod = RoutingResult.class.getMethod("getFailuresWithExceptionInfo");
      detailedFailures =
          (Map<String, Pair<Error, EventProcessingException>>) getDetailedFailuresMethod.invoke(routingResult);
    } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
      e.printStackTrace();
      LOGGER.warn("Invalid Invocation, Expected method doesn't exist");
    }

    if (detailedFailures.isEmpty()) {
      // If we only have an error map that doesn't provide information about exceptions, process with original logic
      for (Entry<String, Error> entry : routingResult.getFailures().entrySet()) {
        String routeSubtitle = String.format("Route %s: ", entry.getKey());
        MuleException muleException = ExceptionHelper.getRootMuleException(entry.getValue().getCause());
        if (muleException != null) {
          builder.append(routeSubtitle).append(muleException.getDetailedMessage());
        } else {
          builder.append(routeSubtitle)
              .append("Caught exception in Exception Strategy: " + entry.getValue().getCause().getMessage());
        }
      }
    } else {
      // If we are provided an error map that also stores the exceptions that make up the composite exception,
      // process with new logic to provide detailed error message
      for (Entry<String, Pair<Error, EventProcessingException>> entry : detailedFailures.entrySet()) {
        String routeSubtitle = String.format("Route %s: ", entry.getKey());
        MuleException muleException = entry.getValue().getSecond();

        builder.append(lineSeparator());
        if (muleException != null) {
          builder.append(routeSubtitle).append(muleException.getVerboseMessage());
        }
      }
    }
    return builder.toString();
  }

  private static I18nMessage buildExceptionMessage(RoutingResult routingResult) {
    StringBuilder builder = new StringBuilder();
    for (Entry<String, Error> routeResult : routingResult.getFailures().entrySet()) {
      Throwable routeException = routeResult.getValue().getCause();
      builder.append(lineSeparator() + "\t").append(routeResult.getKey()).append(": ").append(routeException.getClass().getName())
          .append(": ").append(routeException.getMessage());
    }

    builder.insert(0, MESSAGE_TITLE);
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
