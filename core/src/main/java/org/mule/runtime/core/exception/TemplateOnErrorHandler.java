/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.core.context.notification.ExceptionStrategyNotification.PROCESS_END;
import static org.mule.runtime.core.context.notification.ExceptionStrategyNotification.PROCESS_START;
import static org.mule.runtime.core.message.ErrorBuilder.builder;
import static org.mule.runtime.core.message.ErrorTypeBuilder.ANY;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.connector.NonBlockingReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.context.notification.ExceptionStrategyNotification;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.message.ErrorBuilder;
import org.mule.runtime.core.processor.AbstractRequestResponseMessageProcessor;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.routing.requestreply.ReplyToPropertyRequestReplyReplier;
import org.mule.runtime.core.transaction.TransactionCoordination;

public abstract class TemplateOnErrorHandler extends AbstractExceptionListener
    implements MessagingExceptionHandlerAcceptor {

  private MessageProcessorChain configuredMessageProcessors;
  private MessageProcessor replyToMessageProcessor = new ReplyToPropertyRequestReplyReplier();
  private ErrorType errorType = ANY;
  private String when;
  private boolean handleException;

  @Override
  final public MuleEvent handleException(MessagingException exception, MuleEvent event) {
    try {
      return new ExceptionMessageProcessor(exception, muleContext, flowConstruct).process(event);
    } catch (MuleException e) {
      throw new RuntimeException(e);
    }
  }

  private class ExceptionMessageProcessor extends AbstractRequestResponseMessageProcessor {

    private MessagingException exception;

    public ExceptionMessageProcessor(MessagingException exception, MuleContext muleContext, FlowConstruct flowConstruct) {
      this.exception = exception;
      setMuleContext(muleContext);
      setFlowConstruct(flowConstruct);
    }

    @Override
    protected MuleEvent processRequest(MuleEvent request) throws MuleException {
      if (!handleException && request.getReplyToHandler() instanceof NonBlockingReplyToHandler) {
        request = new DefaultMuleEvent(request, flowConstruct, null, null, true);
      }
      muleContext.getNotificationManager()
          .fireNotification(new ExceptionStrategyNotification(request, flowConstruct, PROCESS_START));
      fireNotification(exception);
      logException(exception, request);
      processStatistics();
      request
          .setMessage(MuleMessage.builder(request.getMessage()).exceptionPayload(new DefaultExceptionPayload(exception)).build());
      request.setError(ErrorBuilder.builder(exception).build());
      request.setMessage(MuleMessage.builder(request.getMessage()).build());

      markExceptionAsHandledIfRequired(exception);
      return beforeRouting(exception, request);
    }

    @Override
    protected MuleEvent processResponse(MuleEvent response, MuleEvent request) throws MuleException {
      processOutboundRouterStatistics();
      response = afterRouting(exception, response);
      if (response != null && !VoidMuleEvent.getInstance().equals(response)) {
        // Only process reply-to if non-blocking is not enabled. Checking the exchange pattern is not sufficient
        // because JMS inbound endpoints for example use a REQUEST_RESPONSE exchange pattern and async processing.
        if (!(request.isAllowNonBlocking() && request.getReplyToHandler() instanceof NonBlockingReplyToHandler)) {
          processReplyTo(response, exception);
        }
        closeStream(response.getMessage());
        nullifyExceptionPayloadIfRequired(response);
      }
      return response;
    }

    @Override
    protected MuleEvent processNext(MuleEvent event) throws MuleException {
      return route(event, exception);
    }

    @Override
    protected MuleEvent processCatch(MuleEvent event, MessagingException exception) throws MessagingException {
      try {
        logger.error("Exception during exception strategy execution");
        doLogException(exception);
        TransactionCoordination.getInstance().rollbackCurrentTransaction();
      } catch (Exception ex) {
        //Do nothing
      }

      event.setError(builder(exception).build());
      event.setMessage(MuleMessage.builder(event.getMessage()).exceptionPayload(new DefaultExceptionPayload(exception)).build());
      return event;
    }

    @Override
    protected void processFinally(MuleEvent event, MessagingException exception) {
      muleContext.getNotificationManager().fireNotification(new ExceptionStrategyNotification(event, flowConstruct, PROCESS_END));
    }

  }

  private void markExceptionAsHandledIfRequired(Exception exception) {
    if (handleException) {
      markExceptionAsHandled(exception);
    }
  }

  protected void markExceptionAsHandled(Exception exception) {
    if (exception instanceof MessagingException) {
      ((MessagingException) exception).setHandled(true);
    }
  }

  protected void processReplyTo(MuleEvent event, Exception e) {
    try {
      replyToMessageProcessor.process(event);
    } catch (MuleException ex) {
      logFatal(event, ex);
    }
  }

  protected void nullifyExceptionPayloadIfRequired(MuleEvent event) {
    if (this.handleException) {
      event.setError(null);
      event.setMessage(MuleMessage.builder(event.getMessage()).exceptionPayload(null).build());
    }
  }

  private void processStatistics() {
    FlowConstructStatistics statistics = flowConstruct.getStatistics();
    if (statistics != null && statistics.isEnabled()) {
      statistics.incExecutionError();
    }
  }

  protected MuleEvent route(MuleEvent event, MessagingException t) {
    if (!getMessageProcessors().isEmpty()) {
      try {
        event.setError(builder(t).build());
        event.setMessage(MuleMessage.builder(event.getMessage()).exceptionPayload(new DefaultExceptionPayload(t)).build());
        MuleEvent result = configuredMessageProcessors.process(event);
        return result;
      } catch (Exception e) {
        logFatal(event, e);
      }
    }
    return event;
  }

  @Override
  protected void doInitialise(MuleContext muleContext) throws InitialisationException {
    super.doInitialise(muleContext);
    DefaultMessageProcessorChainBuilder defaultMessageProcessorChainBuilder =
        new DefaultMessageProcessorChainBuilder(this.flowConstruct);
    try {
      configuredMessageProcessors = defaultMessageProcessorChainBuilder.chain(getMessageProcessors()).build();
    } catch (MuleException e) {
      throw new InitialisationException(e, this);
    }
  }


  public void setWhen(String when) {
    this.when = when;
  }


  @Override
  public boolean accept(MuleEvent event) {
    return acceptsAll() || acceptsEvent(event) || acceptsErrorType(event)
        || (when != null && muleContext.getExpressionManager().evaluateBoolean(when, event, flowConstruct));
  }

  private boolean acceptsErrorType(MuleEvent event) {
    //For now, match by representation, since we can't assign the exact type when parsing
    return event.getError().getErrorType().getStringRepresentation().equals(errorType.getStringRepresentation());
  }

  /**
   * Determines if the exception strategy should process or not a message inside a choice exception strategy.
   * <p>
   * Useful for exception strategies which ALWAYS must accept certain types of events despite when condition is not true.
   *
   * @param event   The MuleEvent being processed
   * @return  true if it should process the exception for the current event, false otherwise.
   */
  protected boolean acceptsEvent(MuleEvent event) {
    return false;
  }

  @Override
  public boolean acceptsAll() {
    return ANY.equals(errorType) && when == null;
  }

  protected MuleEvent afterRouting(MessagingException exception, MuleEvent event) {
    return event;
  }

  protected MuleEvent beforeRouting(MessagingException exception, MuleEvent event) {
    return event;
  }

  @Override
  public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
    return;
  }

  public void setHandleException(boolean handleException) {
    this.handleException = handleException;
  }

  public void setErrorType(ErrorType errorType) {
    this.errorType = errorType;
  }

}
