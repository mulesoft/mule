/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.addEventBindings;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.internal.message.EventInternalContext;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.connector.ReplyToHandler;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.event.context.FlowProcessMediatorContext;
import org.mule.runtime.core.privileged.store.DeserializationPostInitialisable;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

abstract class BaseEventDecorator implements InternalEvent, DeserializationPostInitialisable {

  private static final long serialVersionUID = 2264829044803742047L;

  private final InternalEvent event;

  private transient LazyValue<BindingContext> bindingContextBuilder =
      new LazyValue<>(() -> addEventBindings(this, NULL_BINDING_CONTEXT));

  public BaseEventDecorator(InternalEvent event) {
    this.event = event;
  }

  protected InternalEvent getEvent() {
    return event;
  }

  @Override
  public BaseEventContext getContext() {
    return event.getContext();
  }

  @Override
  public MuleSession getSession() {
    return event.getSession();
  }

  @Override
  public ReplyToHandler getReplyToHandler() {
    return event.getReplyToHandler();
  }

  @Override
  public Object getReplyToDestination() {
    return event.getReplyToDestination();
  }

  @Override
  public byte[] getMessageAsBytes(MuleContext muleContext) throws MuleException {
    return event.getMessageAsBytes(muleContext);
  }

  @Override
  public Object transformMessage(DataType outputType, MuleContext muleContext) throws MessageTransformerException {
    return event.transformMessage(outputType, muleContext);
  }

  @Override
  public String getMessageAsString(MuleContext muleContext) throws MuleException {
    return event.getMessageAsString(muleContext);
  }

  @Override
  public String getMessageAsString(Charset encoding, MuleContext muleContext) throws MuleException {
    return event.getMessageAsString(encoding, muleContext);
  }

  @Override
  public boolean isNotificationsEnabled() {
    return event.isNotificationsEnabled();
  }

  @Override
  public SecurityContext getSecurityContext() {
    return event.getSecurityContext();
  }

  @Override
  public Optional<GroupCorrelation> getGroupCorrelation() {
    return event.getGroupCorrelation();
  }

  @Override
  public FlowCallStack getFlowCallStack() {
    return event.getFlowCallStack();
  }

  @Override
  public Map<String, TypedValue<?>> getVariables() {
    return event.getVariables();
  }

  @Override
  public List<Map<String, TypedValue<?>>> getParametersStack() {
    return event.getParametersStack();
  }

  @Override
  public Map<String, TypedValue<?>> getParameters() {
    return event.getParameters();
  }

  @Override
  public Optional<Map<String, String>> getLoggingVariables() {
    return event.getLoggingVariables();
  }

  @Override
  public Message getMessage() {
    return event.getMessage();
  }

  @Override
  public Optional<Authentication> getAuthentication() {
    return event.getAuthentication();
  }

  @Override
  public Optional<Error> getError() {
    return event.getError();
  }

  @Override
  public Optional<ItemSequenceInfo> getItemSequenceInfo() {
    return event.getItemSequenceInfo();
  }

  @Override
  public String getCorrelationId() {
    return getLegacyCorrelationId() != null ? getLegacyCorrelationId() : getContext().getCorrelationId();
  }

  @Override
  public String getLegacyCorrelationId() {
    return event.getLegacyCorrelationId();
  }

  @Override
  public BindingContext asBindingContext() {
    return bindingContextBuilder.get();
  }

  @Override
  public Map<String, ?> getInternalParameters() {
    return event.getInternalParameters();
  }

  @Override
  public <T> T getInternalParameter(String key) {
    return event.getInternalParameter(key);
  }

  @Override
  public FlowProcessMediatorContext getFlowProcessMediatorContext() {
    return event.getFlowProcessMediatorContext();
  }

  @Override
  public void setFlowProcessMediatorContext(FlowProcessMediatorContext flowProcessMediatorContext) {
    event.setFlowProcessMediatorContext(flowProcessMediatorContext);
  }

  @Override
  public <T extends EventInternalContext> EventInternalContext<T> getSdkInternalContext() {
    return event.getSdkInternalContext();
  }

  @Override
  public <T extends EventInternalContext> void setSdkInternalContext(EventInternalContext<T> context) {
    event.setSdkInternalContext(context);
  }

  @Override
  public <T extends EventInternalContext> EventInternalContext<T> getForeachInternalContext() {
    return event.getForeachInternalContext();
  }

  @Override
  public <T extends EventInternalContext> void setForeachInternalContext(EventInternalContext<T> context) {
    event.setForeachInternalContext(context);
  }

  @Override
  public <T extends EventInternalContext> EventInternalContext<T> getSourcePolicyContext() {
    return event.getSourcePolicyContext();
  }

  @Override
  public <T extends EventInternalContext> void setSourcePolicyContext(EventInternalContext<T> context) {
    event.setSourcePolicyContext(context);
  }

  @Override
  public <T extends EventInternalContext> EventInternalContext<T> getOperationPolicyContext() {
    return event.getOperationPolicyContext();
  }

  @Override
  public <T extends EventInternalContext> void setOperationPolicyContext(EventInternalContext<T> context) {
    event.setOperationPolicyContext(context);
  }

  /**
   * Invoked after deserialization. This is called when the marker interface {@link DeserializationPostInitialisable} is used.
   * This will get invoked after the object has been deserialized passing in the current MuleContext.
   *
   * @param muleContext the current muleContext instance
   * @throws MuleException if there is an error initializing
   */
  @SuppressWarnings({"unused"})
  private void initAfterDeserialisation(MuleContext muleContext) throws MuleException {
    bindingContextBuilder = new LazyValue<>(() -> addEventBindings(this, NULL_BINDING_CONTEXT));
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "->" + event.toString();
  }
}
