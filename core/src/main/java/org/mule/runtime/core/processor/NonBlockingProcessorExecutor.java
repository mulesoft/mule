/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.NonBlockingSupported;
import org.mule.runtime.core.api.connector.NonBlockingReplyToHandler;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;
import org.mule.runtime.core.util.OneTimeWarning;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialized {@link org.mule.runtime.core.processor.BlockingProcessorExecutor} that pauses iteration in the case a
 * {@link org .mule.processor.NonBlockingMessageProcessor} is invoked and the flow is executing using non-blocking. Processor
 * execution is then continued when the {@link org.mule.runtime.core.processor.NonBlockingMessageProcessor} invokes the
 * {@link org.mule.runtime.core.api.connector.ReplyToHandler}.
 */
public class NonBlockingProcessorExecutor extends BlockingProcessorExecutor {

  private static final Logger logger = LoggerFactory.getLogger(NonBlockingProcessorExecutor.class);
  private final ReplyToHandler replyToHandler;
  private final MessageExchangePattern messageExchangePattern;
  private FlowConstruct flowConstruct;

  final OneTimeWarning fallbackWarning =
      new OneTimeWarning(logger, "The message processor {} does not currently support non-blocking execution and " +
          "processing will now fall back to blocking.  The 'non-blocking' processing strategy is " +
          "not recommended if unsupported message processors are being used.  ");


  public NonBlockingProcessorExecutor(MuleEvent event, List<MessageProcessor> processors,
                                      MessageProcessorExecutionTemplate executionTemplate, boolean copyOnVoidEvent,
                                      FlowConstruct flowConstruct) {
    super(event, processors, executionTemplate, copyOnVoidEvent);
    this.replyToHandler = event.getReplyToHandler();
    this.messageExchangePattern = event.getExchangePattern();
    this.flowConstruct = flowConstruct;
  }

  @Override
  protected void preProcess(MessageProcessor processor) {
    if (event.isAllowNonBlocking()) {
      if (!processorSupportsNonBlocking(processor)) {
        fallbackWarning.warn(processor.getClass());
        // Make event synchronous so that non-blocking is not used
        event = MuleEvent.builder(event).flow(flowConstruct).synchronous(true).build();
        // Update RequestContext ThreadLocal for backwards compatibility
        setCurrentEvent(event);
      }

      if (processor instanceof NonBlockingMessageProcessor) {
        // Even if there is no ReplyToHandler available, using non-blocking processing anyway for a non-blocking
        // message processor if a response isn't required.
        if (!(messageExchangePattern.hasResponse() && replyToHandler == null)) {
          event = MuleEvent.builder(event).replyToHandler(new NonBlockingProcessorExecutorReplyToHandler()).build();
          // Update RequestContext ThreadLocal for backwards compatibility
          setCurrentEvent(event);
        }
      }
    }
  }

  private boolean processorSupportsNonBlocking(MessageProcessor processor) {
    if (processor instanceof NonBlockingSupported) {
      return true;
    } else if (processor instanceof AsyncInterceptingMessageProcessor && !messageExchangePattern.hasResponse()) {
      return true;
    } else {
      return !(processor instanceof MessageProcessorContainer || processor instanceof InterceptingMessageProcessor);
    }
  }

  private MuleEvent resume(final MuleEvent event) throws MuleException {
    this.event = recreateEventWithOriginalReplyToHandler(event);

    MuleEvent result = execute();
    if (!(result instanceof NonBlockingVoidMuleEvent) && replyToHandler != null) {
      result = replyToHandler.processReplyTo(result, null, null);
    }
    return result;
  }

  private MuleEvent recreateEventWithOriginalReplyToHandler(MuleEvent event) {
    if (event != null) {
      event = MuleEvent.builder(event).replyToHandler(replyToHandler).build();
      // Update RequestContext ThreadLocal for backwards compatibility
      setCurrentEvent(event);
    }
    return event;
  }

  class NonBlockingProcessorExecutorReplyToHandler implements NonBlockingReplyToHandler {

    @Override
    public MuleEvent processReplyTo(final MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException {
      try {
        return resume(event);
      } catch (Throwable e) {
        if (e instanceof MessagingException) {
          processExceptionReplyTo((MessagingException) e, replyTo);
        } else {
          processExceptionReplyTo(new MessagingException(event, e), replyTo);
        }
        return event;
      }
    }

    @Override
    public void processExceptionReplyTo(MessagingException exception, Object replyTo) {
      if (replyToHandler != null) {
        replyToHandler.processExceptionReplyTo(exception, replyTo);
      } else {
        flowConstruct.getExceptionListener().handleException(exception, exception.getEvent());
      }
    }
  }
}
