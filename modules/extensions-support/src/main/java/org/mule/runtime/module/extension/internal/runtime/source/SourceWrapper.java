/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.lang.String.format;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withAnnotation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceContext;
import org.mule.runtime.module.extension.internal.util.FieldSetter;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A decorator for {@link Source} which propagates lifecycle and performs injection of both, dependencies and parameters
 *
 * @since 4.0
 */
final class SourceWrapper extends Source implements Lifecycle, FlowConstructAware {

  private static final Logger LOGGER = LoggerFactory.getLogger(SourceWrapper.class);

  private final Source delegate;
  private final Optional<FieldSetter<Object, Object>> configurationSetter;
  private final Optional<FieldSetter<Object, Object>> connectionSetter;

  private SourceContext sourceContext;
  private ConnectionHandler<Object> connectionHandler;
  private FlowConstruct flowConstruct;
  private boolean connectionSet = false;

  @Inject
  private ConnectionManager connectionManager;

  @Inject
  private MuleContext muleContext;

  SourceWrapper(Source delegate) {
    this.delegate = delegate;

    configurationSetter = fetchField(UseConfig.class);
    connectionSetter = fetchField(Connection.class);
  }

  @Override
  public void initialise() throws InitialisationException {
    if (delegate instanceof FlowConstructAware) {
      ((FlowConstructAware) delegate).setFlowConstruct(flowConstruct);
    }

    setConfiguration(sourceContext.getConfigurationInstance());
    setConnection(sourceContext);

    initialiseIfNeeded(delegate, true, muleContext);
  }

  @Override
  public void start() throws MuleException {
    try {
      setConnection(sourceContext);
      delegate.start();
    } catch (Exception e) {
      throw new DefaultMuleException(e);
    }
  }

  @Override
  public void stop() throws MuleException {
    try {
      delegate.stop();
    } catch (Exception e) {
      throw new DefaultMuleException(e);
    } finally {
      releaseConnection();
    }
  }

  @Override
  public void dispose() {
    disposeIfNeeded(delegate, LOGGER);
  }

  @Override
  public void setSourceContext(SourceContext sourceContext) {
    this.sourceContext = sourceContext;
    delegate.setSourceContext(sourceContext);
  }

  private <T> void setConfiguration(ConfigurationInstance<T> configuration) {
    if (configurationSetter.isPresent()) {
      configurationSetter.get().set(delegate, configuration.getValue());
    }
  }

  private void setConnection(SourceContext sourceContext) {
    if (connectionSetter.isPresent() && !connectionSet) {
      try {
        connectionHandler = connectionManager.getConnection(sourceContext.getConfigurationInstance().getValue());
        connectionSetter.get().set(delegate, connectionHandler.getConnection());
        connectionSet = true;
      } catch (ConnectionException e) {
        throw new MuleRuntimeException(createStaticMessage(String
            .format("Could not obtain connection for message source '%s' on flow '%s'", getName(), flowConstruct.getName())), e);
      }
    }
  }

  private void releaseConnection() {
    if (connectionHandler != null) {
      try {
        connectionHandler.release();
      } finally {
        connectionHandler = null;
        connectionSet = false;
      }
    }
  }

  private <T> Optional<FieldSetter<Object, T>> fetchField(Class<? extends Annotation> annotation) {
    Set<Field> fields = getAllFields(delegate.getClass(), withAnnotation(annotation));
    if (CollectionUtils.isEmpty(fields)) {
      return Optional.empty();
    }

    if (fields.size() > 1) {
      // TODO: MULE-9220 Move this to a syntax validator
      throw new IllegalModelDefinitionException(format("Message Source defined on class '%s' has more than one field annotated with '@%s'. "
          + "Only one field in the class can bare such annotation", delegate.getClass().getName(),
                                                       annotation.getClass().getSimpleName()));
    }

    return Optional.of(new FieldSetter<>(fields.iterator().next()));
  }

  public String getName() {
    return IntrospectionUtils.getSourceName(delegate.getClass());
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }
}
