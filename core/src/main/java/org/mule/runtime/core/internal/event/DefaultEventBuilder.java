/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.addEventBindings;
import static org.mule.runtime.api.util.collection.SmallMap.copy;
import static org.mule.runtime.api.util.collection.SmallMap.unmodifiable;
import static org.mule.runtime.core.api.util.CaseInsensitiveHashMap.basedOn;
import static org.mule.runtime.core.api.util.CaseInsensitiveHashMap.emptyCaseInsensitiveMap;
import static org.mule.runtime.core.internal.message.EventInternalContext.copyOf;
import static org.mule.runtime.core.internal.util.message.ItemSequenceInfoUtils.fromGroupCorrelation;
import static org.mule.runtime.core.internal.util.message.ItemSequenceInfoUtils.toGroupCorrelation;

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.api.util.CaseInsensitiveHashMap;
import org.mule.runtime.core.internal.event.InternalEvent.Builder;
import org.mule.runtime.core.internal.message.EventInternalContext;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.event.context.FlowProcessMediatorContext;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.MDC;

public class DefaultEventBuilder implements InternalEvent.Builder {

  private BaseEventContext context;
  private Function<EventContext, Message> messageFactory;

  private CaseInsensitiveHashMap<String, TypedValue<?>> flowVariables;
  private CaseInsensitiveHashMap<String, TypedValue<?>> originalVars;
  private boolean varsModified = false;

  private CaseInsensitiveHashMap<String, TypedValue<?>> parameters;
  private final CaseInsensitiveHashMap<String, TypedValue<?>> originalParameters;
  private boolean parametersModified = false;

  private CaseInsensitiveHashMap<String, String> loggingVariables;

  private Map<String, Object> internalParameters;
  private Error error;
  private Optional<ItemSequenceInfo> itemSequenceInfo = empty();
  private String legacyCorrelationId;
  private SecurityContext securityContext;
  private FlowProcessMediatorContext flowProcessMediatorContext;
  private EventInternalContext sdkInternalContext;
  private EventInternalContext foreachInternalContext;
  private EventInternalContext sourcePolicyContext;
  private EventInternalContext operationPolicyContext;
  private InternalEvent originalEvent;
  private boolean modified;
  private boolean internalParametersInitialized = false;
  private boolean notificationsEnabled = true;

  public DefaultEventBuilder(BaseEventContext messageContext) {
    this.context = messageContext;
    this.originalVars = emptyCaseInsensitiveMap();
    this.originalParameters = emptyCaseInsensitiveMap();
    this.internalParameters = new SmallMap<>();
    internalParametersInitialized = true;
  }

  public DefaultEventBuilder(InternalEvent event) {
    this.context = event.getContext();
    this.originalEvent = event;
    this.messageFactory = e -> event.getMessage();
    this.itemSequenceInfo = event.getItemSequenceInfo();
    this.legacyCorrelationId = event.getLegacyCorrelationId();
    this.securityContext = event.getSecurityContext();
    this.error = event.getError().orElse(null);
    this.notificationsEnabled = event.isNotificationsEnabled();

    this.originalVars = (CaseInsensitiveHashMap<String, TypedValue<?>>) event.getVariables();
    originalParameters = (CaseInsensitiveHashMap<String, TypedValue<?>>) event.getParameters();

    this.loggingVariables = (CaseInsensitiveHashMap<String, String>) event.getLoggingVariables().orElse(null);
    if (loggingVariables != null) {
      CaseInsensitiveHashMap<String, String> loggingVariablesCopyMap = new CaseInsensitiveHashMap<>();
      loggingVariablesCopyMap.putAll(loggingVariables);
      this.loggingVariables = loggingVariablesCopyMap;
    }
    this.internalParameters = (Map<String, Object>) event.getInternalParameters();
    flowProcessMediatorContext = copyOf(event.getFlowProcessMediatorContext());
    foreachInternalContext = copyOf(event.getForeachInternalContext());
    sdkInternalContext = copyOf(event.getSdkInternalContext());
    sourcePolicyContext = copyOf(event.getSourcePolicyContext());
    operationPolicyContext = copyOf(event.getOperationPolicyContext());
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
    initVariables();

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
    if (value instanceof TypedValue) {
      return (DefaultEventBuilder) addVariable(key, (TypedValue) value);
    }

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
  public CoreEvent.Builder addVariable(String key, TypedValue<?> value) {
    initVariables();

    flowVariables.put(key, value);
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
  public CoreEvent.Builder parameters(Map<String, ?> parameters) {
    this.parameters = new CaseInsensitiveHashMap<>();
    copyFromTo(parameters, this.parameters);

    parametersModified = true;
    return this;
  }

  @Override
  public CoreEvent.Builder clearParameters() {
    parameters = emptyCaseInsensitiveMap();
    modified = parametersModified = true;

    return this;
  }

  @Override
  public PrivilegedEvent.Builder addLoggingVariable(String key, String value) {
    if (loggingVariables == null) {
      loggingVariables = new CaseInsensitiveHashMap<>();
    }
    loggingVariables.put(key, value);
    modified = true;
    return this;
  }

  @Override
  public PrivilegedEvent.Builder removeLoggingVariable(String key) {
    if (loggingVariables == null) {
      return this;
    }
    if (loggingVariables.remove(key) == null) {
      return this;
    }
    MDC.remove(key);
    modified = true;
    return this;
  }

  @Override
  public PrivilegedEvent.Builder clearLoggingVariables() {
    if (loggingVariables == null) {
      return this;
    }
    if (loggingVariables.isEmpty()) {
      return this;
    }
    loggingVariables.forEach((k, v) -> MDC.remove(k));
    loggingVariables.clear();
    modified = true;
    return this;
  }

  @Override
  public Builder clearVariables() {
    if ((flowVariables != null && !this.flowVariables.isEmpty()) || !this.originalVars.isEmpty()) {
      this.varsModified = true;
      this.modified = true;
      flowVariables = basedOn(new SmallMap<>());
    }
    return this;
  }

  @Override
  public DefaultEventBuilder internalParameters(Map<String, ?> internalParameters) {
    initInternalParameters();

    this.internalParameters.clear();
    this.internalParameters.putAll(internalParameters);
    this.modified = true;
    return this;
  }

  @Override
  public DefaultEventBuilder addInternalParameter(String key, Object value) {
    initInternalParameters();

    internalParameters.put(key, value);
    this.modified = true;
    return this;
  }

  @Override
  public DefaultEventBuilder removeInternalParameter(String key) {
    initInternalParameters();

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
  public DefaultEventBuilder securityContext(SecurityContext securityContext) {
    SecurityContext originalValue = this.securityContext;
    this.securityContext = securityContext;
    this.modified = modified || originalValue != securityContext;
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
      return new InternalEventImplementation(context,
                                             requireNonNull(messageFactory.apply(context)),
                                             varsModified ? flowVariables : originalVars,
                                             parametersModified ? parameters : originalParameters,
                                             loggingVariables,
                                             internalParameters,
                                             securityContext,
                                             itemSequenceInfo,
                                             flowProcessMediatorContext,
                                             sdkInternalContext,
                                             foreachInternalContext,
                                             sourcePolicyContext,
                                             operationPolicyContext,
                                             error,
                                             legacyCorrelationId,
                                             notificationsEnabled);
    }
  }

  protected void initVariables() {
    if (!varsModified && flowVariables == null) {
      flowVariables = originalVars.copy();
    }
  }

  protected void initInternalParameters() {
    if (!internalParametersInitialized) {
      internalParameters = copy(internalParameters);
      internalParametersInitialized = true;
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
  public static class InternalEventImplementation implements InternalEvent {

    private static final long serialVersionUID = 1L;

    /**
     * Immutable MuleEvent state
     **/

    private final BaseEventContext context;
    // TODO MULE-10013 make this final
    private Message message;
    // This is here just for backwards compatibility of preexisting serialized objects.
    private final MuleSession session = null;
    private final SecurityContext securityContext;

    private final boolean notificationsEnabled;

    private final CaseInsensitiveHashMap<String, TypedValue<?>> variables;
    private final CaseInsensitiveHashMap<String, TypedValue<?>> parameters;
    private final CaseInsensitiveHashMap<String, String> loggingVariables;

    private final String legacyCorrelationId;
    private final Error error;

    private final ItemSequenceInfo itemSequenceInfo;

    private transient Map<String, ?> internalParameters;
    private transient FlowProcessMediatorContext flowProcessMediatorContext;
    private transient EventInternalContext sdkInternalContext;
    private transient EventInternalContext foreachInternalContext;
    private transient EventInternalContext sourcePolicyContext;
    private transient EventInternalContext operationPolicyContext;
    private transient volatile BindingContext bindingContextBuilder;

    // Needed for deserialization with kryo
    private InternalEventImplementation() {
      this.context = null;
      this.securityContext = null;
      this.notificationsEnabled = false;
      this.variables = null;
      this.parameters = null;
      this.loggingVariables = null;
      this.legacyCorrelationId = null;
      this.error = null;
      this.itemSequenceInfo = null;
      this.flowProcessMediatorContext = null;
      this.sdkInternalContext = null;
      this.foreachInternalContext = null;
      this.sourcePolicyContext = null;
      this.operationPolicyContext = null;
      this.internalParameters = new SmallMap<>();
    }

    // Use this constructor from the builder
    private InternalEventImplementation(BaseEventContext context,
                                        Message message,
                                        CaseInsensitiveHashMap<String, TypedValue<?>> variables,
                                        CaseInsensitiveHashMap<String, TypedValue<?>> parameters,
                                        CaseInsensitiveHashMap<String, String> loggingVariables,
                                        Map<String, ?> internalParameters,
                                        SecurityContext securityContext,
                                        Optional<ItemSequenceInfo> itemSequenceInfo,
                                        FlowProcessMediatorContext flowProcessMediatorContext,
                                        EventInternalContext sdkInternalContext,
                                        EventInternalContext foreachInternalContext,
                                        EventInternalContext sourcePolicyContext,
                                        EventInternalContext operationPolicyContext,
                                        Error error,
                                        String legacyCorrelationId,
                                        boolean notificationsEnabled) {
      this.context = context;
      this.loggingVariables = loggingVariables;
      this.securityContext = securityContext;
      this.message = message;
      this.variables = variables.toImmutableCaseInsensitiveMap();
      this.parameters = parameters.toImmutableCaseInsensitiveMap();

      this.internalParameters = internalParameters;

      this.itemSequenceInfo = itemSequenceInfo.orElse(null);
      this.flowProcessMediatorContext = flowProcessMediatorContext;
      this.sdkInternalContext = sdkInternalContext;
      this.foreachInternalContext = foreachInternalContext;
      this.sourcePolicyContext = sourcePolicyContext;
      this.operationPolicyContext = operationPolicyContext;
      this.error = error;
      this.legacyCorrelationId = legacyCorrelationId;

      this.notificationsEnabled = notificationsEnabled;
    }

    private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException {
      is.defaultReadObject();
      internalParameters = new SmallMap<>();
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
    public String toString() {
      return "DefaultMuleEvent{" + lineSeparator() +
          "  context=" + context + lineSeparator() +
          "  message=" + message + lineSeparator() +
          "  variables=" + variables + lineSeparator() +
          "  error=" + error + lineSeparator() +
          '}';
    }

    @Override
    public Optional<Map<String, String>> getLoggingVariables() {
      return ofNullable(loggingVariables);
    }

    @Override
    public Map<String, TypedValue<?>> getVariables() {
      return variables;
    }

    @Override
    public Map<String, TypedValue<?>> getParameters() {
      return parameters;
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
      return unmodifiable(internalParameters);
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
    public FlowProcessMediatorContext getFlowProcessMediatorContext() {
      return flowProcessMediatorContext;
    }

    @Override
    public void setFlowProcessMediatorContext(FlowProcessMediatorContext flowProcessMediatorContext) {
      if (this.flowProcessMediatorContext != null) {
        throw new IllegalStateException("flowProcessMediatorContext was already set.");
      }
      this.flowProcessMediatorContext = flowProcessMediatorContext;
    }

    @Override
    public EventInternalContext getSdkInternalContext() {
      return sdkInternalContext;
    }

    @Override
    public void setSdkInternalContext(EventInternalContext sdkInternalContext) {
      this.sdkInternalContext = sdkInternalContext;
    }

    @Override
    public EventInternalContext getForeachInternalContext() {
      return foreachInternalContext;
    }

    @Override
    public void setForeachInternalContext(EventInternalContext foreachInternalContext) {
      this.foreachInternalContext = foreachInternalContext;
    }

    @Override
    public EventInternalContext getSourcePolicyContext() {
      return sourcePolicyContext;
    }

    @Override
    public void setSourcePolicyContext(EventInternalContext sourcePolicyContext) {
      this.sourcePolicyContext = sourcePolicyContext;
    }

    @Override
    public EventInternalContext getOperationPolicyContext() {
      return operationPolicyContext;
    }

    @Override
    public void setOperationPolicyContext(EventInternalContext operationPolicyContext) {
      this.operationPolicyContext = operationPolicyContext;
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
      if (bindingContextBuilder == null) {
        synchronized (this) {
          if (bindingContextBuilder == null) {
            bindingContextBuilder = addEventBindings(this, NULL_BINDING_CONTEXT);
          }
        }
      }
      return bindingContextBuilder;
    }
  }

}
