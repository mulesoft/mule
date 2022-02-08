/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.internal.event.DefaultEventBuilder;
import org.mule.runtime.core.privileged.connector.ReplyToHandler;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.event.context.FlowProcessMediatorContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Contains accessors to the fields of the event only accessible from within the Mule Runtime.
 *
 * @since 4.0
 */
@NoImplement
public interface InternalEvent extends PrivilegedEvent {

  /**
   * Internal parameters used by the runtime to pass information around.
   * 
   * @deprecated since 4.3.0. Replace with {@link EventInternalContext} attributes
   */
  @Deprecated
  Map<String, ?> getInternalParameters();

  /**
   * Internal parameters used by the runtime to pass information around.
   * 
   * @deprecated since 4.3.0. Replace with {@link EventInternalContext} attributes
   */
  @Deprecated
  <T> T getInternalParameter(String key);

  /**
   * @return a {@link FlowProcessMediatorContext} with state from source-flow dispatch
   * @since 4.4, 4.3.1
   */
  FlowProcessMediatorContext getFlowProcessMediatorContext();

  /**
   * Sets context related to the source-flow dispatch
   *
   * @return a {@link FlowProcessMediatorContext} with state from source-flow dispatch
   * @param flowProcessMediatorContext an {@link FlowProcessMediatorContext}
   * @since 4.4, 4.3.1
   */
  void setFlowProcessMediatorContext(FlowProcessMediatorContext flowProcessMediatorContext);

  /**
   * @return a {@link EventInternalContext} with state from the SDK
   * @since 4.3.0
   */
  <T extends EventInternalContext> EventInternalContext<T> getSdkInternalContext();

  /**
   * Sets context related to the SDK
   * 
   * @param context an {@link EventInternalContext}
   * @since 4.3.0
   */
  <T extends EventInternalContext> void setSdkInternalContext(EventInternalContext<T> context);

  /**
   * @return a {@link EventInternalContext} with state from the foreach processor
   * @since 4.4, 4.3.1
   */
  <T extends EventInternalContext> EventInternalContext<T> getForeachInternalContext();

  /**
   * Sets context related to the foreach processor
   * 
   * @param context an {@link EventInternalContext}
   * @since 4.4, 4.3.1
   */
  <T extends EventInternalContext> void setForeachInternalContext(EventInternalContext<T> context);

  /**
   * @return a {@link EventInternalContext} with state from the policy infrastructure relative to sources
   * @since 4.3.0
   */
  <T extends EventInternalContext> EventInternalContext<T> getSourcePolicyContext();

  /**
   * Sets context related to the policy infrastructure relative to sources
   *
   * @param context an {@link EventInternalContext}
   * @since 4.3.0
   */
  <T extends EventInternalContext> void setSourcePolicyContext(EventInternalContext<T> context);

  /**
   * @return a {@link EventInternalContext} with state from the policy infrastructure relative to operations
   * @since 4.3.0
   */
  <T extends EventInternalContext> EventInternalContext<T> getOperationPolicyContext();

  /**
   * Sets context related to the policy infrastructure relative to operations
   *
   * @param context an {@link EventInternalContext}
   * @since 4.3.0
   */
  <T extends EventInternalContext> void setOperationPolicyContext(EventInternalContext<T> context);

  List<Map<String, TypedValue<?>>> getParametersStack();

  /**
   * Create new {@link Builder} based on an existing {@link CoreEvent} instance. The existing {@link EventContext} is conserved.
   *
   * @param event existing event to use as a template to create builder instance
   * @return new builder instance.
   */
  static Builder builder(CoreEvent event) {
    return new DefaultEventBuilder((InternalEvent) event);
  }

  /**
   * Create new {@link Builder}.
   *
   * @param context the context to create event instance with.
   * @return new builder instance.
   */
  static Builder builder(EventContext context) {
    return new DefaultEventBuilder((BaseEventContext) context);
  }

  @NoImplement
  interface Builder extends PrivilegedEvent.Builder {

    /**
     * Set a map of parameters to be internal by the runtime to pass information within the context of an event
     *
     * @param internalParameters parameters to be set.
     * @return the builder instance
     */
    Builder internalParameters(Map<String, ?> internalParameters);

    /**
     * Adds an internal parameter.
     *
     * @param key   the parameter key
     * @param value the parameter value
     * @return the builder instance
     */
    Builder addInternalParameter(String key, Object value);

    /**
     * Remove a internal parameter.
     *
     * @param key the parameter key.
     * @return the builder instance
     */
    Builder removeInternalParameter(String key);

    /**
     * Build a new {@link InternalEvent} based on the state configured in the {@link Builder}.
     *
     * @return new {@link InternalEvent} instance.
     */
    @Override
    InternalEvent build();

    /**
     * Set correlationId overriding the correlationId from {@link EventContext#getCorrelationId()} that came from the source
     * system or that was configured in the connector source. This is only used to support transports and should not be used
     * otherwise.
     *
     * @param correlationId to override existing correlationId
     * @return the builder instance
     * @deprecated Transport infrastructure is deprecated.
     */
    @Override
    @Deprecated
    Builder correlationId(String correlationId);

    /**
     *
     * @param replyToHandler
     * @return the builder instance
     * @deprecated TODO MULE-10739 Move ReplyToHandler to compatibility module.
     */
    @Override
    @Deprecated
    Builder replyToHandler(ReplyToHandler replyToHandler);

    /**
     *
     * @param replyToDestination
     * @return the builder instance
     * @deprecated TODO MULE-10739 Move ReplyToHandler to compatibility module.
     */
    @Override
    @Deprecated
    Builder replyToDestination(Object replyToDestination);

    /**
     * Disables the firing of notifications when processing the produced event.
     *
     * @deprecated Transport infrastructure is deprecated.
     */
    @Override
    @Deprecated
    Builder disableNotifications();

    /**
     * @param session
     * @return the builder instance
     * @deprecated Transport infrastructure is deprecated.
     */
    @Override
    @Deprecated
    Builder session(MuleSession session);

    @Override
    Builder message(Message message);

    @Override
    Builder message(Function<EventContext, Message> messageFactory);

    @Override
    Builder variables(Map<String, ?> variables);

    @Override
    Builder addVariable(String key, Object value);

    @Override
    Builder addVariable(String key, Object value, DataType mediaType);

    @Override
    Builder removeVariable(String key);

    @Override
    Builder clearVariables();

    @Override
    @Deprecated
    Builder groupCorrelation(Optional<GroupCorrelation> groupCorrelation);

    @Override
    Builder error(org.mule.runtime.api.message.Error error);

    @Override
    Builder securityContext(SecurityContext securityContext);

  }
}
