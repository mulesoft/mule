/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.util;

import static org.mule.runtime.api.exception.ExceptionHelper.getExceptionsAsList;
import static org.mule.runtime.api.exception.MuleException.INFO_ALREADY_LOGGED_KEY;
import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.core.api.exception.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CRITICAL_IDENTIFIER;
import static org.mule.runtime.core.api.util.ExceptionUtils.getComponentIdentifierOf;
import static org.mule.runtime.core.api.util.ExceptionUtils.getMessagingExceptionCause;
import static org.mule.runtime.core.api.util.ExceptionUtils.isUnknownMuleError;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.internal.message.ErrorBuilder.builder;
import static org.mule.runtime.core.internal.util.InternalExceptionUtils.createErrorEvent;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.notification.EnrichedNotificationInfo;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.internal.exception.ErrorMapping;
import org.mule.runtime.core.internal.exception.ErrorMappingsAware;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.policy.FlowExecutionException;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Tries to resolve a new {@link MessagingException}s with the real cause of the problem from an incoming
 * {@link MessagingException} that carries a list of causes inside it so it can be thrown and no information is lost.
 *
 * @since 4.0
 */
public class MessagingExceptionResolver {

  private final Component component;

  public MessagingExceptionResolver(Component component) {
    this.component = component;
  }

  /**
   * Resolves a new {@link MessagingException} with the real cause of the problem based on the content of an Incoming
   * {@link MessagingException} with a chain of causes inside it and the current event that the exception is carrying.
   * <p>
   * This method will pick the FIRST cause exception that has a mule or extension KNOWN error as the real cause, if there is not
   * an exception in the causes that match with an Known error type then this method will try to find the error that the current
   * {@link Event} is carrying.
   * <p>
   * When there are multiple exceptions that contains the same root error type, then this method will wrap the one that has
   * highest position in the causes list
   *
   * @return a {@link MessagingException} with the proper {@link Error} associated to it's {@link CoreEvent}
   *
   * @since 4.1.3
   */
  public MessagingException resolve(final MessagingException me, ErrorTypeLocator locator,
                                    Collection<ExceptionContextProvider> exceptionContextProviders) {
    Optional<Pair<Throwable, ErrorType>> rootCause = findRoot(component, me, locator);

    if (!rootCause.isPresent()) {
      return updateCurrent(me, component, locator, exceptionContextProviders);
    }

    Throwable root = rootCause.get().getFirst();
    ErrorType rootErrorType = rootCause.get().getSecond();
    Component failingComponent = getFailingProcessor(me, root).orElse(component);

    ErrorType errorType = rootErrorType;
    if (component instanceof ErrorMappingsAware) {
      if (((ErrorMappingsAware) component).getErrorMappings().isEmpty()) {
        errorType = rootErrorType;
      } else {
        errorType = ((ErrorMappingsAware) component).getErrorMappings()
            .stream()
            .filter(m -> m.match(rootErrorType))
            .findFirst()
            .map(ErrorMapping::getTarget)
            .orElse(rootErrorType);
      }
    }

    CoreEvent event = quickCopy(builder(getMessagingExceptionCause(root)).errorType(errorType).build(), me.getEvent());

    MessagingException result;
    if (root instanceof MessagingException) {
      ((MessagingException) root).setProcessedEvent(event);
      result = ((MessagingException) root);
    } else {
      result = me instanceof FlowExecutionException ? new FlowExecutionException(event, root, failingComponent)
          : new MessagingException(event, root, failingComponent);
    }
    propagateAlreadyLogged(me, result);
    return enrich(result, failingComponent, component, event, exceptionContextProviders);
  }

  private void propagateAlreadyLogged(MessagingException origin, MuleException result) {
    if (origin.getInfo().containsKey(INFO_ALREADY_LOGGED_KEY)) {
      result.addInfo(INFO_ALREADY_LOGGED_KEY, origin.getInfo().get(INFO_ALREADY_LOGGED_KEY));
    }
  }

  private Optional<Pair<Throwable, ErrorType>> findRoot(Component obj, MessagingException me, ErrorTypeLocator locator) {
    List<Pair<Throwable, ErrorType>> errors = collectErrors(obj, me, locator);

    if (errors.isEmpty()) {
      return collectCritical(obj, me, locator).stream().findFirst();
    }
    // We look if there is a more specific error in the chain that matches with the root error (is child or has the same error)
    SingleErrorTypeMatcher matcher = new SingleErrorTypeMatcher(errors.get(errors.size() - 1).getSecond());

    return errors.stream()
        .filter(p -> matcher.match(p.getSecond()))
        .findFirst();
  }

  private List<Pair<Throwable, ErrorType>> collectErrors(Component obj, MessagingException me, ErrorTypeLocator locator) {
    List<Pair<Throwable, ErrorType>> errors = new LinkedList<>();
    final List<Throwable> exceptionsAsList = getExceptionsAsList(me);
    for (Throwable e : exceptionsAsList) {
      ErrorType type = errorTypeFromException(obj, locator, e);
      if (!isUnknownMuleError(type) && !isCriticalMuleError(type)) {
        errors.add(0, new Pair<>(e, type));
      }
    }
    return errors;
  }

  private List<Pair<Throwable, ErrorType>> collectCritical(Component obj, MessagingException me, ErrorTypeLocator locator) {
    List<Pair<Throwable, ErrorType>> errors = new LinkedList<>();
    final List<Throwable> exceptionsAsList = getExceptionsAsList(me);
    for (Throwable e : exceptionsAsList) {
      ErrorType type = errorTypeFromException(obj, locator, e);
      if (isCriticalMuleError(type)) {
        errors.add(new Pair<>(e, type));
      }
    }
    return errors;
  }

  private MessagingException updateCurrent(MessagingException me, Component processor, ErrorTypeLocator locator,
                                           Collection<ExceptionContextProvider> exceptionContextProviders) {
    CoreEvent errorEvent = createErrorEvent(me.getEvent(), processor, me, locator);
    Component failingProcessor = me.getFailingComponent() != null ? me.getFailingComponent() : processor;
    MessagingException updated =
        me instanceof FlowExecutionException ? new FlowExecutionException(errorEvent, me.getCause(), failingProcessor)
            : new MessagingException(me.getI18nMessage(), errorEvent, me.getCause(), failingProcessor);

    return enrich(updated, failingProcessor, processor, errorEvent, exceptionContextProviders);
  }

  private Optional<Component> getFailingProcessor(MessagingException me, Throwable root) {
    Component failing = me.getFailingComponent();
    if (failing == null && root instanceof MessagingException) {
      failing = ((MessagingException) root).getFailingComponent();
    }
    return Optional.ofNullable(failing);
  }

  private ErrorType errorTypeFromException(Component failing, ErrorTypeLocator locator, Throwable e) {
    final ErrorType mapped;
    if (isMessagingExceptionWithError(e)) {
      mapped = ((MessagingException) e).getEvent().getError().map(Error::getErrorType).orElse(null);
    } else {
      final ComponentIdentifier identifier = getComponentIdentifierOf(failing);

      if (identifier != null) {
        mapped = locator.lookupComponentErrorType(identifier, e);
      } else {
        mapped = null;
      }
    }

    return mapped != null ? mapped : locator.lookupErrorType(e);
  }

  private boolean isMessagingExceptionWithError(Throwable cause) {
    return cause instanceof MessagingException && ((MessagingException) cause).getEvent().getError().isPresent();
  }

  private <T extends MuleException> T enrich(T me, Component failing, Component handling, CoreEvent event,
                                             Collection<ExceptionContextProvider> exceptionContextProviders) {
    EnrichedNotificationInfo notificationInfo = createInfo(event, me, null);
    for (ExceptionContextProvider exceptionContextProvider : exceptionContextProviders) {
      exceptionContextProvider.putContextInfo(me.getInfo(), notificationInfo, failing);
    }
    return me;
  }

  private boolean isCriticalMuleError(ErrorType type) {
    return type.getNamespace().equals(CORE_NAMESPACE_NAME) && type.getIdentifier().equals(CRITICAL_IDENTIFIER);
  }
}
