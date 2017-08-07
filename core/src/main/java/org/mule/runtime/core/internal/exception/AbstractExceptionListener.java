/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.lang.Boolean.TRUE;
import static java.text.MessageFormat.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.mule.runtime.core.api.context.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.core.api.context.notification.SecurityNotification.SECURITY_AUTHENTICATION_FAILED;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.GlobalNameableObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.ExceptionNotification;
import org.mule.runtime.core.api.context.notification.SecurityNotification;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.TypedException;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.config.ExceptionHelper;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the base class for exception strategies which contains several helper methods. However, you should probably inherit
 * from <code>AbstractMessagingExceptionStrategy</code> (if you are creating a Messaging Exception Strategy) or
 * <code>AbstractSystemExceptionStrategy</code> (if you are creating a System Exception Strategy) rather than directly from this
 * class.
 */
public abstract class AbstractExceptionListener extends AbstractMessageProcessorOwner implements GlobalNameableObject {

  protected static final String NOT_SET = "<not set>";

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  protected List<Processor> messageProcessors = new CopyOnWriteArrayList<>();

  protected AtomicBoolean initialised = new AtomicBoolean(false);

  protected boolean enableNotifications = true;
  protected String logException = TRUE.toString();

  protected String globalName;
  protected FlowConstructStatistics statistics;

  @Override
  public String getGlobalName() {
    return globalName;
  }

  @Override
  public void setGlobalName(String globalName) {
    this.globalName = globalName;
  }

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
  public final synchronized void initialise() throws InitialisationException {
    if (!initialised.get()) {
      doInitialise(muleContext);
      super.initialise();
      initialised.set(true);
    }
  }

  protected void doInitialise(MuleContext context) throws InitialisationException {
    requireNonNull(muleContext);
    logger.info("Initialising exception listener: " + toString());
  }

  protected void fireNotification(Exception ex, Event event) {
    if (enableNotifications) {
      if (ex.getCause() != null && getCause(ex) instanceof SecurityException) {
        fireNotification(new SecurityNotification((SecurityException) getCause(ex), SECURITY_AUTHENTICATION_FAILED));
      } else {
        fireNotification(new ExceptionNotification(createInfo(event, ex, null), getLocation()));
      }
    }
  }

  private Throwable getCause(Exception ex) {
    return ex.getCause() instanceof TypedException ? ex.getCause().getCause() : ex.getCause();
  }

  protected void doLogException(Throwable t) {
    MuleException muleException = ExceptionHelper.getRootMuleException(t);
    if (muleException != null) {
      logger.error(muleException.getDetailedMessage());
    } else {
      logger.error("Caught exception in Exception Strategy: " + t.getMessage(), t);
    }
  }

  /**
   * Logs a fatal error message to the logging system. This should be used mostly if an error occurs in the exception listener
   * itself. This implementation logs the the message itself to the logs if it is not null
   *
   * @param event The MuleEvent currently being processed
   * @param t the fatal exception to log
   */
  protected void logFatal(Event event, Throwable t) {
    if (statistics != null && statistics.isEnabled()) {
      statistics.incFatalError();
    }

    String logUniqueId = defaultString(event.getCorrelationId(), NOT_SET);

    String printableLogMessage =
        format("Message identification summary here: id={0}, correlation={1}", logUniqueId, event.getGroupCorrelation());

    logger.error("Failed to dispatch message to error queue after it failed to process.  This may cause message loss. "
        + (event.getMessage() == null ? "" : printableLogMessage), t);
  }

  public boolean isInitialised() {
    return initialised.get();
  }

  /**
   * Fires a server notification to all registered
   * {@link org.mule.runtime.core.api.context.notification.ExceptionNotificationListener} eventManager.
   *
   * @param notification the notification to fire.
   */
  protected void fireNotification(ServerNotification notification) {
    if (muleContext != null) {
      muleContext.fireNotification(notification);
    } else if (logger.isWarnEnabled()) {
      logger.debug("MuleContext is not yet available for firing notifications, ignoring event: " + notification);
    }
  }

  public boolean isEnableNotifications() {
    return enableNotifications;
  }

  public void setEnableNotifications(boolean enableNotifications) {
    this.enableNotifications = enableNotifications;
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return messageProcessors;
  }

  protected void commit() {
    TransactionCoordination.getInstance().commitCurrentTransaction();
  }

  protected void rollback(Exception ex) {
    if (TransactionCoordination.getInstance().getTransaction() != null) {
      TransactionCoordination.getInstance().rollbackCurrentTransaction();
    }
    if (ex instanceof MessagingException) {
      MessagingException messagingException = (MessagingException) ex;
      messagingException.setCauseRollback(true);
    }
  }

  public void setStatistics(FlowConstructStatistics statistics) {
    this.statistics = statistics;
  }
}
