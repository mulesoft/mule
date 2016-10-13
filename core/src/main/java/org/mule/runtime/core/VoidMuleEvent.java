/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.GroupCorrelation;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link VoidMuleEvent} represents a void return from a {@link Processor} such as a ONE_WAY
 */
public class VoidMuleEvent implements Event {

  private static final long serialVersionUID = 1418044092304465540L;

  private static final VoidMuleEvent instance = new VoidMuleEvent();

  public static VoidMuleEvent getInstance() {
    return instance;
  }

  protected VoidMuleEvent() {
    super();
  }

  @Override
  public EventContext getContext() {
    throw new UnsupportedOperationException();
  }

  @Override
  public InternalMessage getMessage() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Error> getError() {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] getMessageAsBytes(MuleContext muleContext) throws MuleException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T transformMessage(Class<T> outputType, MuleContext muleContext) throws TransformerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object transformMessage(DataType outputType, MuleContext muleContext) throws TransformerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String transformMessageToString(MuleContext muleContext) throws TransformerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getMessageAsString(MuleContext muleContext) throws MuleException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getMessageAsString(Charset encoding, MuleContext muleContext) throws MuleException {
    throw new UnsupportedOperationException();
  }

  @Override
  public MuleSession getSession() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FlowConstruct getFlowConstruct() {
    throw new UnsupportedOperationException();
  }

  @Override
  public MuleContext getMuleContext() {
    throw new UnsupportedOperationException();
  }

  @Override
  public MessageExchangePattern getExchangePattern() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isTransacted() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ReplyToHandler getReplyToHandler() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object getReplyToDestination() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSynchronous() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> TypedValue<T> getVariable(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<String> getVariableNames() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isNotificationsEnabled() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FlowCallStack getFlowCallStack() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SecurityContext getSecurityContext() {
    throw new UnsupportedOperationException();
  }

  @Override
  public GroupCorrelation getGroupCorrelation() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getCorrelationId() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getLegacyCorrelationId() {
    throw new UnsupportedOperationException();
  }

}
