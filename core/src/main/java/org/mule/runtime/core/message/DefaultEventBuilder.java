/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;


import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.connector.DefaultReplyToHandler;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.internal.message.DefaultMessageBuilder;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.util.CopyOnWriteCaseInsensitiveMap;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultEventBuilder implements Event.Builder {

  private static final Logger logger = LoggerFactory.getLogger(DefaultMessageBuilder.class);

  private EventContext context;
  private Message message;
  private Map<String, TypedValue<Object>> flowVariables = new HashMap<>();
  private Map<String, TypedValue<Object>> moduleProperties = new HashMap<>();
  private Map<String, TypedValue<Object>> moduleParameters = new HashMap<>();
  private Error error;
  private FlowConstruct flow;
  private GroupCorrelation groupCorrelation = new GroupCorrelation(null, null);
  private String legacyCorrelationId;
  private FlowCallStack flowCallStack = new DefaultFlowCallStack();
  private ReplyToHandler replyToHandler;
  private Object replyToDestination;
  private MuleSession session = new DefaultMuleSession();
  private Event originalEvent;
  private boolean modified;
  private boolean notificationsEnabled = true;

  public DefaultEventBuilder(EventContext messageContext) {
    this.context = messageContext;
  }

  public DefaultEventBuilder(Event event) {
    this.context = event.getContext();
    this.originalEvent = event;
    this.message = event.getMessage();
    this.flow = event.getFlowConstruct();
    this.groupCorrelation = event.getGroupCorrelation();
    this.legacyCorrelationId = event.getLegacyCorrelationId();
    this.flowCallStack = event.getFlowCallStack().clone();
    this.replyToHandler = event.getReplyToHandler();
    this.replyToDestination = event.getReplyToDestination();
    this.session = event.getSession();
    this.error = event.getError().orElse(null);
    this.notificationsEnabled = event.isNotificationsEnabled();

    event.getVariableNames().forEach(key -> this.flowVariables.put(key, event.getVariable(key)));
    this.moduleProperties = event.getProperties();
    this.moduleParameters = event.getParameters();
  }

  public DefaultEventBuilder(EventContext messageContext, Event event) {
    this(event);
    this.context = messageContext;
    this.modified = true;
  }

  @Override
  public Event.Builder message(Message message) {
    requireNonNull(message);
    this.message = message;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder variables(Map<String, Object> flowVariables) {
    copyFromTo(flowVariables, this.flowVariables);
    return this;
  }

  @Override
  public Event.Builder addVariable(String key, Object value) {
    flowVariables.put(key, new TypedValue<>(value, DataType.fromObject(value)));
    this.modified = true;
    return this;

  }

  @Override
  public Event.Builder addVariable(String key, Object value, DataType dataType) {
    flowVariables.put(key, new TypedValue<>(value, dataType));
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder removeVariable(String key) {
    flowVariables.remove(key);
    this.modified = true;
    return this;
  }

  @Override
  public Builder properties(Map<String, Object> properties) {
    copyFromTo(properties, this.moduleProperties);
    return this;
  }

  @Override
  public Builder parameters(Map<String, Object> parameters) {
    copyFromTo(parameters, this.moduleParameters);
    return this;
  }

  @Override
  public Event.Builder correlationId(String correlationId) {
    legacyCorrelationId = correlationId;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder groupCorrelation(GroupCorrelation correlation) {
    this.groupCorrelation = correlation;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder error(Error error) {
    this.error = error;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder flow(FlowConstruct flow) {
    this.flow = flow;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder replyToHandler(ReplyToHandler replyToHandler) {
    this.replyToHandler = replyToHandler;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder replyToDestination(Object replyToDestination) {
    this.replyToDestination = replyToDestination;
    this.modified = true;
    return this;
  }

  @Override
  public Event.Builder session(MuleSession session) {
    this.session = session;
    this.modified = true;
    return this;
  }

  @Override
  public Builder disableNotifications() {
    this.notificationsEnabled = false;
    this.modified = true;
    return this;
  }

  @Override
  public Event build() {
    if (originalEvent != null && !modified) {
      return originalEvent;
    } else {
      requireNonNull(message);

      return new EventImplementation(context, message, flowVariables, moduleProperties, moduleParameters, flow, session,
                                     replyToDestination,
                                     replyToHandler,
                                     flowCallStack, groupCorrelation, error, legacyCorrelationId, notificationsEnabled);
    }
  }

  private void copyFromTo(Map<String, Object> source, Map<String, TypedValue<Object>> target) {
    target.clear();
    source.forEach((s, o) -> target.put(s, new TypedValue<>(o, DataType.fromObject(o))));
    this.modified = true;
  }

  /**
   * <code>EventImplementation</code> represents any data event occurring in the Mule environment. All data sent or received
   * within the Mule environment will be passed between components as an MuleEvent.
   * <p>
   * The {@link Event} holds some data and provides helper methods for obtaining the data in a format that the receiving Mule
   * component understands. The event can also maintain any number of flowVariables that can be set and retrieved by Mule
   * components.
   */
  public static class EventImplementation implements Event, DeserializationPostInitialisable {

    private static final long serialVersionUID = 1L;

    /** Immutable MuleEvent state **/

    private EventContext context;
    // TODO MULE-10013 make this final
    private Message message;
    private final MuleSession session;
    // TODO MULE-10013 make this final
    private transient FlowConstruct flowConstruct;

    private final ReplyToHandler replyToHandler;

    /** Mutable MuleEvent state **/
    private final Object replyToDestination;

    private final boolean notificationsEnabled;

    private final CopyOnWriteCaseInsensitiveMap<String, TypedValue> variables = new CopyOnWriteCaseInsensitiveMap<>();
    private final Map<String, TypedValue<Object>> properties;
    private final Map<String, TypedValue<Object>> parameters;

    private FlowCallStack flowCallStack = new DefaultFlowCallStack();
    private final String legacyCorrelationId;
    private final Error error;

    // Used in deserialization to obtain instance of flowConstruct via registry lookup.
    private String flowName;

    // Use this constructor from the builder
    private EventImplementation(EventContext context, Message message, Map<String, TypedValue<Object>> variables,
                                Map<String, TypedValue<Object>> properties, Map<String, TypedValue<Object>> parameters,
                                FlowConstruct flowConstruct, MuleSession session,
                                Object replyToDestination, ReplyToHandler replyToHandler,
                                FlowCallStack flowCallStack, GroupCorrelation groupCorrelation, Error error,
                                String legacyCorrelationId, boolean notificationsEnabled) {
      this.context = context;
      this.flowConstruct = flowConstruct;
      if (flowConstruct != null) {
        this.flowName = flowConstruct.getName();
      }
      this.session = session;
      this.message = message;
      variables.forEach((s, value) -> this.variables.put(s, new TypedValue<>(value.getValue(), value.getDataType())));
      this.properties = properties;
      this.parameters = parameters;

      this.replyToHandler = replyToHandler;
      this.replyToDestination = replyToDestination;

      this.flowCallStack = flowCallStack;

      this.groupCorrelation = groupCorrelation;
      this.error = error;
      this.legacyCorrelationId = legacyCorrelationId;

      this.notificationsEnabled = notificationsEnabled;
    }

    @Override
    public EventContext getContext() {
      return context;
    }

    @Override
    public Message getMessage() {
      return message;
    }

    @Override
    public Optional<Error> getError() {
      return ofNullable(error);
    }

    @Override
    public byte[] getMessageAsBytes(MuleContext muleContext) throws DefaultMuleException {
      try {
        return (byte[]) transformMessage(DataType.BYTE_ARRAY, muleContext);
      } catch (Exception e) {
        throw new DefaultMuleException(CoreMessages.cannotReadPayloadAsBytes(message.getPayload().getValue()
            .getClass()
            .getName()), e);
      }
    }

    @Override
    public <T> T transformMessage(Class<T> outputType, MuleContext muleContext) throws TransformerException {
      return (T) transformMessage(DataType.fromType(outputType), muleContext);
    }

    @Override
    public Object transformMessage(DataType outputType, MuleContext muleContext) throws TransformerException {
      if (outputType == null) {
        throw new TransformerException(CoreMessages.objectIsNull("outputType"));
      }

      Message transformedMessage = muleContext.getTransformationService().transform(message, outputType);
      if (message.getPayload().getDataType().isStreamType()) {
        setMessage(transformedMessage);
      }
      return transformedMessage.getPayload().getValue();
    }

    /**
     * Returns the message transformed into it's recognised or expected format and then into a String. The transformer used is the
     * one configured on the endpoint through which this event was received.
     *
     * @return the message transformed into it's recognised or expected format as a Strings.
     * @throws org.mule.runtime.core.api.transformer.TransformerException if a failure occurs in the transformer
     * @see org.mule.runtime.core.api.transformer.Transformer
     */
    @Override
    public String transformMessageToString(MuleContext muleContext) throws TransformerException {
      final DataType dataType = DataType.builder(getMessage().getPayload().getDataType()).type(String.class).build();
      return (String) transformMessage(dataType, muleContext);
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
      return "DefaultMuleEvent{" +
          "context=" + context +
          ", message=" + message +
          ", variables=" + variables +
          ", error=" + error +
          '}';
    }

    @Override
    public MuleSession getSession() {
      return session;
    }

    @Override
    public FlowConstruct getFlowConstruct() {
      return flowConstruct;
    }

    /**
     * Invoked after deserialization. This is called when the marker interface
     * {@link org.mule.runtime.core.util.store.DeserializationPostInitialisable} is used. This will get invoked after the object
     * has been deserialized passing in the current MuleContext when using either
     * {@link org.mule.runtime.core.transformer.wire.SerializationWireFormat},
     * {@link org.mule.runtime.core.transformer.wire.SerializedMuleMessageWireFormat} or the
     * {@link org.mule.runtime.core.transformer.simple.ByteArrayToSerializable} transformer.
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
      if (flowName != null) {
        flowConstruct = muleContext.getRegistry().lookupFlowConstruct(flowName);
        // If flow construct is a Pipeline and has a EventContext instance cache, then reestablish the event context here with
        // that instance in order to conserve non-serializable subscribers which in turn reference callbacks. Otherwise use the
        // serialized version with no subscribers.
        if (flowConstruct instanceof Pipeline) {
          EventContext cachedValue = ((Pipeline) flowConstruct).getSerializationEventContextCache().remove(context.getId());
          context = cachedValue != null ? cachedValue : context;
        }
      }
    }

    @Override
    public MuleContext getMuleContext() {
      return flowConstruct.getMuleContext();
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
      if (flowName != null && flowConstruct instanceof Pipeline) {
        ((Pipeline) flowConstruct).getSerializationEventContextCache().put(context.getId(), context);
      }
      for (Map.Entry<String, TypedValue> entry : variables.entrySet()) {
        Object value = entry.getValue();
        if (value != null && !(value instanceof Serializable)) {
          String message = String.format(
                                         "Unable to serialize the flow variable %s, which is of type %s ", entry.getKey(), value);
          logger.error(message);
          throw new IOException(message);
        }
      }
    }

    private void setMessage(Message message) {
      this.message = message;
    }

    @Override
    public Set<String> getVariableNames() {
      return variables.keySet();
    }

    @Override
    public <T> TypedValue<T> getVariable(String key) {
      TypedValue<T> typedValue = variables.get(key);

      if (typedValue == null) {
        throw new NoSuchElementException("The flow variable '" + key + "' does not exist.");
      } else {
        return typedValue;
      }
    }

    @Override
    public Map<String, TypedValue<Object>> getProperties() {
      return properties;
    }

    @Override
    public Map<String, TypedValue<Object>> getParameters() {
      return parameters;
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
      return session.getSecurityContext();
    }

    private GroupCorrelation groupCorrelation;

    @Override
    public GroupCorrelation getGroupCorrelation() {
      return groupCorrelation;
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
  }

}
