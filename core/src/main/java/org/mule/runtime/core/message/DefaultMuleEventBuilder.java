/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;


import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MessageContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.session.DefaultMuleSession;

import java.util.HashMap;
import java.util.Map;

public class DefaultMuleEventBuilder implements MuleEvent.Builder {


  private MessageContext context;
  private MuleMessage message;
  private Map<String, TypedValue<Object>> flowVariables = new HashMap<>();
  private MessageExchangePattern exchangePattern;
  private FlowConstruct flow;
  private ReplyToHandler replyToHandler;

  public DefaultMuleEventBuilder(MessageContext messageContext) {
    this.context = messageContext;
  }

  public DefaultMuleEventBuilder(MuleEvent event) {
    this.context = event.getContext();
    this.message = event.getMessage();
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
  public MuleEvent build() {
    MuleEvent event =
        new DefaultMuleEvent(context, message, null, null, exchangePattern, flow, new DefaultMuleSession(), false, null,
                             replyToHandler);
    this.flowVariables.forEach((s, value) -> event.setFlowVariable(s, value.getValue(), value.getDataType()));
    return event;
  }
}
