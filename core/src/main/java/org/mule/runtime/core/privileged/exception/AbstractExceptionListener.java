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

import static java.lang.Boolean.TRUE;
import static java.text.MessageFormat.format;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.notification.ExceptionNotification;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.notification.SecurityNotification;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.construct.FlowBackPressureException;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the base class for exception strategies which contains several helper methods. However, you should probably inherit
 * from <code>AbstractMessagingExceptionStrategy</code> (if you are creating a Messaging Exception Strategy) or
 * <code>AbstractSystemExceptionStrategy</code> (if you are creating a System Exception Strategy) rather than directly from this
 * class.
 *
 * @deprecated Use either {@link AbstractDeclaredExceptionListener} or {@link DefaultExceptionListener}.
 */
@NoExtend
@Deprecated
public abstract class AbstractExceptionListener extends AbstractMessageProcessorOwner {

  protected static final String NOT_SET = "<not set>";

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  @Inject
  protected NotificationDispatcher notificationFirer;

  private final List<Processor> messageProcessors = new CopyOnWriteArrayList<>();

  private final AtomicBoolean initialised = new AtomicBoolean(false);

  private boolean enableNotifications = true;
  protected String logException = TRUE.toString();

  protected FlowConstructStatistics statistics;

  private String representation;

  public List<Processor> getMessageProcessors() {
    return messageProcessors;
  }

  public void setMessageProcessors(List<Processor> processors) {
    if (processors != null) {
      this.messageProcessors.clear();
      this.messageProcessors.addAll(processors);
    } else {
      throw new IllegalArgumentException("List of targets = null");
    }
  }

  /**
   * The initialise method is call every time the Exception strategy is assigned to a service or connector. This implementation
   * ensures that initialise is called only once. The actual initialisation code is contained in the <code>doInitialise()</code>
   * method.
   *
   * @throws InitialisationException
   */
  @Override
  public synchronized void initialise() throws InitialisationException {
    if (!initialised.get()) {
      doInitialise();
      super.initialise();
      representation = this.getClass().getSimpleName() + (getLocation() != null ? " @ " + getLocation().getLocation() : "");
      initialised.set(true);
    }
  }

  protected void doInitialise() throws InitialisationException {
    if (logger.isDebugEnabled()) {
      logger.debug("Initialising exception listener: " + toString());
    }
    doInitialise(muleContext);
  }

  /**
   * @deprecated Implement {@link #doInitialise()} instead.
   */
  @Deprecated
  protected void doInitialise(MuleContext muleContext) throws InitialisationException {
    // nothing to do
  }

  protected void fireNotification(Exception ex, CoreEvent event, ComponentLocation componentLocation) {
    if (enableNotifications) {
      if (ex.getCause() != null && getCause(ex) instanceof SecurityException) {
        fireNotification(new SecurityNotification((SecurityException) getCause(ex), SECURITY_AUTHENTICATION_FAILED));
      } else {
        Component component = null;
        if (ex instanceof MessagingException) {
          component = ((MessagingException) ex).getFailingComponent();
        }
        fireNotification(new ExceptionNotification(createInfo(event, ex, component),
                                                   componentLocation != null ? componentLocation : getLocation()));
      }
    }
  }

  protected void fireNotification(Exception ex, CoreEvent event) {
    fireNotification(ex, event, null);
  }

  private Throwable getCause(Exception ex) {
    return ex.getCause() instanceof TypedException ? ex.getCause().getCause() : ex.getCause();
  }

  protected Pair<MuleException, String> resolveExceptionAndMessageToLog(Throwable t) {
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

  protected void resolveAndLogException(Throwable t) {
    Pair<MuleException, String> resolvedException = resolveExceptionAndMessageToLog(t);
    if (resolvedException.getSecond() == null) {
      doLogException("Caught exception in Exception Strategy: " + t.getMessage(), t);
      return;
    }
    // First check if exception was not logged already
    // MULE-19344: Always log FlowBackPressureExceptions because they are created as a single instance.
    if (resolvedException.getFirst().getExceptionInfo().isAlreadyLogged()
        && !(resolvedException.getFirst() instanceof FlowBackPressureException)) {
      // Don't log anything, error while getting root or exception already logged.
      return;
    }
    doLogException(resolvedException.getSecond(), null);
    resolvedException.getFirst().getExceptionInfo().setAlreadyLogged(true);
  }

  protected void doLogException(String message, Throwable t) {
    if (t == null) {
      logger.error(message);
    } else {
      logger.error(message, t);
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

  public boolean isInitialised() {
    return initialised.get();
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
      logger.debug("notificationFirer is not yet available for firing notifications, ignoring event: " + notification);
    }
  }

  public boolean isEnableNotifications() {
    return enableNotifications;
  }

  public void setEnableNotifications(boolean enableNotifications) {
    this.enableNotifications = enableNotifications;
  }

  public void setLogException(String logException) {
    this.logException = logException;
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return messageProcessors;
  }

  protected void commit() {
    TransactionCoordination.getInstance().commitCurrentTransaction();
  }

  protected void rollback(Exception ex) {
    TransactionCoordination.getInstance().rollbackCurrentTransaction();
  }

  public void setNotificationFirer(NotificationDispatcher notificationFirer) {
    this.notificationFirer = notificationFirer;
  }

  public void setStatistics(FlowConstructStatistics statistics) {
    this.statistics = statistics;
  }

  @Override
  public String toString() {
    return representation;
  }
}
