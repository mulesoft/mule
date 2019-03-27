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
import static org.mule.runtime.core.api.config.i18n.CoreMessages.cannotReadPayloadAsBytes;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.cannotReadPayloadAsString;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.core.api.util.CaseInsensitiveHashMap.emptyCaseInsensitiveMap;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.core.internal.util.message.ItemSequenceInfoUtils.fromGroupCorrelation;
import static org.mule.runtime.core.internal.util.message.ItemSequenceInfoUtils.toGroupCorrelation;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.DefaultMuleException;
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
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.util.CaseInsensitiveHashMap;
import org.mule.runtime.core.internal.message.DefaultMessageBuilder;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.message.InternalEvent.Builder;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.connector.ReplyToHandler;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.DefaultMuleSession;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.store.DeserializationPostInitialisable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

public class DefaultEventBuilder implements InternalEvent.Builder {

  private static final Logger logger = LoggerFactory.getLogger(DefaultMessageBuilder.class);

  private BaseEventContext context;
  private Function<EventContext, Message> messageFactory;
  private boolean varsModified = false;
  private final CaseInsensitiveHashMap<String, TypedValue<?>> flowVariables = new CaseInsensitiveHashMap<>();
  private CaseInsensitiveHashMap<String, TypedValue<?>> originalVars;
  private final Map<String, Object> internalParameters;
  private Error error;
  private Optional<ItemSequenceInfo> itemSequenceInfo = empty();
  private String legacyCorrelationId;
  private MuleSession session;
  private SecurityContext securityContext;
  private InternalEvent originalEvent;
  private boolean modified;
  private boolean notificationsEnabled = true;

  public DefaultEventBuilder(BaseEventContext messageContext) {
    this.context = messageContext;
    this.session = new DefaultMuleSession();
    this.originalVars = emptyCaseInsensitiveMap();
    this.internalParameters = new HashMap<>(4);
  }

  public DefaultEventBuilder(InternalEvent event) {
    this.context = event.getContext();
    this.originalEvent = event;
    this.messageFactory = e -> event.getMessage();
    this.itemSequenceInfo = event.getItemSequenceInfo();
    this.legacyCorrelationId = event.getLegacyCorrelationId();
    this.securityContext = event.getSecurityContext();
    this.session = event.getSession();
    this.error = event.getError().orElse(null);
    this.notificationsEnabled = event.isNotificationsEnabled();

    this.originalVars = (CaseInsensitiveHashMap<String, TypedValue<?>>) event.getVariables();
    this.internalParameters = new HashMap<>(event.getInternalParameters());
  }

  public DefaultEventBuilder(BaseEventContext messageContext, InternalEvent event) {
    this(event);
    this.context = messageContext;
    this.modified = true;
  }

  @Override
  public DefaultEventBuilder message(Message message) {
    requireNonNull(message);
    this.messageFactory = e -> message;
    this.modified = true;
    return this;
  }

  @Override
  public Builder message(Function<EventContext, Message> messageFactory) {
    requireNonNull(messageFactory);
    this.messageFactory = messageFactory;
    this.modified = true;
    return this;
  }

  @Override
  public DefaultEventBuilder variables(Map<String, ?> flowVariables) {
    copyFromTo(flowVariables, this.flowVariables);
    this.varsModified = true;

    return this;
  }

  @Override
  public DefaultEventBuilder variablesTyped(Map<String, TypedValue<?>> variables) {
    if (!(variables instanceof CaseInsensitiveHashMap)) {
      return variables(variables);
    }

    if (varsModified) {
      this.flowVariables.clear();
    }

    originalVars = (CaseInsensitiveHashMap<String, TypedValue<?>>) variables;
    this.varsModified = false;
    this.modified = true;

    return this;
  }

  @Override
  public DefaultEventBuilder addVariable(String key, Object value) {
    initVariables();

    flowVariables.put(key, new TypedValue<>(value, DataType.fromObject(value)));
    this.varsModified = true;
    this.modified = true;
    return this;

  }

  @Override
  public DefaultEventBuilder addVariable(String key, Object value, DataType dataType) {
    initVariables();

    flowVariables.put(key, new TypedValue<>(value, dataType));
    this.varsModified = true;
    this.modified = true;
    return this;
  }

  @Override
  public DefaultEventBuilder removeVariable(String key) {
    initVariables();

    this.modified = flowVariables.remove(key) != null || modified;
    this.varsModified = this.varsModified || modified;
    return this;
  }

  @Override
  public Builder clearVariables() {
    if (!this.flowVariables.isEmpty()) {
      this.varsModified = true;
      this.modified = true;
      this.flowVariables.clear();
    }
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
  @Deprecated
  public DefaultEventBuilder groupCorrelation(Optional<GroupCorrelation> correlation) {
    return this.itemSequenceInfo(ofNullable(fromGroupCorrelation(correlation.orElse(null))));
  }

  @Override
  public DefaultEventBuilder itemSequenceInfo(Optional<ItemSequenceInfo> itemSequenceInfo) {
    this.itemSequenceInfo = itemSequenceInfo;
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
    return this;
  }

  @Override
  public DefaultEventBuilder replyToDestination(Object replyToDestination) {
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
      return new InternalEventImplementation(context, requireNonNull(messageFactory.apply(context)),
                                             varsModified ? flowVariables : originalVars,
                                             internalParameters, session, securityContext, itemSequenceInfo, error,
                                             legacyCorrelationId,
                                             notificationsEnabled);
    }
  }

  protected void initVariables() {
    if (!this.varsModified) {
      this.flowVariables.putAll(originalVars);
    }
  }

  private void copyFromTo(Map<String, ?> source, Map<String, TypedValue<?>> target) {
    target.clear();

    for (Entry<String, ?> entry : source.entrySet()) {
      if (entry.getValue() instanceof TypedValue) {
        target.put(entry.getKey(), (TypedValue<?>) entry.getValue());
      } else {
        target.put(entry.getKey(), new TypedValue<>(entry.getValue(), DataType.fromObject(entry.getValue())));
      }
    }
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

    private final BaseEventContext context;
    // TODO MULE-10013 make this final
    private Message message;
    private final MuleSession session;
    private final SecurityContext securityContext;

    private final boolean notificationsEnabled;

    private final CaseInsensitiveHashMap<String, TypedValue<?>> variables;

    private final String legacyCorrelationId;
    private final Error error;

    private final ItemSequenceInfo itemSequenceInfo;
    
    private transient Map<String, ?> internalParameters;
    private transient LazyValue<BindingContext> bindingContextBuilder =
        new LazyValue<>(() -> addEventBindings(this, NULL_BINDING_CONTEXT));

    // Use this constructor from the builder
    private InternalEventImplementation(BaseEventContext context, Message message,
                                        CaseInsensitiveHashMap<String, TypedValue<?>> variables,
                                        Map<String, ?> internalParameters, MuleSession session, SecurityContext securityContext,
                                        Optional<ItemSequenceInfo> itemSequenceInfo,
                                        Error error,
                                        String legacyCorrelationId, boolean notificationsEnabled) {
      this.context = context;
      this.session = session;
      this.securityContext = securityContext;
      this.message = message;
      this.variables = variables.toImmutableCaseInsensitiveMap();
      this.internalParameters = internalParameters;

      this.itemSequenceInfo = itemSequenceInfo.orElse(null);
      this.error = error;
      this.legacyCorrelationId = legacyCorrelationId;

      this.notificationsEnabled = notificationsEnabled;
    }

    private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
      is.defaultReadObject();
      this.internalParameters = new HashMap<>();
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
        throw new DefaultMuleException(cannotReadPayloadAsBytes(message.getPayload().getValue()
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
        throw new DefaultMuleException(cannotReadPayloadAsString(message.getClass().getName()), e);
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

      bindingContextBuilder = new LazyValue<>(() -> addEventBindings(this, NULL_BINDING_CONTEXT));
    }

    @Override
    public ReplyToHandler getReplyToHandler() {
      return null;
    }

    @Override
    public Object getReplyToDestination() {
      return null;
    }

    private void setMessage(Message message) {
      this.message = message;
    }

    @Override
    public Map<String, TypedValue<?>> getVariables() {
      return variables;
    }

    @Override
    public boolean isNotificationsEnabled() {
      return notificationsEnabled;
    }

    @Override
    public FlowCallStack getFlowCallStack() {
      return context.getFlowCallStack();
    }

    @Override
    public SecurityContext getSecurityContext() {
      return securityContext;
    }

    @Override
    public Map<String, ?> getInternalParameters() {
      return unmodifiableMap(internalParameters);
    }

    @Override
    public <T> T getInternalParameter(String key) {
      return (T) internalParameters.get(key);
    }

    @Override
    public Optional<GroupCorrelation> getGroupCorrelation() {
      return ofNullable(toGroupCorrelation(itemSequenceInfo));
    }

    @Override
    public Optional<ItemSequenceInfo> getItemSequenceInfo() {
      return ofNullable(itemSequenceInfo);
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
      return bindingContextBuilder.get();
    }
  }

}
