/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.exception;

import static org.mule.runtime.api.exception.ExceptionHelper.getRootMuleException;
import static org.mule.runtime.api.exception.ExceptionHelper.sanitize;
import static org.mule.runtime.api.exception.MuleException.isVerboseExceptions;
import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.api.notification.SecurityNotification.SECURITY_AUTHENTICATION_FAILED;
import static org.mule.runtime.core.api.error.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.api.error.Errors.Identifiers.UNKNOWN_ERROR_IDENTIFIER;

import static java.text.MessageFormat.format;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.notification.ExceptionNotification;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.notification.SecurityNotification;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.internal.construct.FlowBackPressureException;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultExceptionListener implements Initialisable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultExceptionListener.class);

  private static final String NOT_SET = "<not set>";

  private Logger logger = LOGGER;

  private NotificationDispatcher notificationFirer;

  private FlowConstructStatistics statistics;

  private String representation;

  /**
   * The initialise method is call every time the Exception strategy is assigned to a service or connector. This implementation
   * ensures that initialise is called only once. The actual initialisation code is contained in the <code>doInitialise()</code>
   * method.
   *
   * @throws InitialisationException
   */
  @Override
  public final synchronized void initialise() throws InitialisationException {
    logger.debug("Initialising exception listener: {}", this);
    if (representation == null) {
      representation = this.getClass().getSimpleName();
    }
  }

  private void fireNotification(Exception ex, CoreEvent event, ComponentLocation componentLocation) {
    if (ex.getCause() != null && getCause(ex) instanceof SecurityException) {
      fireNotification(new SecurityNotification((SecurityException) getCause(ex), SECURITY_AUTHENTICATION_FAILED));
    } else {
      Component component = null;
      if (ex instanceof MessagingException) {
        component = ((MessagingException) ex).getFailingComponent();
      }
      fireNotification(new ExceptionNotification(createInfo(event, ex, component),
                                                 componentLocation != null ? componentLocation : componentLocation));
    }
  }

  public void fireNotification(Exception ex, CoreEvent event) {
    fireNotification(ex, event, null);
  }

  private Throwable getCause(Exception ex) {
    return ex.getCause() instanceof TypedException ? ex.getCause().getCause() : ex.getCause();
  }

  public Pair<MuleException, String> resolveExceptionAndMessageToLog(Throwable t) {
    MuleException muleException = getRootMuleException(t);
    String logMessage = null;
    if (muleException != null) {
      if (!isVerboseExceptions() && t instanceof EventProcessingException
          && ((EventProcessingException) t).getEvent().getError()
              .map(e -> CORE_NAMESPACE_NAME.equals(e.getErrorType().getNamespace())
                  && UNKNOWN_ERROR_IDENTIFIER.equals(e.getErrorType().getIdentifier()))
              .orElse(false)) {
        logMessage = ((MuleException) sanitize(muleException)).getVerboseMessage();
      } else {
        logMessage = muleException.getDetailedMessage();
      }
    }
    return new Pair<>(muleException, logMessage);
  }

  public boolean resolveAndLogException(Throwable t) {
    Pair<MuleException, String> resolvedException = resolveExceptionAndMessageToLog(t);
    if (resolvedException.getSecond() == null) {
      doLogException("Caught exception in Exception Strategy: " + t.getMessage(), t);
      return true;
    }
    // First check if exception was not logged already
    // MULE-19344: Always log FlowBackPressureExceptions because they are created as a single instance.
    if (resolvedException.getFirst().getExceptionInfo().isAlreadyLogged()
        && !(resolvedException.getFirst() instanceof FlowBackPressureException)) {
      // Don't log anything, error while getting root or exception already logged.
      return false;
    }
    doLogException(resolvedException.getSecond(), null);
    resolvedException.getFirst().getExceptionInfo().setAlreadyLogged(true);
    return true;
  }

  protected void doLogException(String message, Throwable t) {
    if (t == null) {
      logger.error(message);
    } else {
      logger.error(message, t);
    }
  }

  public void processStatistics() {
    if (statistics != null) {
      statistics.incExecutionError();
    }
  }

  /**
   * Logs a fatal error message to the logging system. This should be used mostly if an error occurs in the exception listener
   * itself. This implementation logs the the message itself to the logs if it is not null
   *
   * @param event The MuleEvent currently being processed
   * @param t     the fatal exception to log
   */
  protected void logFatal(CoreEvent event, Throwable t) {
    if (statistics != null) {
      statistics.incFatalError();
    }

    String logUniqueId = Objects.toString(event.getCorrelationId(), NOT_SET);

    String printableLogMessage =
        format("Message identification summary here: id={0}, correlation={1}", logUniqueId, event.getGroupCorrelation());

    logger.error("Failed to dispatch message to error queue after it failed to process.  This may cause message loss. "
        + (event.getMessage() == null ? "" : printableLogMessage), t);
  }

  /**
   * Fires a server notification to all registered {@link ExceptionNotificationListener} eventManager.
   *
   * @param notification the notification to fire.
   */
  protected void fireNotification(Notification notification) {
    if (notificationFirer != null) {
      notificationFirer.dispatch(notification);
    } else if (logger.isWarnEnabled()) {
      logger.debug("notificationFirer is not yet available for firing notifications, ignoring event: {}", notification);
    }
  }

  public void setNotificationFirer(NotificationDispatcher notificationFirer) {
    this.notificationFirer = notificationFirer;
  }

  public void setStatistics(FlowConstructStatistics statistics) {
    this.statistics = statistics;
  }

  public void setRepresentation(String representation) {
    this.representation = representation;
  }

  @Override
  public String toString() {
    return representation;
  }

  public void setLogger(Logger logger) {
    this.logger = logger;
  }
}
