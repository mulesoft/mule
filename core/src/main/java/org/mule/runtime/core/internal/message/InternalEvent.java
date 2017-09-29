/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.internal.event.DefaultEventBuilder;
import org.mule.runtime.core.privileged.connector.ReplyToHandler;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import java.util.Map;
import java.util.Optional;

/**
 * Contains accessors to the fields of the event only accessible from within the Mule Runtime.
 *
 * @since 4.0
 */
public interface InternalEvent extends PrivilegedEvent {

  /**
   * Internal parameters used by the runtime to pass information around.
   *
   */
  Map<String, ?> getInternalParameters();

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

  public interface Builder extends PrivilegedEvent.Builder {

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
     * @param key the parameter key
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
    Builder variables(Map<String, ?> variables);

    @Override
    Builder addVariable(String key, Object value);

    @Override
    Builder addVariable(String key, Object value, DataType mediaType);

    @Override
    Builder removeVariable(String key);

    @Override
    Builder groupCorrelation(Optional<GroupCorrelation> groupCorrelation);

    @Override
    Builder error(org.mule.runtime.api.message.Error error);

    @Override
    Builder securityContext(SecurityContext securityContext);

  }
}
