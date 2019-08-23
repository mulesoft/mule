/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Unhandleable.OVERLOAD;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.updateRootContainerName;
import static reactor.core.publisher.Mono.error;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.processor.AbstractMuleObjectOwner;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.exception.AbstractExceptionListener;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;

import java.util.List;

import javax.inject.Inject;

import org.reactivestreams.Publisher;

/**
 * Selects which "on error" handler to execute based on filtering. Replaces the choice-exception-strategy from Mule 3. On error
 * handlers must implement {@link MessagingExceptionHandlerAcceptor} to be part of ErrorHandler.
 *
 * @since 4.0
 */
public class ErrorHandler extends AbstractMuleObjectOwner<MessagingExceptionHandlerAcceptor>
    implements MessagingExceptionHandlerAcceptor, MuleContextAware, Lifecycle {

  private static final String MUST_ACCEPT_ANY_EVENT_MESSAGE = "Default error handler must accept any event.";
  private List<MessagingExceptionHandlerAcceptor> exceptionListeners;
  private ErrorType anyErrorType;
  protected String name;

  @Inject
  private NotificationDispatcher notificationDispatcher;

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    anyErrorType = muleContext.getErrorTypeRepository().getAnyErrorType();
    addCriticalErrorHandler();
    addDefaultErrorHandlerIfRequired();
    validateConfiguredExceptionStrategies();
  }

  @Override
  public CoreEvent handleException(Exception exception, CoreEvent event) {
    if (!(exception instanceof MessagingException)) {
      exception = new MessagingException(event, exception);
    }

    for (MessagingExceptionHandlerAcceptor exceptionListener : exceptionListeners) {
      if (exceptionListener.accept(event)) {
        return exceptionListener.handleException(exception, event);
      }
    }
    throw new MuleRuntimeException(createStaticMessage(MUST_ACCEPT_ANY_EVENT_MESSAGE));
  }

  @Override
  public Publisher<CoreEvent> apply(Exception exception) {
    if (exception instanceof MessagingException) {
      CoreEvent event = ((MessagingException) exception).getEvent();
      ((MessagingException) exception).setProcessedEvent(event);
      try {
        for (MessagingExceptionHandlerAcceptor exceptionListener : exceptionListeners) {
          if (exceptionListener.accept(event)) {
            return exceptionListener.apply(exception);
          }
        }
        throw new MuleRuntimeException(createStaticMessage(MUST_ACCEPT_ANY_EVENT_MESSAGE));
      } catch (Exception e) {
        return error(new MessagingExceptionResolver(this).resolve(new MessagingException(event, e, this),
                                                                  muleContext));
      }
    } else {
      // This should never occur since all exceptions at this point are ME
      return error(exception);
    }
  }

  @Override
  protected List<MessagingExceptionHandlerAcceptor> getOwnedObjects() {
    return exceptionListeners != null ? unmodifiableList(exceptionListeners) : emptyList();
  }

  @Override
  public boolean accept(CoreEvent event) {
    return true;
  }

  @Override
  public boolean acceptsAll() {
    return true;
  }

  private void addCriticalErrorHandler() {
    exceptionListeners.add(0, new OnCriticalErrorHandler(new SingleErrorTypeMatcher(muleContext.getErrorTypeRepository()
        .getErrorType(OVERLOAD).get())));
  }

  private void addDefaultErrorHandlerIfRequired() throws InitialisationException {
    MessagingExceptionHandlerAcceptor lastAcceptor = exceptionListeners.get(exceptionListeners.size() - 1);
    if (!lastAcceptor.acceptsAll() && !matchesAny(lastAcceptor)) {
      String defaultErrorHandlerName = getMuleContext().getConfiguration().getDefaultErrorHandlerName();
      if (defaultErrorHandlerName != null && defaultErrorHandlerName.equals(name)) {
        logger
            .warn("Default 'error-handler' should include a final \"catch-all\" 'on-error-propagate'. Attempting implicit injection.");
      }
      OnErrorPropagateHandler acceptsAllOnErrorPropagate = new OnErrorPropagateHandler();
      acceptsAllOnErrorPropagate.setRootContainerName(getRootContainerLocation().toString());
      acceptsAllOnErrorPropagate.setNotificationFirer(notificationDispatcher);
      initialiseIfNeeded(acceptsAllOnErrorPropagate, muleContext);
      this.exceptionListeners.add(acceptsAllOnErrorPropagate);
    }
  }

  /**
   * Defines whether a handler effectively accepts all errors. We cannot modify {@link OnErrorPropagateHandler#acceptsAll()}
   * without breaking compatibility since {@link #validateOnlyLastAcceptsAll()} would start failing for currently accepted mid
   * level handlers for "ANY".
   *
   * @param acceptor the handler to check
   * @return whether this is an {@link OnErrorPropagateHandler} with a matcher for "ANY"
   */
  private boolean matchesAny(MessagingExceptionHandlerAcceptor acceptor) {
    return acceptor instanceof OnErrorPropagateHandler && ((OnErrorPropagateHandler) acceptor).acceptsErrorType(anyErrorType);
  }

  private void validateConfiguredExceptionStrategies() {
    validateOnlyLastAcceptsAll();
  }

  private void validateOnlyLastAcceptsAll() {
    for (int i = 0; i < exceptionListeners.size() - 1; i++) {
      MessagingExceptionHandlerAcceptor acceptor = exceptionListeners.get(i);
      if (acceptor.acceptsAll()) {
        throw new MuleRuntimeException(
                                       createStaticMessage("Only last <on-error> inside <error-handler> can accept any errors. Otherwise the following handlers will never execute."));
      } else if (matchesAny(acceptor)) {
        logger
            .warn("Only the last <on-error> inside an <error-handler> should accept any errors. Otherwise the following handlers will never execute.");
      }
    }
  }

  public void setExceptionListeners(List<MessagingExceptionHandlerAcceptor> exceptionListeners) {
    this.exceptionListeners = exceptionListeners;
  }

  public List<MessagingExceptionHandlerAcceptor> getExceptionListeners() {
    return unmodifiableList(exceptionListeners);
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setRootContainerName(String rootContainerName) {
    updateRootContainerName(rootContainerName, this);
    for (MessagingExceptionHandlerAcceptor exceptionListener : exceptionListeners) {
      if (exceptionListener instanceof TemplateOnErrorHandler) {
        ((TemplateOnErrorHandler) exceptionListener).setRootContainerName(rootContainerName);
      }
    }
  }

  public void setStatistics(FlowConstructStatistics flowStatistics) {
    for (MessagingExceptionHandlerAcceptor exceptionListener : exceptionListeners) {
      if (exceptionListener instanceof AbstractExceptionListener) {
        ((AbstractExceptionListener) exceptionListener).setStatistics(flowStatistics);
      }
    }
  }

  public void setExceptionListenersLocation(Location flowLocation) {
    List<MessagingExceptionHandlerAcceptor> listeners =
        this.getExceptionListeners().stream().map(exceptionListener -> (exceptionListener instanceof TemplateOnErrorHandler)
            ? ((TemplateOnErrorHandler) exceptionListener).duplicateFor(flowLocation) : exceptionListener).collect(toList());
    this.setExceptionListeners(listeners);
  }

}
