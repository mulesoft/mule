/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.ProcessorsTrace;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.security.Credentials;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.management.stats.ProcessingTime;

import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * A {@link VoidMuleEvent} represents a void return from a {@link MessageProcessor} such as a ONE_WAY
 */
public class VoidMuleEvent implements MuleEvent {

  private static final long serialVersionUID = 1418044092304465540L;

  private static final VoidMuleEvent instance = new VoidMuleEvent();

  public static VoidMuleEvent getInstance() {
    return instance;
  }

  protected VoidMuleEvent() {
    super();
  }

  @Override
  public MuleMessage getMessage() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Credentials getCredentials() {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] getMessageAsBytes() throws MuleException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T transformMessage(Class<T> outputType) throws TransformerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object transformMessage(DataType outputType) throws TransformerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String transformMessageToString() throws TransformerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getMessageAsString() throws MuleException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getMessageAsString(Charset encoding) throws MuleException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getId() {
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
  public boolean isStopFurtherProcessing() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setStopFurtherProcessing(boolean stopFurtherProcessing) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getTimeout() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setTimeout(int timeout) {
    throw new UnsupportedOperationException();

  }

  @Override
  public OutputStream getOutputStream() {
    throw new UnsupportedOperationException();
  }

  @Override
  public MuleContext getMuleContext() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ProcessingTime getProcessingTime() {
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
  public URI getMessageSourceURI() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getMessageSourceName() {
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
  public void setMessage(MuleMessage message) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T getFlowVariable(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataType getFlowVariableDataType(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setFlowVariable(String key, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setFlowVariable(String key, Object value, DataType dataType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeFlowVariable(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<String> getFlowVariableNames() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clearFlowVariables() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isNotificationsEnabled() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setEnableNotifications(boolean enabled) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isAllowNonBlocking() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FlowCallStack getFlowCallStack() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ProcessorsTrace getProcessorsTrace() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SecurityContext getSecurityContext() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setSecurityContext(SecurityContext context) {
    throw new UnsupportedOperationException();
  }
}
