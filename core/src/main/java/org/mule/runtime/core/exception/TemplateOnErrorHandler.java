/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.context.notification.ExceptionStrategyNotification.PROCESS_END;
import static org.mule.runtime.core.context.notification.ExceptionStrategyNotification.PROCESS_START;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.context.notification.ExceptionStrategyNotification;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.processor.AbstractRequestResponseMessageProcessor;
import org.mule.runtime.core.routing.requestreply.ReplyToPropertyRequestReplyReplier;
import org.mule.runtime.core.transaction.TransactionCoordination;

public abstract class TemplateOnErrorHandler extends AbstractExceptionListener
    implements MessagingExceptionHandlerAcceptor {

  private MessageProcessorChain configuredMessageProcessors;
  private Processor replyToMessageProcessor = new ReplyToPropertyRequestReplyReplier();

  private ErrorTypeMatcher errorTypeMatcher = null;
  private String when;
  private boolean handleException;

  @Override
  final public Event handleException(MessagingException exception, Event event) {
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
    protected Event processRequest(Event request) throws MuleException {
      muleContext.getNotificationManager()
          .fireNotification(new ExceptionStrategyNotification(request, flowConstruct, PROCESS_START));
      fireNotification(exception);
      logException(exception, request);
      processStatistics();

      markExceptionAsHandledIfRequired(exception);
      return beforeRouting(exception, request);
    }

    @Override
    protected Event processResponse(Event response, Event request) throws MuleException {
      processOutboundRouterStatistics();
      response = afterRouting(exception, response);
      if (response != null) {
        response = processReplyTo(response, exception);
        closeStream(response.getMessage());
        return nullifyExceptionPayloadIfRequired(response);
      }
      return response;
    }

    @Override
    protected Event processNext(Event event) throws MuleException {
      return route(event, exception);
    }

    @Override
    protected Event processCatch(Event event, MessagingException exception) throws MessagingException {
      try {
        logger.error("Exception during exception strategy execution");
        doLogException(exception);
        TransactionCoordination.getInstance().rollbackCurrentTransaction();
      } catch (Exception ex) {
        // Do nothing
        logger.warn(ex.getMessage());
      }

      throw exception;
    }

    @Override
    protected void processFinally(Event event, MessagingException exception) {
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

  protected Event processReplyTo(Event event, Exception e) {
    try {
      return replyToMessageProcessor.process(event);
    } catch (MuleException ex) {
      logFatal(event, ex);
      return event;
    }
  }

  protected Event nullifyExceptionPayloadIfRequired(Event event) {
    if (this.handleException) {
      return Event.builder(event).error(null).message(InternalMessage.builder(event.getMessage()).exceptionPayload(null).build())
          .build();
    } else {
      return event;
    }
  }

  private void processStatistics() {
    FlowConstructStatistics statistics = flowConstruct.getStatistics();
    if (statistics != null && statistics.isEnabled()) {
      statistics.incExecutionError();
    }
  }

  protected Event route(Event event, MessagingException t) throws MessagingException {
    if (!getMessageProcessors().isEmpty()) {
      try {
        event = Event.builder(event)
            .message(InternalMessage.builder(event.getMessage()).exceptionPayload(new DefaultExceptionPayload(t)).build())
            .build();
        Event result = configuredMessageProcessors.process(event);
        return result;
      } catch (MessagingException e) {
        throw e;
      } catch (Exception e) {
        throw new MessagingException(event, e);
      }
    }
    return event;
  }

  @Override
  protected void doInitialise(MuleContext muleContext) throws InitialisationException {
    super.doInitialise(muleContext);
    configuredMessageProcessors = newChain(getMessageProcessors());
    configuredMessageProcessors.setFlowConstruct(flowConstruct);
    configuredMessageProcessors.setMuleContext(muleContext);
  }

  public void setWhen(String when) {
    this.when = when;
  }


  @Override
  public boolean accept(Event event) {
    return acceptsAll() || acceptsErrorType(event)
        || (when != null && muleContext.getExpressionLanguage().evaluateBoolean(when, event, flowConstruct));
  }


  private boolean acceptsErrorType(Event event) {
    return errorTypeMatcher != null && errorTypeMatcher.match(event.getError().get().getErrorType());
  }

  @Override
  public boolean acceptsAll() {
    return errorTypeMatcher == null && when == null;
  }

  protected Event afterRouting(MessagingException exception, Event event) {
    return event;
  }

  protected Event beforeRouting(MessagingException exception, Event event) {
    return event;
  }

  @Override
  public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
    return;
  }

  public void setHandleException(boolean handleException) {
    this.handleException = handleException;
  }

  public void setErrorTypeMatcher(ErrorTypeMatcher errorTypeMatcher) {
    this.errorTypeMatcher = errorTypeMatcher;
  }

}
