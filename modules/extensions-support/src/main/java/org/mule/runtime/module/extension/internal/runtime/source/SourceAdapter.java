/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.tx.TransactionType.LOCAL;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_ACTION_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_TYPE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.tx.SourceTransactionalAction.NONE;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsOfType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getSourceName;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withAnnotation;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.create;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.api.tx.TransactionType;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.connectivity.Reconnectable;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.tx.SourceTransactionalAction;
import org.mule.runtime.extension.internal.property.TransactionalActionModelProperty;
import org.mule.runtime.extension.internal.property.TransactionalTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SourceCallbackModelProperty;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ReactiveReconnectionCallback;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.util.FieldSetter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;

/**
 * An adapter for {@link Source} which acts as a bridge with {@link ExtensionMessageSource}. It also propagates lifecycle and
 * performs injection of both, dependencies and parameters
 *
 * @since 4.0
 */
public final class SourceAdapter implements Startable, Stoppable, Initialisable {

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
  private final ComponentLocation componentLocation;
  private final SourceConnectionManager connectionManager;
  private final MessagingExceptionResolver exceptionResolver;

  @Inject
  private StreamingManager streamingManager;

  @Inject
  private MuleContext muleContext;

  public SourceAdapter(ExtensionModel extensionModel, SourceModel sourceModel,
                       Source source,
                       Optional<ConfigurationInstance> configurationInstance,
                       CursorProviderFactory cursorProviderFactory,
                       SourceCallbackFactory sourceCallbackFactory,
                       ComponentLocation componentLocation,
                       SourceConnectionManager connectionManager,
                       ResolverSet nonCallbackParameters,
                       ResolverSet successCallbackParameters,
                       ResolverSet errorCallbackParameters,
                       MessagingExceptionResolver exceptionResolver) {
    this.extensionModel = extensionModel;
    this.sourceModel = sourceModel;
    this.source = source;
    this.cursorProviderFactory = cursorProviderFactory;
    this.configurationInstance = configurationInstance;
    this.sourceCallbackFactory = sourceCallbackFactory;
    this.componentLocation = componentLocation;
    this.connectionManager = connectionManager;
    this.nonCallbackParameters = nonCallbackParameters;
    this.successCallbackParameters = successCallbackParameters;
    this.errorCallbackParameters = errorCallbackParameters;
    this.exceptionResolver = exceptionResolver;
    this.configurationSetter = fetchConfigurationField();
    this.connectionSetter = fetchConnectionProviderField();
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
    final SourceCallbackExecutor onSuccessExecutor = getMethodExecutor(modelProperty.getOnSuccessMethod(), modelProperty);
    final SourceCallbackExecutor onErrorExecutor = getMethodExecutor(modelProperty.getOnErrorMethod(), modelProperty);
    final SourceCallbackExecutor onTerminateExecutor = getMethodExecutor(modelProperty.getOnTerminateMethod(), modelProperty);

    return context -> new DefaultSourceCompletionHandler(onSuccessExecutor, onErrorExecutor, onTerminateExecutor, context);
  }

  private SourceCallbackExecutor getMethodExecutor(Optional<Method> method, SourceCallbackModelProperty sourceCallbackModel) {
    return method.map(m -> (SourceCallbackExecutor) new ReflectiveSourceCallbackExecutor(extensionModel, configurationInstance,
                                                                                         sourceModel, source, m,
                                                                                         cursorProviderFactory,
                                                                                         streamingManager,
                                                                                         componentLocation,
                                                                                         muleContext,
                                                                                         sourceCallbackModel))
        .orElse(new NullSourceCallbackExecutor());
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(this.nonCallbackParameters, true, muleContext);
    initialiseIfNeeded(this.errorCallbackParameters, true, muleContext);
    initialiseIfNeeded(this.successCallbackParameters, true, muleContext);
  }


  public class DefaultSourceCompletionHandler implements SourceCompletionHandler {

    private final SourceCallbackExecutor onSuccessExecutor;
    private final SourceCallbackExecutor onErrorExecutor;
    private final SourceCallbackContextAdapter context;
    private final SourceCallbackExecutor onTerminateExecutor;

    public DefaultSourceCompletionHandler(SourceCallbackExecutor onSuccessExecutor,
                                          SourceCallbackExecutor onErrorExecutor,
                                          SourceCallbackExecutor onTerminateExecutor,
                                          SourceCallbackContextAdapter context) {
      this.onSuccessExecutor = onSuccessExecutor;
      this.onErrorExecutor = onErrorExecutor;
      this.onTerminateExecutor = onTerminateExecutor;
      this.context = context;
    }

    @Override
    public Publisher<Void> onCompletion(CoreEvent event, Map<String, Object> parameters) {
      return from(onSuccessExecutor.execute(event, parameters, context)).doOnSuccess(v -> commit());
    }

    @Override
    public Publisher<Void> onFailure(MessagingException exception, Map<String, Object> parameters) {
      return from(onErrorExecutor.execute(exception.getEvent(), parameters, context))
          .doAfterTerminate(() -> rollback());
    }

    @Override
    public void onTerminate(Either<MessagingException, CoreEvent> result) throws Exception {
      CoreEvent event = result.isRight() ? result.getRight() : result.getLeft().getEvent();
      from(onTerminateExecutor.execute(event, emptyMap(), context))
          .doAfterTerminate(() -> context.releaseConnection())
          .subscribe();
    }

    private void commit() {
      try {
        context.getTransactionHandle().commit();
      } catch (TransactionException e) {
        LOGGER.error(format("Failed to commit transaction for message source at '%s': %s",
                            componentLocation.toString(),
                            e.getMessage()),
                     e);
      }
    }

    private void rollback() {
      try {
        context.getTransactionHandle().rollback();
      } catch (TransactionException e) {
        LOGGER.error(format("Failed to rollback transaction for message source at '%s': %s",
                            componentLocation.toString(),
                            e.getMessage()),
                     e);
      }
    }

    @Override
    public Map<String, Object> createResponseParameters(CoreEvent event) throws MessagingException {
      try {
        ResolverSetResult parameters = SourceAdapter.this.successCallbackParameters.resolve(from(event, configurationInstance));
        return parameters.asMap();
      } catch (Exception e) {
        throw createSourceException(event, e);
      }
    }

    @Override
    public Map<String, Object> createFailureResponseParameters(CoreEvent event) throws MessagingException {
      try {
        ResolverSetResult parameters = SourceAdapter.this.errorCallbackParameters.resolve(from(event, configurationInstance));
        return parameters.asMap();
      } catch (Exception e) {
        throw createSourceException(event, e);
      }
    }
  }

  @Override
  public void start() throws MuleException {
    injectComponentLocation();

    try {
      setConfiguration(configurationInstance);
      setConnection();
      muleContext.getInjector().inject(source);
      source.onStart(createSourceCallback());
    } catch (Exception e) {
      throw new DefaultMuleException(e);
    }
  }

  private void injectComponentLocation() {
    // ComponentLocationModelValidator assures that there's at most one field
    List<Field> fields = getFieldsOfType(source.getClass(), ComponentLocation.class);
    if (fields.isEmpty()) {
      return;
    }

    new FieldSetter<>(fields.get(0)).set(source, componentLocation);
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
      configurationSetter.get().set(source, configuration.get().getValue());
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
                                                                                                                        componentLocation
                                                                                                                            .getRootContainerName())));

    if (!config.getConnectionProvider().isPresent()) {
      throw new DefaultMuleException(createStaticMessage(format(
                                                                "Message Source on root component '%s' requires a connection, but points to config '%s' which doesn't specify any. "
                                                                    + "Please review your application",
                                                                componentLocation.getRootContainerName(), config.getName())));
    }

    ConnectionProvider<Object> connectionProvider = new SourceConnectionProvider(connectionManager, config);
    setter.set(source, connectionProvider);
  }

  Optional<ConfigurationInstance> getConfigurationInstance() {
    return configurationInstance;
  }

  private <T> Optional<FieldSetter<Object, T>> fetchConfigurationField() {
    return fetchField(Config.class).map(FieldSetter::new);
  }

  private <T> Optional<FieldSetter<Object, T>> fetchConnectionProviderField() {
    return fetchField(Connection.class).map(field -> {
      if (!ConnectionProvider.class.equals(field.getType())) {
        throw new IllegalModelDefinitionException(format(
                                                         "Message Source defined on class '%s' has field '%s' of type '%s' annotated with @%s. That annotation can only be "
                                                             + "used on fields of type '%s'",
                                                         source.getClass().getName(), field.getName(), field.getType().getName(),
                                                         Connection.class.getName(),
                                                         ConnectionProvider.class.getName()));
      }

      return new FieldSetter<>(field);
    });
  }

  private Optional<Field> fetchField(Class<? extends Annotation> annotation) {
    Set<Field> fields = getAllFields(source.getClass(), withAnnotation(annotation));
    if (CollectionUtils.isEmpty(fields)) {
      return empty();
    }

    if (fields.size() > 1) {
      // TODO: MULE-9220 Move this to a syntax validator
      throw new IllegalModelDefinitionException(
                                                format("Message Source defined on class '%s' has more than one field annotated with '@%s'. "
                                                    + "Only one field in the class can bare such annotation",
                                                       source.getClass().getName(),
                                                       annotation.getSimpleName()));
    }

    return of(fields.iterator().next());
  }

  public String getName() {
    return getSourceName(source.getClass());
  }

  public Source getDelegate() {
    return source;
  }

  Optional<Publisher<Void>> getReconnectionAction(ConnectionException e) {
    if (source instanceof Reconnectable) {
      return of(create(sink -> ((Reconnectable) source).reconnect(e, new ReactiveReconnectionCallback(sink))));
    }

    return empty();
  }

  public SourceTransactionalAction getTransactionalAction() {
    return getNonCallbackParameterValue(getTransactionalActionFieldName(), SourceTransactionalAction.class)
        .orElse(NONE);
  }

  TransactionType getTransactionalType() {
    return getNonCallbackParameterValue(getTransactionTypeFieldName(), TransactionType.class)
        .orElse(LOCAL);
  }

  private <T> Optional<T> getNonCallbackParameterValue(String fieldName, Class<T> type) {
    ValueResolver<T> valueResolver = (ValueResolver<T>) nonCallbackParameters.getResolvers().get(fieldName);

    if (valueResolver == null) {
      return empty();
    }

    T object;

    try {
      object = valueResolver.resolve(from(getInitialiserEvent(muleContext)));
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage("Unable to get the " + type.getSimpleName()
          + " value for Message Source"), e);
    }

    if (!(type.isInstance(object))) {
      throw new IllegalStateException("The resolved value is not a " + type.getSimpleName());
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
    MessagingException messagingException = new MessagingException(event, cause);

    return exceptionResolver.resolve(messagingException, muleContext);
  }
}
