/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;


import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.addEventBindings;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.util.CaseInsensitiveHashMap;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.internal.message.DefaultMessageBuilder;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.connector.DefaultReplyToHandler;
import org.mule.runtime.core.privileged.connector.ReplyToHandler;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.DefaultMuleSession;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.store.DeserializationPostInitialisable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DefaultEventBuilder implements InternalEvent.Builder {

  private static final Logger logger = LoggerFactory.getLogger(DefaultMessageBuilder.class);

  private BaseEventContext context;
  private Message message;
  private Map<String, TypedValue<?>> flowVariables = new HashMap<>();
  private Map<String, Object> internalParameters = new HashMap<>(4);
  private Error error;
  private Optional<GroupCorrelation> groupCorrelation = empty();
  private String legacyCorrelationId;
  private FlowCallStack flowCallStack;
  private ReplyToHandler replyToHandler;
  private Object replyToDestination;
  private MuleSession session;
  private SecurityContext securityContext;
  private InternalEvent originalEvent;
  private boolean modified;
  private boolean notificationsEnabled = true;

  public DefaultEventBuilder(BaseEventContext messageContext) {
    this.context = messageContext;
    this.flowCallStack = new DefaultFlowCallStack();
    this.session = new DefaultMuleSession();
  }

  public DefaultEventBuilder(InternalEvent event) {
    this.context = event.getContext();
    this.originalEvent = event;
    this.message = event.getMessage();
    this.groupCorrelation = event.getGroupCorrelation();
    this.legacyCorrelationId = event.getLegacyCorrelationId();
    this.flowCallStack = event.getFlowCallStack().clone();
    this.replyToHandler = event.getReplyToHandler();
    this.replyToDestination = event.getReplyToDestination();
    this.securityContext = event.getSecurityContext();
    this.session = event.getSession();
    this.error = event.getError().orElse(null);
    this.notificationsEnabled = event.isNotificationsEnabled();

    this.flowVariables.putAll(event.getVariables());
    this.internalParameters.putAll(event.getInternalParameters());
  }

  public DefaultEventBuilder(BaseEventContext messageContext, InternalEvent event) {
    this(event);
    this.context = messageContext;
    this.modified = true;
  }

  @Override
  public DefaultEventBuilder message(Message message) {
    requireNonNull(message);
    this.message = message;
    this.modified = true;
    return this;
  }

  @Override
  public DefaultEventBuilder variables(Map<String, ?> flowVariables) {
    copyFromTo(flowVariables, this.flowVariables);
    return this;
  }

  @Override
  public DefaultEventBuilder addVariable(String key, Object value) {
    flowVariables.put(key, new TypedValue<>(value, DataType.fromObject(value)));
    this.modified = true;
    return this;

  }

  @Override
  public DefaultEventBuilder addVariable(String key, Object value, DataType dataType) {
    flowVariables.put(key, new TypedValue<>(value, dataType));
    this.modified = true;
    return this;
  }

  @Override
  public DefaultEventBuilder removeVariable(String key) {
    this.modified = flowVariables.remove(key) != null || modified;
    return this;
  }

  @Override
  public DefaultEventBuilder internalParameters(Map<String, ?> internalParameters) {
    this.internalParameters.clear();
    this.internalParameters.putAll(internalParameters);
    this.modified = true;
    return this;
  }

  @Override
  public DefaultEventBuilder addInternalParameter(String key, Object value) {
    internalParameters.put(key, value);
    this.modified = true;
    return this;
  }

  @Override
  public DefaultEventBuilder removeInternalParameter(String key) {
    this.modified = internalParameters.remove(key) != null || modified;
    return this;
  }

  @Override
  public DefaultEventBuilder correlationId(String correlationId) {
    legacyCorrelationId = correlationId;
    this.modified = true;
    return this;
  }

  @Override
  public DefaultEventBuilder groupCorrelation(Optional<GroupCorrelation> correlation) {
    this.groupCorrelation = correlation;
    this.modified = true;
    return this;
  }

  @Override
  public DefaultEventBuilder error(Error error) {
    this.error = error;
    this.modified = true;
    return this;
  }

  @Override
  public DefaultEventBuilder replyToHandler(ReplyToHandler replyToHandler) {
    this.replyToHandler = replyToHandler;
    this.modified = true;
    return this;
  }

  @Override
  public DefaultEventBuilder replyToDestination(Object replyToDestination) {
    this.replyToDestination = replyToDestination;
    this.modified = true;
    return this;
  }

  @Override
  public DefaultEventBuilder session(MuleSession session) {
    this.session = session;
    this.modified = true;
    return this;
  }

  @Override
  public DefaultEventBuilder securityContext(SecurityContext securityContext) {
    SecurityContext originalValue = this.securityContext;
    this.securityContext = securityContext;
    this.modified = originalValue != securityContext;
    return this;
  }

  @Override
  public DefaultEventBuilder disableNotifications() {
    this.notificationsEnabled = false;
    this.modified = true;
    return this;
  }

  @Override
  public InternalEvent build() {
    if (originalEvent != null && !modified) {
      return originalEvent;
    } else {
      requireNonNull(message);

      return new InternalEventImplementation(context, message, flowVariables,
                                             internalParameters, session, securityContext, replyToDestination,
                                             replyToHandler, flowCallStack, groupCorrelation, error, legacyCorrelationId,
                                             notificationsEnabled);
    }
  }

  private void copyFromTo(Map<String, ?> source, Map<String, TypedValue<?>> target) {
    target.clear();
    source.forEach((s, o) -> target
        .put(s, o instanceof TypedValue ? (TypedValue<Object>) o : new TypedValue<>(o, DataType.fromObject(o))));
    this.modified = true;
  }

  /**
   * <code>EventImplementation</code> represents any data event occurring in the Mule environment. All data sent or received
   * within the Mule environment will be passed between components as an MuleEvent.
   * <p>
   * The {@link CoreEvent} holds some data and provides helper methods for obtaining the data in a format that the receiving Mule
   * component understands. The event can also maintain any number of flowVariables that can be set and retrieved by Mule
   * components.
   */
  public static class InternalEventImplementation implements InternalEvent, DeserializationPostInitialisable {

    private static final long serialVersionUID = 1L;

    /** Immutable MuleEvent state **/

    private BaseEventContext context;
    // TODO MULE-10013 make this final
    private Message message;
    private final MuleSession session;
    private SecurityContext securityContext;

    private final ReplyToHandler replyToHandler;

    /** Mutable MuleEvent state **/
    private final Object replyToDestination;

    private final boolean notificationsEnabled;

    private final CaseInsensitiveHashMap<String, TypedValue<?>> variables;
    private final Map<String, ?> internalParameters;

    private FlowCallStack flowCallStack = new DefaultFlowCallStack();
    private final String legacyCorrelationId;
    private final Error error;

    // Use this constructor from the builder
    private InternalEventImplementation(BaseEventContext context, Message message, Map<String, TypedValue<?>> variables,
                                        Map<String, ?> internalParameters, MuleSession session, SecurityContext securityContext,
                                        Object replyToDestination, ReplyToHandler replyToHandler, FlowCallStack flowCallStack,
                                        Optional<GroupCorrelation> groupCorrelation, Error error,
                                        String legacyCorrelationId, boolean notificationsEnabled) {
      this.context = context;
      this.session = session;
      this.securityContext = securityContext;
      this.message = message;
      this.variables = new CaseInsensitiveHashMap<>(variables);
      this.internalParameters = internalParameters;

      this.replyToHandler = replyToHandler;
      this.replyToDestination = replyToDestination;

      this.flowCallStack = flowCallStack;

      this.groupCorrelation = groupCorrelation.orElse(null);
      this.error = error;
      this.legacyCorrelationId = legacyCorrelationId;

      this.notificationsEnabled = notificationsEnabled;
    }

    @Override
    public BaseEventContext getContext() {
      return context;
    }

    @Override
    public Message getMessage() {
      return message;
    }

    @Override
    public Optional<Authentication> getAuthentication() {
      if (securityContext == null) {
        return empty();
      }
      return ofNullable(securityContext.getAuthentication());
    }

    @Override
    public Optional<Error> getError() {
      return ofNullable(error);
    }

    @Override
    public byte[] getMessageAsBytes(MuleContext muleContext) throws MuleException {
      try {
        return (byte[]) transformMessage(DataType.BYTE_ARRAY, muleContext);
      } catch (Exception e) {
        throw new DefaultMuleException(CoreMessages.cannotReadPayloadAsBytes(message.getPayload().getValue()
            .getClass()
            .getName()), e);
      }
    }

    @Override
    public Object transformMessage(DataType outputType, MuleContext muleContext) throws MessageTransformerException {
      if (outputType == null) {
        throw new MessageTransformerException(objectIsNull("outputType"), null, message);
      }

      Message transformedMessage = muleContext.getTransformationService().transform(message, outputType);
      if (message.getPayload().getDataType().isStreamType()) {
        setMessage(transformedMessage);
      }
      return transformedMessage.getPayload().getValue();
    }

    @Override
    public String getMessageAsString(MuleContext muleContext) throws MuleException {
      return getMessageAsString(getMessage().getPayload().getDataType().getMediaType().getCharset()
          .orElse(getDefaultEncoding(muleContext)), muleContext);
    }

    /**
     * Returns the message contents for logging
     *
     * @param encoding the encoding to use when converting bytes to a string, if necessary
     * @param muleContext the Mule node.
     * @return the message contents as a string
     * @throws MuleException if the message cannot be converted into a string
     */
    @Override
    public String getMessageAsString(Charset encoding, MuleContext muleContext) throws MuleException {
      try {
        Message transformedMessage = muleContext.getTransformationService()
            .transform(message, DataType.builder().type(String.class).charset(encoding).build());
        if (message.getPayload().getDataType().isStreamType()) {
          setMessage(transformedMessage);
        }

        return (String) transformedMessage.getPayload().getValue();
      } catch (Exception e) {
        throw new DefaultMuleException(CoreMessages.cannotReadPayloadAsString(message.getClass().getName()), e);
      }
    }

    @Override
    public String toString() {
      return "DefaultMuleEvent{" + lineSeparator() +
          "  context=" + context + lineSeparator() +
          "  message=" + message + lineSeparator() +
          "  variables=" + variables + lineSeparator() +
          "  error=" + error + lineSeparator() +
          '}';
    }

    @Override
    public MuleSession getSession() {
      return session;
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
      // TODO MULE-10013 remove this logic from here
      if (message instanceof InternalMessage) {
        setMessage(message);
      }
      if (replyToHandler instanceof DefaultReplyToHandler) {
        ((DefaultReplyToHandler) replyToHandler).initAfterDeserialisation(muleContext);
      }
      if (replyToDestination instanceof DeserializationPostInitialisable) {
        try {
          DeserializationPostInitialisable.Implementation.init(replyToDestination, muleContext);
        } catch (Exception e) {
          throw new DefaultMuleException(e);
        }

      }
    }

    @Override
    public ReplyToHandler getReplyToHandler() {
      return replyToHandler;
    }

    @Override
    public Object getReplyToDestination() {
      return replyToDestination;
    }

    // //////////////////////////
    // Serialization methods
    // //////////////////////////

    private void writeObject(ObjectOutputStream out) throws IOException {
      // TODO MULE-10013 remove this logic from here
      out.defaultWriteObject();
      for (Map.Entry<String, TypedValue<?>> entry : variables.entrySet()) {
        Object value = entry.getValue();
        if (value != null && !(value instanceof Serializable)) {
          String message = format("Unable to serialize the flow variable %s, which is of type %s ", entry.getKey(), value);
          logger.error(message);
          throw new IOException(message);
        }
      }
    }

    private void setMessage(Message message) {
      this.message = message;
    }

    @Override
    public Map<String, TypedValue<?>> getVariables() {
      return unmodifiableMap(variables);
    }

    @Override
    public boolean isNotificationsEnabled() {
      return notificationsEnabled;
    }

    @Override
    public FlowCallStack getFlowCallStack() {
      return flowCallStack;
    }

    @Override
    public SecurityContext getSecurityContext() {
      return securityContext;
    }

    private GroupCorrelation groupCorrelation;

    @Override
    public Map<String, ?> getInternalParameters() {
      return unmodifiableMap(internalParameters);
    }

    @Override
    public Optional<GroupCorrelation> getGroupCorrelation() {
      return ofNullable(groupCorrelation);
    }

    @Override
    public String getCorrelationId() {
      return legacyCorrelationId != null ? legacyCorrelationId : getContext().getCorrelationId();
    }

    /**
     * Obtain the correlationId set during flow execution if any. This is only used to support transports and should not be used
     * otherwise. Customization of the correlationId, if needed, should instead be done as part of the source connector
     * configuration.
     *
     * @return legacy correlationId if set, otherwise {@code null}.
     * @deprecated Transport infrastructure is deprecated.
     */
    @Override
    @Deprecated
    public String getLegacyCorrelationId() {
      return this.legacyCorrelationId;
    }

    @Override
    public BindingContext asBindingContext() {
      return addEventBindings(this, NULL_BINDING_CONTEXT);
    }
  }

}
