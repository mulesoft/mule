/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;


import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MessageContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleEvent.Builder;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.processor.ProcessingDescriptor;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.session.DefaultMuleSession;

import java.util.HashMap;
import java.util.Map;

public class DefaultMuleEventBuilder implements MuleEvent.Builder {


  private MessageContext context;
  private MuleMessage message;
  private Map<String, TypedValue<Object>> flowVariables = new HashMap<>();
  private Error error;
  private MessageExchangePattern exchangePattern = REQUEST_RESPONSE;
  private FlowConstruct flow;
  private Correlation correlation = new Correlation(null, null);
  private FlowCallStack flowCallStack = new DefaultFlowCallStack();
  private ReplyToHandler replyToHandler;
  private Object replyToDestination;
  private boolean transacted;
  private Boolean synchronous;
  private boolean nonBlocking;
  private MuleSession session = new DefaultMuleSession();

  public DefaultMuleEventBuilder(MessageContext messageContext) {
    this.context = messageContext;
  }

  public DefaultMuleEventBuilder(MuleEvent event) {
    this.context = event.getContext();
    this.message = event.getMessage();
    this.flow = event.getFlowConstruct();
    this.correlation = event.getCorrelation();

    this.flowCallStack = event.getFlowCallStack().clone();

    this.exchangePattern = event.getExchangePattern();

    this.replyToHandler = event.getReplyToHandler();
    this.replyToDestination = event.getReplyToDestination();
    this.message = event.getMessage();

    this.transacted = event.isTransacted();
    this.nonBlocking = event.isAllowNonBlocking();

    this.session = event.getSession();
    this.error = event.getError();

    event.getFlowVariableNames().forEach(key -> this.flowVariables
        .put(key, new TypedValue<>(event.getFlowVariable(key), event.getFlowVariableDataType(key))));
  }


  @Override
  public MuleEvent.Builder message(MuleMessage message) {
    this.message = message;
    return this;
  }

  @Override
  public MuleEvent.Builder flowVariables(Map<String, Object> flowVariables) {
    flowVariables.forEach((s, o) -> this.flowVariables.put(s, new TypedValue<>(o, DataType.fromObject(o))));
    return this;
  }

  @Override
  public MuleEvent.Builder addFlowVariable(String key, Object value) {
    flowVariables.put(key, new TypedValue<>(value, DataType.fromObject(value)));
    return this;

  }

  @Override
  public MuleEvent.Builder addFlowVariable(String key, Object value, DataType dataType) {
    flowVariables.put(key, new TypedValue<>(value, dataType));
    return this;
  }

  @Override
  public MuleEvent.Builder removeFlowVariable(String key) {
    flowVariables.remove(key);
    return this;
  }

  @Override
  public Builder error(Error error) {
    this.error = error;
    return this;
  }

  @Override
  public Builder synchronous(boolean synchronous) {
    this.synchronous = synchronous;
    return this;
  }

  @Override
  public MuleEvent.Builder exchangePattern(MessageExchangePattern exchangePattern) {
    this.exchangePattern = exchangePattern;
    return this;
  }

  @Override
  public MuleEvent.Builder flow(FlowConstruct flow) {
    this.flow = flow;
    return this;
  }

  @Override
  public MuleEvent.Builder replyToHandler(ReplyToHandler replyToHandler) {
    this.replyToHandler = replyToHandler;
    return this;
  }

  @Override
  public Builder replyToDestination(Object replyToDestination) {
    this.replyToDestination = replyToDestination;
    return this;
  }

  @Override
  public Builder transacted(boolean transacted) {
    this.transacted = transacted;
    return this;
  }

  @Override
  public Builder session(MuleSession session) {
    this.session = session;
    return this;
  }

  @Override
  public MuleEvent build() {
    DefaultMuleEvent event =
        new DefaultMuleEvent(context, message, exchangePattern, flow, session, transacted,
                             synchronous == null ? (resolveEventSynchronicity() && replyToHandler == null) : synchronous,
                             nonBlocking || isFlowConstructNonBlockingProcessingStrategy(),
                             replyToDestination, replyToHandler, flowCallStack);
    this.flowVariables.forEach((s, value) -> event.setFlowVariable(s, value.getValue(), value.getDataType()));
    event.setCorrelation(correlation);
    event.setError(error);
    return event;
  }

  protected boolean resolveEventSynchronicity() {
    return transacted
        || isFlowConstructSynchronous()
        || exchangePattern.hasResponse() && !isFlowConstructNonBlockingProcessingStrategy();
  }

  private boolean isFlowConstructSynchronous() {
    return (flow instanceof ProcessingDescriptor) && ((ProcessingDescriptor) flow)
        .isSynchronous();
  }

  protected boolean isFlowConstructNonBlockingProcessingStrategy() {
    return (flow instanceof Pipeline)
        && ((Pipeline) flow).getProcessingStrategy() instanceof NonBlockingProcessingStrategy;
  }


}
