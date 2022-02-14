/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static com.google.common.base.Functions.identity;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.component.execution.CompletableCallback.always;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.tx.TransactionType.LOCAL;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Handleable.REDELIVERY_EXHAUSTED;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Unhandleable.FLOW_BACK_PRESSURE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_ACTION_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_TYPE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.runtime.source.BackPressureAction.FAIL;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.BACK_PRESSURE_ACTION_CONTEXT_PARAM;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.fetchConfigFieldFromSourceObject;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.fetchConnectionFieldFromSourceObject;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsOfType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getSourceName;
import static org.mule.sdk.api.tx.SourceTransactionalAction.NONE;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.create;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.api.tx.TransactionType;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.source.BackPressureAction;
import org.mule.runtime.extension.internal.property.TransactionalActionModelProperty;
import org.mule.runtime.extension.internal.property.TransactionalTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SourceCallbackModelProperty;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ReactiveReconnectionCallback;
import org.mule.runtime.module.extension.internal.runtime.connectivity.SdkReconnectableAdapter;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.runtime.source.legacy.LegacySourceWrapper;
import org.mule.runtime.module.extension.internal.runtime.source.legacy.SourceTransactionalActionUtils;
import org.mule.runtime.module.extension.internal.runtime.source.poll.RestartContext;
import org.mule.runtime.module.extension.internal.runtime.source.poll.Restartable;
import org.mule.runtime.module.extension.internal.util.FieldSetter;
import org.mule.sdk.api.runtime.connectivity.Reconnectable;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.sdk.api.tx.SourceTransactionalAction;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

/**
 * An adapter for {@link Source} which acts as a bridge with {@link ExtensionMessageSource}. It also propagates lifecycle and
 * performs injection of both, dependencies and parameters
 *
 * @since 4.0
 */
public class SourceAdapter implements Lifecycle, Restartable {

  private static final Logger LOGGER = getLogger(SourceAdapter.class);

  private final ExtensionModel extensionModel;
  private final SourceModel sourceModel;
  private final Source source;
  private final Optional<ConfigurationInstance> configurationInstance;
  private final Optional<FieldSetter<Object, Object>> configurationSetter;
  private final Optional<FieldSetter<Object, ConnectionProvider>> connectionSetter;
  private final SourceCallbackFactory sourceCallbackFactory;
  private final CursorProviderFactory cursorProviderFactory;
  private final ResolverSet nonCallbackParameters;
  private final ResolverSet successCallbackParameters;
  private final ResolverSet errorCallbackParameters;
  private final Component component;
  private final SourceConnectionManager connectionManager;
  private final MessagingExceptionResolver exceptionResolver;
  private final BackPressureAction backPressureAction;
  private final Supplier<Object> sourceInvokationTarget;

  private ErrorType flowBackPressueErrorType;
  private ErrorType redeliveryExhaustedErrorType;
  private boolean initialised = false;

  @Inject
  private StreamingManager streamingManager;

  @Inject
  private ErrorTypeRepository errorTypeRepository;

  @Inject
  private ExpressionManager expressionManager;

  @Inject
  private Collection<ExceptionContextProvider> exceptionContextProviders;

  @Inject
  private ErrorTypeLocator errorTypeLocator;

  @Inject
  private MuleContext muleContext;

  public SourceAdapter(ExtensionModel extensionModel, SourceModel sourceModel,
                       Source source,
                       Optional<ConfigurationInstance> configurationInstance,
                       CursorProviderFactory cursorProviderFactory,
                       SourceCallbackFactory sourceCallbackFactory,
                       Component component,
                       SourceConnectionManager connectionManager,
                       ResolverSet nonCallbackParameters,
                       ResolverSet successCallbackParameters,
                       ResolverSet errorCallbackParameters,
                       Optional<BackPressureAction> backPressureAction) {
    this.extensionModel = extensionModel;
    this.sourceModel = sourceModel;
    this.source = source;
    sourceInvokationTarget = new LazyValue<>(() -> unwrapSource(source));
    this.cursorProviderFactory = cursorProviderFactory;
    this.configurationInstance = configurationInstance;
    this.sourceCallbackFactory = sourceCallbackFactory;
    this.component = component;
    this.connectionManager = connectionManager;
    this.nonCallbackParameters = nonCallbackParameters;
    this.successCallbackParameters = successCallbackParameters;
    this.errorCallbackParameters = errorCallbackParameters;
    this.exceptionResolver = new MessagingExceptionResolver(component);
    this.configurationSetter = fetchConfigurationField();
    this.connectionSetter = fetchConnectionProviderField();
    this.backPressureAction = backPressureAction.orElse(FAIL);
  }

  private Object unwrapSource(Source source) {
    if (source instanceof SourceWrapper) {
      return unwrapSource(((SourceWrapper) source).getDelegate());
    } else if (source instanceof LegacySourceWrapper) {
      return ((LegacySourceWrapper) source).getDelegate();
    }
    return source;
  }

  private SourceCallback createSourceCallback() {
    return sourceCallbackFactory.createSourceCallback(createCompletionHandlerFactory());
  }

  private SourceCompletionHandlerFactory createCompletionHandlerFactory() {
    return sourceModel.getModelProperty(SourceCallbackModelProperty.class)
        .map(this::doCreateCompletionHandler)
        .orElse(context -> new NullSourceCompletionHandler());
  }

  private SourceCompletionHandlerFactory doCreateCompletionHandler(SourceCallbackModelProperty modelProperty) {
    SourceCallbackExecutor onSuccessExecutor;
    SourceCallbackExecutor onErrorExecutor;
    SourceCallbackExecutor onTerminateExecutor;
    SourceCallbackExecutor onBackPressureExecutor;

    if (source instanceof SourceWrapper) {
      SourceWrapper wrapper = (SourceWrapper) source;
      onSuccessExecutor = getMethodExecutor(modelProperty.getOnSuccessMethod(), modelProperty, wrapper::onSuccess);
      onErrorExecutor = getMethodExecutor(modelProperty.getOnErrorMethod(), modelProperty, wrapper::onError);
      onTerminateExecutor = getMethodExecutor(modelProperty.getOnTerminateMethod(), modelProperty, wrapper::onTerminate);
      onBackPressureExecutor = getMethodExecutor(modelProperty.getOnBackPressureMethod(), modelProperty, wrapper::onBackPressure);
    } else {
      onSuccessExecutor = getMethodExecutor(modelProperty.getOnSuccessMethod(), modelProperty);
      onErrorExecutor = getMethodExecutor(modelProperty.getOnErrorMethod(), modelProperty);
      onTerminateExecutor = getMethodExecutor(modelProperty.getOnTerminateMethod(), modelProperty);
      onBackPressureExecutor = getMethodExecutor(modelProperty.getOnBackPressureMethod(), modelProperty);
    }

    return context -> new DefaultSourceCompletionHandler(onSuccessExecutor, onErrorExecutor, onTerminateExecutor,
                                                         onBackPressureExecutor, context);
  }

  private SourceCallbackExecutor getMethodExecutor(Optional<Method> method,
                                                   SourceCallbackModelProperty sourceCallbackModel) {
    return getMethodExecutor(method, sourceCallbackModel, null);
  }

  private SourceCallbackExecutor getMethodExecutor(Optional<Method> method,
                                                   SourceCallbackModelProperty sourceCallbackModel,
                                                   SourceCallbackExecutor then) {
    SourceCallbackExecutor executor = method
        .map(m -> (SourceCallbackExecutor) new DefaultSourceCallbackExecutor(extensionModel, configurationInstance,
                                                                             sourceModel,
                                                                             sourceInvokationTarget.get(),
                                                                             m, cursorProviderFactory,
                                                                             streamingManager,
                                                                             component,
                                                                             muleContext,
                                                                             sourceCallbackModel))
        .orElse(NullSourceCallbackExecutor.INSTANCE);

    if (then != null) {
      executor = new ComposedSourceCallbackExecutor(executor, then);
    }

    return executor;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (initialised) {
      return;
    }

    flowBackPressueErrorType = errorTypeRepository.getErrorType(FLOW_BACK_PRESSURE)
        .orElseThrow(() -> new IllegalStateException("FLOW_BACK_PRESSURE error type not found"));
    redeliveryExhaustedErrorType = errorTypeRepository.getErrorType(REDELIVERY_EXHAUSTED)
        .orElseThrow(() -> new IllegalStateException("REDELIVERY_EXHAUSTED error type not found"));

    initialiseIfNeeded(nonCallbackParameters, true, muleContext);
    initialiseIfNeeded(errorCallbackParameters, true, muleContext);
    initialiseIfNeeded(successCallbackParameters, true, muleContext);

    injectComponentLocation();

    try {
      setConfiguration(configurationInstance);
      setConnection();
      muleContext.getInjector().inject(sourceInvokationTarget.get());
      if (source instanceof SourceWrapper) {
        muleContext.getInjector().inject(source);
      }
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }

    initialiseIfNeeded(source);
    initialised = true;
  }

  @Override
  public void dispose() {
    disposeIfNeeded(source, LOGGER);
    initialised = false;
  }

  @Override
  public RestartContext beginRestart() {
    if (source instanceof Restartable) {
      return ((Restartable) source).beginRestart();
    }
    return null;
  }

  @Override
  public void finishRestart(RestartContext restartContext) {
    if (source instanceof Restartable) {
      ((Restartable) source).finishRestart(restartContext);
    }
  }


  public class DefaultSourceCompletionHandler implements SourceCompletionHandler {

    private final SourceCallbackExecutor onSuccessExecutor;
    private final SourceCallbackExecutor onErrorExecutor;
    private final SourceCallbackContextAdapter context;
    private final SourceCallbackExecutor onTerminateExecutor;
    private final SourceCallbackExecutor onBackPressureExecutor;

    public DefaultSourceCompletionHandler(SourceCallbackExecutor onSuccessExecutor,
                                          SourceCallbackExecutor onErrorExecutor,
                                          SourceCallbackExecutor onTerminateExecutor,
                                          SourceCallbackExecutor onBackPressureExecutor,
                                          SourceCallbackContextAdapter context) {
      this.onSuccessExecutor = onSuccessExecutor;
      this.onErrorExecutor = onErrorExecutor;
      this.onTerminateExecutor = onTerminateExecutor;
      this.onBackPressureExecutor = onBackPressureExecutor;
      this.context = context;
    }

    @Override
    public void onCompletion(CoreEvent event, Map<String, Object> parameters, CompletableCallback<Void> callback) {
      if (context.getTransactionHandle().isTransacted()) {
        callback = callback.before(v -> commit());
      }
      onSuccessExecutor.execute(event, parameters, context, callback);
    }

    @Override
    public void onFailure(MessagingException exception, Map<String, Object> parameters, CompletableCallback<Void> callback) {
      final CoreEvent event = exception.getEvent();
      final boolean isBackPressureError = event.getError()
          .map(e -> flowBackPressueErrorType.equals(e.getErrorType()))
          .orElse(false);
      final boolean isRedeliveryExhaustedError = event.getError()
          .map(e -> redeliveryExhaustedErrorType.equals(e.getErrorType()))
          .orElse(false);

      SourceCallbackExecutor executor;
      if (isBackPressureError) {
        LOGGER.info("FLOW OVERLOAD - {}.", event.getError().get().getCause().getMessage());
        executor = onBackPressureExecutor;
        parameters = emptyMap();
        context.addVariable(BACK_PRESSURE_ACTION_CONTEXT_PARAM, backPressureAction);
      } else {
        executor = onErrorExecutor;
      }

      if (context.getTransactionHandle().isTransacted()) {
        if (isRedeliveryExhaustedError) {
          callback = callback.finallyBefore(this::commit);
        } else {
          callback = callback.finallyBefore(this::rollback);
        }
      }

      executor.execute(event, parameters, context, callback);
    }

    @Override
    public void onTerminate(Either<MessagingException, CoreEvent> result) throws Exception {
      onTerminateExecutor.execute(result.reduce(MessagingException::getEvent, identity()), emptyMap(), context,
                                  always(context::releaseConnection));
    }

    private void commit() {
      try {
        context.getTransactionHandle().resolve();
      } catch (TransactionException e) {
        LOGGER.error(format("Failed to commit transaction for message source at '%s': %s",
                            component.getLocation().toString(),
                            e.getMessage()),
                     e);
      }
    }

    private void rollback() {
      try {
        context.getTransactionHandle().rollback();
      } catch (TransactionException e) {
        LOGGER.error(format("Failed to rollback transaction for message source at '%s': %s",
                            component.getLocation().toString(),
                            e.getMessage()),
                     e);
      }
    }

    @Override
    public Map<String, Object> createResponseParameters(CoreEvent event) throws MessagingException {
      try (ValueResolvingContext context = buildResolvingContext(event)) {
        return SourceAdapter.this.successCallbackParameters.resolve(context).asMap();
      } catch (Exception e) {
        throw createSourceException(event, e);
      }
    }

    @Override
    public Map<String, Object> createFailureResponseParameters(CoreEvent event) throws MessagingException {
      ResolverSet resolverSet = SourceAdapter.this.errorCallbackParameters;
      try (ValueResolvingContext ctx = buildResolvingContext(event)) {
        ResolverSetResult parameters = resolverSet.resolve(ctx);
        return parameters.asMap();
      } catch (Exception e) {
        throw createSourceException(event, e);
      }
    }

    private ValueResolvingContext buildResolvingContext(CoreEvent event) {
      return ValueResolvingContext.builder(event)
          .withExpressionManager(expressionManager)
          .withConfig(configurationInstance)
          .resolveCursors(false)
          .build();
    }
  }

  @Override
  public void start() throws MuleException {
    try {
      source.onStart(createSourceCallback());
    } catch (Exception e) {
      throw new DefaultMuleException(e);
    }
  }

  private void injectComponentLocation() {
    injectComponentLocation(sourceInvokationTarget.get());
    if (source instanceof SourceWrapper) {
      injectComponentLocation(source);
    }
  }

  private void injectComponentLocation(Object source) {
    // ComponentLocationModelValidator assures that there's at most one field
    List<Field> fields = getFieldsOfType(source.getClass(), ComponentLocation.class);
    if (fields.isEmpty()) {
      return;
    }

    new FieldSetter<>(fields.get(0)).set(source, component.getLocation());
  }

  @Override
  public void stop() throws MuleException {
    try {
      source.onStop();
    } catch (Exception e) {
      throw new DefaultMuleException(e);
    }
  }

  private void setConfiguration(Optional<ConfigurationInstance> configuration) {
    if (configurationSetter.isPresent() && configuration.isPresent()) {
      configurationSetter.get().set(sourceInvokationTarget.get(), configuration.get().getValue());
    }
  }

  private void setConnection() throws MuleException {
    if (!connectionSetter.isPresent()) {
      return;
    }

    FieldSetter<Object, ConnectionProvider> setter = connectionSetter.get();
    ConfigurationInstance config = configurationInstance.orElseThrow(() -> new DefaultMuleException(createStaticMessage(
                                                                                                                        "Message Source on root component '%s' requires a connection but it doesn't point to any configuration. Please review your "
                                                                                                                            + "application",
                                                                                                                        component
                                                                                                                            .getLocation()
                                                                                                                            .getRootContainerName())));

    if (!config.getConnectionProvider().isPresent()) {
      throw new DefaultMuleException(createStaticMessage(format(
                                                                "Message Source on root component '%s' requires a connection, but points to config '%s' which doesn't specify any. "
                                                                    + "Please review your application",
                                                                component.getLocation().getRootContainerName(),
                                                                config.getName())));
    }

    ConnectionProvider<Object> connectionProvider = new SourceConnectionProvider(connectionManager, config);
    setter.set(sourceInvokationTarget.get(), connectionProvider);
  }

  Optional<ConfigurationInstance> getConfigurationInstance() {
    return configurationInstance;
  }

  private <T> Optional<FieldSetter<Object, T>> fetchConfigurationField() {
    Optional<Field> configurationField = fetchConfigFieldFromSourceObject(sourceInvokationTarget.get());
    return configurationField.map(FieldSetter::new);
  }

  private <T> Optional<FieldSetter<Object, T>> fetchConnectionProviderField() {
    Optional<Field> connectionField = fetchConnectionFieldFromSourceObject(sourceInvokationTarget.get());
    return connectionField.map(field -> {
      if (!ConnectionProvider.class.equals(field.getType())) {
        throw new IllegalModelDefinitionException(format(
                                                         "Message Source defined on class '%s' has field '%s' of type '%s' annotated with @%s. That annotation can only be "
                                                             + "used on fields of type '%s'",
                                                         sourceInvokationTarget.get().getClass().getName(), field.getName(),
                                                         field.getType().getName(),
                                                         Connection.class.getName(),
                                                         ConnectionProvider.class.getName()));
      }

      return new FieldSetter<>(field);
    });
  }

  public String getName() {
    return getSourceName(sourceInvokationTarget.get().getClass());
  }

  public Object getDelegate() {
    return sourceInvokationTarget.get();
  }

  Optional<Publisher<Void>> getReconnectionAction(ConnectionException e) {
    Optional<Reconnectable> adapter = SdkReconnectableAdapter.from(sourceInvokationTarget.get());
    return adapter.map(reconnectable -> create(sink -> (reconnectable).reconnect(e, new ReactiveReconnectionCallback(sink))));
  }

  public SourceTransactionalAction getTransactionalAction() {
    Optional<Object> transactionalAction = getNonCallbackParameterValue(getTransactionalActionFieldName());
    if (transactionalAction.isPresent()) {
      try {
        return SourceTransactionalActionUtils.from(transactionalAction.get());
      } catch (Exception e) {
        throw new IllegalStateException("The resolved value is not a " + SourceTransactionalAction.class.getSimpleName(), e);
      }
    } else {
      return NONE;
    }
  }

  TransactionType getTransactionalType() {
    Optional<Object> transactionalType = getNonCallbackParameterValue(getTransactionTypeFieldName());
    if (transactionalType.isPresent()) {
      if (transactionalType.get() instanceof TransactionType) {
        return (TransactionType) transactionalType.get();
      }
      throw new IllegalStateException("The resolved value is not a " + TransactionType.class.getSimpleName());
    } else {
      return LOCAL;
    }
  }

  private <T> Optional<T> getNonCallbackParameterValue(String fieldName) {
    ValueResolver<T> valueResolver = (ValueResolver<T>) nonCallbackParameters.getResolvers().get(fieldName);

    if (valueResolver == null) {
      return empty();
    }

    T object;

    CoreEvent initialiserEvent = getInitialiserEvent(muleContext);
    try (ValueResolvingContext context = ValueResolvingContext.builder(initialiserEvent, expressionManager).build()) {
      object = valueResolver.resolve(context);
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage("Unable to get the " + fieldName + " value for Message Source"), e);
    } finally {
      if (initialiserEvent != null) {
        ((BaseEventContext) initialiserEvent.getContext()).success();
      }
    }

    return of(object);
  }

  private String getTransactionalActionFieldName() {
    return getFieldNameEnrichedWith(TransactionalActionModelProperty.class, TRANSACTIONAL_ACTION_PARAMETER_NAME);
  }

  private String getTransactionTypeFieldName() {
    return getFieldNameEnrichedWith(TransactionalTypeModelProperty.class, TRANSACTIONAL_TYPE_PARAMETER_NAME);
  }

  private String getFieldNameEnrichedWith(Class<? extends ModelProperty> type, String defaultName) {
    return sourceModel.getAllParameterModels()
        .stream()
        .filter(param -> param.getModelProperty(type).isPresent())
        .filter(param -> param.getModelProperty(DeclaringMemberModelProperty.class).isPresent())
        .map(param -> param.getModelProperty(DeclaringMemberModelProperty.class).get())
        .findAny()
        .map(modelProperty -> modelProperty.getDeclaringField().getName()).orElse(defaultName);
  }

  private MessagingException createSourceException(CoreEvent event, Throwable cause) {
    return exceptionResolver.resolve(new MessagingException(event, cause), errorTypeLocator, exceptionContextProviders);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + ": " + Objects.toString(source);
  }
}
