/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static java.text.MessageFormat.format;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.mule.runtime.core.api.context.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.core.context.notification.SecurityNotification.SECURITY_AUTHENTICATION_FAILED;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.GlobalNameableObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.context.notification.ExceptionNotification;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.message.ExceptionMessage;
import org.mule.runtime.core.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.routing.filters.WildcardFilter;
import org.mule.runtime.core.routing.outbound.MulticastingRouter;
import org.mule.runtime.core.transaction.TransactionCoordination;

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

  protected WildcardFilter rollbackTxFilter;
  protected WildcardFilter commitTxFilter;

  protected boolean enableNotifications = true;
  protected String logException = "mel:true";

  protected String globalName;

  @Override
  public String getGlobalName() {
    return globalName;
  }

  @Override
  public void setGlobalName(String globalName) {
    this.globalName = globalName;
  }

  protected boolean isRollback(Throwable t) {
    // Work with the root exception, not anything thaat wraps it
    t = ExceptionHelper.getRootException(t);
    if (rollbackTxFilter == null && commitTxFilter == null) {
      return true;
    } else {
      return (rollbackTxFilter != null && rollbackTxFilter.accept(t.getClass().getName()))
          || (commitTxFilter != null && !commitTxFilter.accept(t.getClass().getName()));
    }
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

  public void addEndpoint(Processor processor) {
    if (processor != null) {
      messageProcessors.add(processor);
    }
  }

  public boolean removeMessageProcessor(Processor processor) {
    return messageProcessors.remove(processor);
  }

  protected Throwable getExceptionType(Throwable t, Class<? extends Throwable> exceptionType) {
    while (t != null) {
      if (exceptionType.isAssignableFrom(t.getClass())) {
        return t;
      }

      t = t.getCause();
    }

    return null;
  }

  /**
   * The initialise method is call every time the Exception stategy is assigned to a service or connector. This implementation
   * ensures that initialise is called only once. The actual initialisation code is contained in the <code>doInitialise()</code>
   * method.
   *
   * @throws InitialisationException
   */
  @Override
  public final synchronized void initialise() throws InitialisationException {
    if (!initialised.get()) {
      super.initialise();
      doInitialise(muleContext);
      initialised.set(true);
    }
  }

  protected void doInitialise(MuleContext context) throws InitialisationException {
    logger.info("Initialising exception listener: " + toString());
    super.setMessagingExceptionHandler(new MessagingExceptionHandlerToSystemAdapter(muleContext));
  }

  protected void fireNotification(Exception ex, Event event) {
    if (enableNotifications) {
      if (ex.getCause() != null && getCause(ex) instanceof SecurityException) {
        fireNotification(new SecurityNotification((SecurityException) getCause(ex), SECURITY_AUTHENTICATION_FAILED));
      } else {
        fireNotification(new ExceptionNotification(createInfo(event, ex, null), flowConstruct));
      }
    }
  }

  private Throwable getCause(Exception ex) {
    return ex.getCause() instanceof TypedException ? ex.getCause().getCause() : ex.getCause();
  }

  /**
   * Routes the current exception to an error endpoint such as a Dead Letter Queue (jms) This method is only invoked if there is a
   * Message available to dispatch. The message dispatched from this method will be an <code>ExceptionMessage</code> which
   * contains the exception thrown the Message and any context information.
   *
   * @param event the MuleEvent being processed when the exception occurred
   * @param flowConstruct the flow that was processing the event when the exception occurred.
   * @param t the exception thrown. This will be sent with the ExceptionMessage
   * @see ExceptionMessage
   */
  protected Event routeException(Event event, FlowConstruct flowConstruct, Throwable t) {
    Event result = event;

    if (!messageProcessors.isEmpty()) {
      try {
        if (logger.isDebugEnabled()) {
          logger.debug("Message being processed is: "
              + (muleContext.getTransformationService().getPayloadForLogging(event.getMessage())));
        }
        // Create an ExceptionMessage which contains the original payload, the exception, and some additional context info.
        ExceptionMessage msg =
            new ExceptionMessage(event, t, flowConstruct.getName(), event.getContext().getOriginatingConnectorName());

        Message exceptionMessage = Message.builder(event.getMessage()).payload(msg).build();

        MulticastingRouter router = buildRouter();
        router.setRoutes(getMessageProcessors());
        router.setMuleContext(muleContext);

        // Route the ExceptionMessage to the new router
        result = router.route(Event.builder(event).message(exceptionMessage).build());
      } catch (Exception e) {
        logFatal(event, e);
      }
    }

    return result;
  }

  protected MulticastingRouter buildRouter() {
    // Create an outbound router with all endpoints configured on the exception strategy
    MulticastingRouter router = new MulticastingRouter();
    return router;
  }

  protected void closeStream(Message message) {
    if (muleContext == null || muleContext.isDisposing() || muleContext.isDisposed()) {
      return;
    }
    if (message != null) {
      muleContext.getStreamCloserService().closeStream(message.getPayload().getValue());
    }
  }

  /**
   * Used to log the error passed into this Exception Listener
   *
   * @param t the exception thrown
   */
  protected void logException(Throwable t, Event event) {
    if (this.muleContext.getExpressionManager().evaluateBoolean(logException, event, flowConstruct, true, true)) {
      doLogException(t);
    }
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
    FlowConstructStatistics statistics = flowConstruct.getStatistics();
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

  public WildcardFilter getCommitTxFilter() {
    return commitTxFilter;
  }

  public void setCommitTxFilter(WildcardFilter commitTxFilter) {
    this.commitTxFilter = commitTxFilter;
  }

  public boolean isEnableNotifications() {
    return enableNotifications;
  }

  public void setEnableNotifications(boolean enableNotifications) {
    this.enableNotifications = enableNotifications;
  }

  /**
   * Determines whether the handled exception will be logged to its standard logger in the ERROR level before being handled.
   */
  public String isLogException() {
    return logException;
  }

  public void setLogException(String logException) {
    this.logException = logException;
  }

  public WildcardFilter getRollbackTxFilter() {
    return rollbackTxFilter;
  }

  public void setRollbackTxFilter(WildcardFilter rollbackTxFilter) {
    this.rollbackTxFilter = rollbackTxFilter;
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return messageProcessors;
  }

  @Override
  public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
    return;
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

}
