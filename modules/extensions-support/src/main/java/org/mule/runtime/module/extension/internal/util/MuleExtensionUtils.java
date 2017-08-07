/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_ALWAYS_BEGIN;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_ALWAYS_JOIN;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_JOIN_IF_POSSIBLE;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_NONE;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_NOT_SUPPORTED;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.api.util.UUID.getUUID;
import static org.mule.runtime.core.api.util.collection.Collectors.toImmutableList;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader.VERSION;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedFields;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.internal.management.stats.DefaultFlowConstructStatistics;
import org.mule.runtime.core.internal.metadata.NullMetadataResolverFactory;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthModelProperty;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalSourceModelDefinitionException;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.property.ClassLoaderModelProperty;
import org.mule.runtime.extension.api.runtime.InterceptorFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationFactory;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutorFactory;
import org.mule.runtime.extension.api.runtime.source.SourceFactory;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.runtime.extension.api.tx.SourceTransactionalAction;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.InterceptorsModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.NullSafeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.OperationExecutorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.RequireNameField;
import org.mule.runtime.module.extension.internal.loader.java.property.SourceFactoryModelProperty;
import org.mule.runtime.module.extension.internal.runtime.execution.OperationExecutorFactoryWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.xml.namespace.QName;

/**
 * Utilities for handling {@link ExtensionModel extensions}
 *
 * @since 3.7.0
 */
public class MuleExtensionUtils {

  /**
   * @param componentModel a {@link ComponentModel}
   * @return Whether the {@code componentModel} returns a list of messages
   */
  public static boolean returnsListOfMessages(ComponentModel componentModel) {
    MetadataType outputType = componentModel.getOutput().getType();
    return outputType instanceof ArrayType &&
        Message.class.getName().equals(getTypeId(((ArrayType) outputType).getType()).orElse(null));
  }

  /**
   * Returns {@code true} if any of the items in {@code resolvers} return true for the {@link ValueResolver#isDynamic()} method
   *
   * @param resolvers a {@link Iterable} with instances of {@link ValueResolver}
   * @param <T> the generic type of the {@link ValueResolver} items
   * @return {@code true} if at least one {@link ValueResolver} is dynamic, {@code false} otherwise
   */
  public static <T extends Object> boolean hasAnyDynamic(Iterable<ValueResolver<T>> resolvers) {
    for (ValueResolver resolver : resolvers) {
      if (resolver.isDynamic()) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param parameterModel a {@link ParameterModel}
   * @return Whether the given parameter is null safe
   */
  public static boolean isNullSafe(ParameterModel parameterModel) {
    return parameterModel.getModelProperty(NullSafeModelProperty.class).isPresent();
  }

  /**
   * Returns all the {@link ConnectionProviderModel} instances available for the given {@code configurationModel} plus the ones
   * globally defined at the {@code extensionModel}. The {@link List} will first contain those defined at a
   * {@link ConfigurationModel#getConnectionProviders()} level and finally the ones at
   * {@link ExtensionModel#getConnectionProviders()}
   *
   * @param extensionModel the {@link ExtensionModel} which owns the {@code configurationModel}
   * @param configurationModel a {@link ConfigurationModel}
   * @return a {@link List}. Might be empty but will never be {@code null}
   */
  public static List<ConnectionProviderModel> getAllConnectionProviders(ExtensionModel extensionModel,
                                                                        ConfigurationModel configurationModel) {
    return ImmutableList.<ConnectionProviderModel>builder().addAll(configurationModel.getConnectionProviders())
        .addAll(extensionModel.getConnectionProviders()).build();
  }

  /**
   * Whether at least one {@link ConnectionProviderModel} in the given {@cod extensionModel} supports OAuth authentication
   *
   * @param extensionModel a {@link ExtensionModel}
   * @return {@code true} if a {@link ConnectionProviderModel} exist which is OAuth enabled
   */
  public static boolean supportsOAuth(ExtensionModel extensionModel) {
    Reference<ConnectionProviderModel> connectionProvider = new Reference<>();
    new IdempotentExtensionWalker() {

      @Override
      protected void onConnectionProvider(ConnectionProviderModel model) {
        if (model.getModelProperty(OAuthModelProperty.class).isPresent()) {
          connectionProvider.set(model);
          stop();
        }
      }
    }.walk(extensionModel);

    return connectionProvider.get() != null;
  }

  /**
   * Creates a new {@link List} of {@link Interceptor interceptors} using the factories returned by
   * {@link InterceptorsModelProperty} (if present).
   *
   * @param model the model on which {@link InterceptorsModelProperty} is to be invoked
   * @return an immutable {@link List} with instances of {@link Interceptor}
   */
  public static List<Interceptor> createInterceptors(EnrichableModel model) {
    return model.getModelProperty(InterceptorsModelProperty.class)
        .map(p -> createInterceptors(p.getInterceptorFactories()))
        .orElse(ImmutableList.of());
  }

  /**
   * Adds the given {@code interceptorFactory} to the {@code declaration} as the last interceptor in the list
   *
   * @param declaration a {@link BaseDeclaration}
   * @param interceptorFactory a {@link InterceptorFactory}
   */
  public static void addInterceptorFactory(BaseDeclaration declaration, InterceptorFactory interceptorFactory) {
    getOrCreateInterceptorModelProperty(declaration).addInterceptorFactory(interceptorFactory);
  }

  /**
   * Adds the given {@code interceptorFactory} to the {@code declaration} at the given {@code position}
   *
   * @param declaration a {@link BaseDeclaration}
   * @param interceptorFactory a {@link InterceptorFactory}
   * @param position a valid list index
   */
  public static void addInterceptorFactory(BaseDeclaration declaration, InterceptorFactory interceptorFactory, int position) {
    getOrCreateInterceptorModelProperty(declaration).addInterceptorFactory(interceptorFactory, position);
  }

  private static InterceptorsModelProperty getOrCreateInterceptorModelProperty(BaseDeclaration declaration) {
    InterceptorsModelProperty property =
        (InterceptorsModelProperty) declaration.getModelProperty(InterceptorsModelProperty.class).orElse(null);
    if (property == null) {
      property = new InterceptorsModelProperty(emptyList());
      declaration.addModelProperty(property);
    }
    return property;
  }

  /**
   * Creates a new {@link List} of {@link Interceptor interceptors} using the {@code interceptorFactories}
   *
   * @param interceptorFactories a {@link List} with instances of {@link InterceptorFactory}
   * @return an immutable {@link List} with instances of {@link Interceptor}
   */
  public static List<Interceptor> createInterceptors(List<InterceptorFactory> interceptorFactories) {
    if (isEmpty(interceptorFactories)) {
      return ImmutableList.of();
    }

    return interceptorFactories.stream().map(InterceptorFactory::createInterceptor).collect(toImmutableList());
  }

  public static Event getInitialiserEvent() {
    return getInitialiserEvent(null);
  }

  public static Event getInitialiserEvent(MuleContext muleContext) {
    FlowConstruct flowConstruct = new FlowConstruct() {

      @Override
      public Object getAnnotation(QName name) {
        return null;
      }

      @Override
      public Map<QName, Object> getAnnotations() {
        return null;
      }

      @Override
      public void setAnnotations(Map<QName, Object> annotations) {

      }

      @Override
      public ComponentLocation getLocation() {
        return null;
      }

      @Override
      public String getRootContainerName() {
        return null;
      }
      // TODO MULE-9076: This is only needed because the muleContext is get from the given flow.

      @Override
      public MuleContext getMuleContext() {
        return muleContext;
      }

      @Override
      public String getServerId() {
        return "InitialiserServer";
      }

      @Override
      public String getUniqueIdString() {
        return getUUID();
      }

      @Override
      public String getName() {
        return "InitialiserEventFlow";
      }

      @Override
      public LifecycleState getLifecycleState() {
        return null;
      }

      @Override
      public MessagingExceptionHandler getExceptionListener() {
        return null;
      }

      @Override
      public DefaultFlowConstructStatistics getStatistics() {
        return null;
      }
    };
    return Event.builder(create(flowConstruct, fromSingleComponent("InitializerEvent"))).message(of(null)).flow(flowConstruct)
        .build();
  }

  /**
   * Returns the {@link Method} that was used to declare the given {@code operationDeclaration}.
   *
   * @param operationDeclaration a {@link OperationDeclaration}
   * @return A {@link Method} or {@code null} if the {@code operationDeclaration} was defined by other means
   */
  public static java.util.Optional<Method> getImplementingMethod(OperationDeclaration operationDeclaration) {
    return operationDeclaration.getModelProperty(ImplementingMethodModelProperty.class)
        .map(ImplementingMethodModelProperty::getMethod);
  }

  /**
   * Returns the {@link Class} that was used to declare the given {@code declaration}.
   *
   * @param declaration a {@link BaseDeclaration} implementation.
   * @return A {@link Class} where the element was defined or {@link Optional#empty()} if the element was defined by other means.
   */
  public static java.util.Optional<? extends Class<?>> getImplementingType(BaseDeclaration<?> declaration) {
    return declaration.getModelProperty(ImplementingTypeModelProperty.class).map(ImplementingTypeModelProperty::getType);
  }

  /**
   * If the {@code extensionModel} contains a {@link ClassLoaderModelProperty}, then it returns the {@link ClassLoader} associated
   * to such property. Otherwise, it returns the current TCCL
   *
   * @param extensionModel a {@link ExtensionModel}
   * @return a {@link ClassLoader}
   */
  public static ClassLoader getClassLoader(ExtensionModel extensionModel) {
    return extensionModel.getModelProperty(ClassLoaderModelProperty.class).map(ClassLoaderModelProperty::getClassLoader)
        .orElse(currentThread().getContextClassLoader());
  }

  /**
   * Executes the given {@code callable} using the {@link ClassLoader} associated to the {@code extensionModel}
   *
   * @param extensionModel a {@link ExtensionModel}
   * @param callable a {@link Callable}
   * @param <T> the generic type of the {@code callable}'s return type
   * @return the value returned by the {@code callable}
   * @throws Exception if the {@code callable} fails to execute
   */
  public static <T> T withExtensionClassLoader(ExtensionModel extensionModel, Callable<T> callable) throws Exception {
    return withContextClassLoader(getClassLoader(extensionModel), callable);
  }

  public static void injectRefName(EnrichableModel model, Object target, String configName) {
    model.getModelProperty(RequireNameField.class).ifPresent(property -> {
      final Field configNameField = property.getConfigNameField();

      if (!configNameField.getDeclaringClass().isInstance(target)) {
        throw new IllegalConfigurationModelDefinitionException(
                                                               format("field '%s' is annotated with @%s but not defined on an instance of type '%s'",
                                                                      configNameField.toString(),
                                                                      RefName.class.getSimpleName(),
                                                                      target.getClass().getName()));
      }
      new FieldSetter<>(configNameField).set(target, configName);
    });
  }

  public static void injectRefName(Object target, String refName) {
    final Class<?> type = target.getClass();
    List<Field> fields = getAnnotatedFields(type, RefName.class);
    if (fields.isEmpty()) {
      return;
    } else if (fields.size() > 1) {
      throw new IllegalModelDefinitionException(format(
                                                       "Class '%s' has %d fields annotated with @%s. Only one field may carry that annotation",
                                                       type.getName(), fields.size(), RefName.class));
    }

    new FieldSetter<>(fields.get(0)).set(target, refName);
  }

  /**
   * Converts the given {@code action} to its equivalent transactional action as defined in {@link TransactionConfig}
   *
   * @param action a {@link OperationTransactionalAction}
   * @return a byte transactional action
   */
  public static byte toActionCode(OperationTransactionalAction action) {
    switch (action) {
      case ALWAYS_JOIN:
        return ACTION_ALWAYS_JOIN;
      case JOIN_IF_POSSIBLE:
        return ACTION_JOIN_IF_POSSIBLE;
      case NOT_SUPPORTED:
        return ACTION_NOT_SUPPORTED;
    }

    throw new IllegalArgumentException("Unsupported action: " + action.name());
  }

  /**
   * Converts the given {@code action} to its equivalent transactional action as defined in {@link TransactionConfig}
   *
   * @param action a {@link SourceTransactionalAction}
   * @return a byte transactional action
   */
  public static byte toActionCode(SourceTransactionalAction action) {
    switch (action) {
      case ALWAYS_BEGIN:
        return ACTION_ALWAYS_BEGIN;
      case NONE:
        return ACTION_NONE;
    }

    throw new IllegalArgumentException("Unsupported action: " + action.name());
  }

  /**
   * Tests the {@code configurationModel} for a {@link ConfigurationFactoryModelProperty} and returns the contained
   * {@link ConfigurationFactory}.
   *
   * @param configurationModel a {@link ConfigurationModel}
   * @return a {@link ConfigurationFactory}
   * @throws IllegalConfigurationModelDefinitionException if the {@code configurationModel} doesn't contain such model property
   */
  public static ConfigurationFactory getConfigurationFactory(ConfigurationModel configurationModel) {
    return fromModelProperty(configurationModel,
                             ConfigurationFactoryModelProperty.class,
                             ConfigurationFactoryModelProperty::getConfigurationFactory,
                             () -> new IllegalConfigurationModelDefinitionException(
                                                                                    format("Configuration '%s' does not provide a %s",
                                                                                           configurationModel.getName(),
                                                                                           ConfigurationFactory.class
                                                                                               .getName())));
  }

  /**
   * Tests the given {@code model} for a {@link MetadataResolverFactoryModelProperty} and if present it returns the contained
   * {@link MetadataResolverFactory}. If no such property is found, then a {@link NullMetadataResolverFactory} is returned
   *
   * @param model an enriched model
   * @return a {@link MetadataResolverFactory}
   */
  public static MetadataResolverFactory getMetadataResolverFactory(EnrichableModel model) {
    return model.getModelProperty(MetadataResolverFactoryModelProperty.class)
        .map(MetadataResolverFactoryModelProperty::getMetadataResolverFactory)
        .orElse(new NullMetadataResolverFactory());
  }

  /**
   * Tests the given {@code operationModel} for a {@link OperationExecutorModelProperty} and if present it returns the enclosed
   * {@link OperationExecutorFactory}. If no such property is found, then a {@link IllegalOperationModelDefinitionException} is
   * thrown.
   *
   * @param operationModel an {@link OperationModel}
   * @return a {@link OperationExecutorFactory}
   * @throws IllegalOperationModelDefinitionException if the operation is not properly enriched
   */
  public static OperationExecutorFactory getOperationExecutorFactory(OperationModel operationModel) {
    OperationExecutorFactory executorFactory =
        fromModelProperty(operationModel,
                          OperationExecutorModelProperty.class,
                          OperationExecutorModelProperty::getExecutorFactory,
                          () -> new IllegalOperationModelDefinitionException(format("Operation '%s' does not provide a %s",
                                                                                    operationModel.getName(),
                                                                                    OperationExecutorFactory.class
                                                                                        .getSimpleName())));

    return new OperationExecutorFactoryWrapper(executorFactory, createInterceptors(operationModel));
  }

  /**
   * Tests the given {@code sourceModel} for a {@link SourceFactoryModelProperty} and if present it returns the enclosed
   * {@link SourceFactory}. If no such property is found, then a {@link IllegalSourceModelDefinitionException} is thrown
   *
   * @param sourceModel a {@link SourceModel}
   * @return a {@link SourceFactory}
   * @throws IllegalSourceModelDefinitionException if the source is not properly enriched
   */
  public static SourceFactory getSourceFactory(SourceModel sourceModel) {
    return fromModelProperty(sourceModel,
                             SourceFactoryModelProperty.class,
                             SourceFactoryModelProperty::getSourceFactory,
                             () -> new IllegalSourceModelDefinitionException(
                                                                             format("Source '%s' does not provide a %s",
                                                                                    sourceModel.getName(),
                                                                                    SourceFactory.class.getSimpleName())));
  }

  /**
   * Tests the given {@code connectionProviderModel} for a {@link ConnectionProviderFactoryModelProperty} and if present it
   * returns the enclosed {@link ConnectionProviderFactory}. If no such property is found, then a
   * {@link IllegalConnectionProviderModelDefinitionException} is thrown
   *
   * @param connectionProviderModel a {@link ConnectionProviderModel}
   * @return a {@link SourceFactory}
   * @throws IllegalConnectionProviderModelDefinitionException if the connection provider is not properly enriched
   */
  public static ConnectionProviderFactory getConnectionProviderFactory(ConnectionProviderModel connectionProviderModel) {
    return fromModelProperty(connectionProviderModel,
                             ConnectionProviderFactoryModelProperty.class,
                             ConnectionProviderFactoryModelProperty::getConnectionProviderFactory,
                             () -> new IllegalConnectionProviderModelDefinitionException(
                                                                                         format("Connection Provider '%s' does not provide a %s",
                                                                                                connectionProviderModel.getName(),
                                                                                                ConnectionProviderFactory.class
                                                                                                    .getSimpleName())));
  }

  /**
   * Tests the given {@code connectionProviderModel} for a {@link ConnectionTypeModelProperty} and if present it returns the
   * enclosed connection type. If no such property is found, then a {@link IllegalConnectionProviderModelDefinitionException} is
   * thrown
   *
   * @param connectionProviderModel a {@link ConnectionProviderModel}
   * @return a connection {@link Class}
   * @throws IllegalConnectionProviderModelDefinitionException if the connection provider is not properly enriched
   */
  public static Class<?> getConnectionType(ConnectionProviderModel connectionProviderModel) {
    return fromModelProperty(connectionProviderModel,
                             ConnectionTypeModelProperty.class,
                             ConnectionTypeModelProperty::getConnectionType,
                             () -> new IllegalConnectionProviderModelDefinitionException(
                                                                                         format("Connection Provider '%s' does not specify a connection type",
                                                                                                connectionProviderModel
                                                                                                    .getName())));
  }

  private static <T, P extends ModelProperty> T fromModelProperty(EnrichableModel model,
                                                                  Class<P> modelPropertyType,
                                                                  Function<P, T> map,
                                                                  Supplier<? extends RuntimeException> exceptionSupplier) {
    return model.getModelProperty(modelPropertyType).map(map).orElseThrow(exceptionSupplier);
  }

  /**
   * @return the extension's error namespace for a given {@link ExtensionModel}
   */
  public static String getExtensionsErrorNamespace(ExtensionModel extensionModel) {
    return getExtensionsErrorNamespace(extensionModel.getXmlDslModel());
  }

  /**
   * @return the extension's error namespace for a given {@link ExtensionDeclaration}
   */
  public static String getExtensionsErrorNamespace(ExtensionDeclaration extensionDeclaration) {
    return getExtensionsErrorNamespace(extensionDeclaration.getXmlDslModel());
  }

  private static String getExtensionsErrorNamespace(XmlDslModel dslModel) {
    return dslModel.getPrefix().toUpperCase();
  }

  public static ExtensionModel loadExtension(Class<?> clazz) {
    return loadExtension(clazz, new HashMap<>());
  }

  public static ExtensionModel loadExtension(Class<?> clazz, Map<String, Object> params) {
    params.put(TYPE_PROPERTY_NAME, clazz.getName());
    params.put(VERSION, getProductVersion());
    // TODO MULE-11797: as this utils is consumed from
    // org.mule.runtime.module.extension.internal.capability.xml.schema.AbstractXmlResourceFactory.generateResource(org.mule.runtime.api.meta.model.ExtensionModel),
    // this util should get dropped once the ticket gets implemented.
    final DslResolvingContext dslResolvingContext = getDefault(emptySet());
    return new DefaultJavaExtensionModelLoader().loadExtensionModel(clazz.getClassLoader(), dslResolvingContext, params);
  }

  public static String getImplicitConfigurationProviderName(ExtensionModel extensionModel,
                                                            ConfigurationModel implicitConfigurationModel) {
    return format("%s-%s-implicit", extensionModel.getName(), implicitConfigurationModel.getName());
  }

}
