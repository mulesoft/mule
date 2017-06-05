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
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_ACTION_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getSourceName;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withAnnotation;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.streaming.CursorProviderFactory;
import org.mule.runtime.core.streaming.StreamingManager;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.extension.api.tx.SourceTransactionalAction;
import org.mule.runtime.extension.internal.property.TransactionalActionModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SourceCallbackModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.util.FieldSetter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.reactivestreams.Publisher;

/**
 * An adapter for {@link Source} which acts as a bridge with {@link ExtensionMessageSource}. It also propagates lifecycle and
 * performs injection of both, dependencies and parameters
 *
 * @since 4.0
 */
public final class SourceAdapter implements Startable, Stoppable, Initialisable, FlowConstructAware {

  private final ExtensionModel extensionModel;
  private final SourceModel sourceModel;
  private final Source source;
  private final Optional<ConfigurationInstance> configurationInstance;
  private final Optional<FieldSetter<Object, Object>> configurationSetter;
  private final Optional<FieldSetter<Object, Object>> connectionSetter;
  private final SourceCallbackFactory sourceCallbackFactory;
  private final CursorProviderFactory cursorProviderFactory;
  private final ResolverSet nonCallbackParameters;
  private final ResolverSet successCallbackParameters;
  private final ResolverSet errorCallbackParameters;
  private final ResolverSet terminateCallbackParameters;

  private ConnectionHandler<Object> connectionHandler;
  private FlowConstruct flowConstruct;

  @Inject
  private ConnectionManager connectionManager;

  @Inject
  private StreamingManager streamingManager;

  @Inject
  private MuleContext muleContext;

  public SourceAdapter(ExtensionModel extensionModel, SourceModel sourceModel,
                       Source source,
                       Optional<ConfigurationInstance> configurationInstance,
                       CursorProviderFactory cursorProviderFactory,
                       SourceCallbackFactory sourceCallbackFactory,
                       ResolverSet nonCallbackParameters,
                       ResolverSet successCallbackParameters,
                       ResolverSet terminateCallbackParameters,
                       ResolverSet errorCallbackParameters) {
    this.extensionModel = extensionModel;
    this.sourceModel = sourceModel;
    this.source = source;
    this.cursorProviderFactory = cursorProviderFactory;
    this.configurationInstance = configurationInstance;
    this.sourceCallbackFactory = sourceCallbackFactory;
    this.nonCallbackParameters = nonCallbackParameters;
    this.successCallbackParameters = successCallbackParameters;
    this.terminateCallbackParameters = terminateCallbackParameters;
    this.errorCallbackParameters = errorCallbackParameters;
    this.configurationSetter = fetchField(Config.class);
    this.connectionSetter = fetchField(Connection.class);
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
    private final SourceCallbackContext context;
    private final SourceCallbackExecutor onTerminateExecutor;

    public DefaultSourceCompletionHandler(SourceCallbackExecutor onSuccessExecutor,
                                          SourceCallbackExecutor onErrorExecutor,
                                          SourceCallbackExecutor onTerminateExecutor,
                                          SourceCallbackContext context) {
      this.onSuccessExecutor = onSuccessExecutor;
      this.onErrorExecutor = onErrorExecutor;
      this.onTerminateExecutor = onTerminateExecutor;
      this.context = context;
    }

    @Override
    public Publisher<Void> onCompletion(Event event, Map<String, Object> parameters) {
      return onSuccessExecutor.execute(event, parameters, context);
    }

    @Override
    public Publisher<Void> onFailure(MessagingException exception, Map<String, Object> parameters) {
      return onErrorExecutor.execute(exception.getEvent(), parameters, context);
    }

    @Override
    public void onTerminate(Either<MessagingException, Event> result) throws Exception {
      Event event = result.isRight() ? result.getRight() : result.getLeft().getEvent();
      onTerminateExecutor.execute(event, emptyMap(), context);
    }

    @Override
    public Map<String, Object> createResponseParameters(Event event) throws MessagingException {
      try {
        ResolverSetResult parameters = SourceAdapter.this.successCallbackParameters.resolve(from(event, configurationInstance));
        return parameters.asMap();
      } catch (Exception e) {
        throw new MessagingException(event, e);
      }
    }

    @Override
    public Map<String, Object> createFailureResponseParameters(Event event) throws MessagingException {
      try {
        ResolverSetResult parameters = SourceAdapter.this.errorCallbackParameters.resolve(from(event, configurationInstance));
        return parameters.asMap();
      } catch (Exception e) {
        throw new MessagingException(event, e);
      }
    }
  }

  @Override
  public void start() throws MuleException {
    if (source instanceof FlowConstructAware) {
      ((FlowConstructAware) source).setFlowConstruct(flowConstruct);
    }

    try {
      setConfiguration(configurationInstance);
      setConnection();
      muleContext.getInjector().inject(source);
      source.onStart(createSourceCallback());
    } catch (Exception e) {
      throw new DefaultMuleException(e);
    }
  }

  @Override
  public void stop() throws MuleException {
    try {
      source.onStop();
    } catch (Exception e) {
      throw new DefaultMuleException(e);
    } finally {
      releaseConnection();
    }
  }

  private void setConfiguration(Optional<ConfigurationInstance> configuration) {
    if (configurationSetter.isPresent() && configuration.isPresent()) {
      configurationSetter.get().set(source, configuration.get().getValue());
    }
  }

  private void setConnection() {
    if (connectionSetter.isPresent() && configurationInstance.isPresent()) {
      try {
        connectionHandler = connectionManager.getConnection(configurationInstance.get().getValue());
        connectionSetter.get().set(source, connectionHandler.getConnection());
      } catch (ConnectionException e) {
        throw new MuleRuntimeException(createStaticMessage(format(
                                                                  "Could not obtain connection for message source '%s' on flow '%s'",
                                                                  getName(), flowConstruct.getName())),
                                       e);
      }
    }
  }

  Optional<ConfigurationInstance> getConfigurationInstance() {
    return configurationInstance;
  }

  Optional<ConnectionHandler> getConnectionHandler() {
    return ofNullable(connectionHandler);
  }

  private void releaseConnection() {
    if (connectionHandler != null) {
      try {
        connectionHandler.release();
      } finally {
        connectionHandler = null;
      }
    }
  }

  private <T> Optional<FieldSetter<Object, T>> fetchField(Class<? extends Annotation> annotation) {
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

    return Optional.of(new FieldSetter<>(fields.iterator().next()));
  }

  public String getName() {
    return getSourceName(source.getClass());
  }

  public Source getDelegate() {
    return source;
  }

  public SourceTransactionalAction getTransactionalAction() {
    ValueResolver valueResolver = nonCallbackParameters.getResolvers().get(getTransactionalActionFieldName());
    Object transactionalAction;

    try {
      transactionalAction = valueResolver.resolve(from(getInitialiserEvent(muleContext)));
    } catch (MuleException e) {
      throw new MuleRuntimeException(createStaticMessage("Unable to get the Transactional Action value for Message Source"), e);
    }

    if (!(transactionalAction instanceof SourceTransactionalAction)) {
      throw new IllegalStateException("The resolved value is not a Transactional Action");
    }
    return (SourceTransactionalAction) transactionalAction;
  }

  private String getTransactionalActionFieldName() {
    return sourceModel.getAllParameterModels()
        .stream()
        .filter(param -> param.getModelProperty(TransactionalActionModelProperty.class).isPresent())
        .filter(param -> param.getModelProperty(DeclaringMemberModelProperty.class).isPresent())
        .map(param -> param.getModelProperty(DeclaringMemberModelProperty.class).get())
        .findAny()
        .map(modelProperty -> modelProperty.getDeclaringField().getName()).orElse(TRANSACTIONAL_ACTION_PARAMETER_NAME);
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }
}
