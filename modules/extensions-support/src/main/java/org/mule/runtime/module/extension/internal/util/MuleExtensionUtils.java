/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_ALWAYS_JOIN;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_JOIN_IF_POSSIBLE;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_NOT_SUPPORTED;
import static org.mule.runtime.core.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.springframework.util.ReflectionUtils.setField;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.internal.metadata.NullMetadataResolverFactory;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalSourceModelDefinitionException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationFactory;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.model.property.ClassLoaderModelProperty;
import org.mule.runtime.extension.api.model.property.ConnectivityModelProperty;
import org.mule.runtime.extension.api.runtime.InterceptorFactory;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutorFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.SourceFactory;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.runtime.module.extension.internal.model.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ConnectionProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ConnectionTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.InterceptorsModelProperty;
import org.mule.runtime.module.extension.internal.model.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.model.property.NullSafeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.OperationExecutorModelProperty;
import org.mule.runtime.module.extension.internal.model.property.RequireNameField;
import org.mule.runtime.module.extension.internal.model.property.SourceFactoryModelProperty;
import org.mule.runtime.module.extension.internal.runtime.execution.OperationExecutorFactoryWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utilities for handling {@link ExtensionModel extensions}
 *
 * @since 3.7.0
 */
public class MuleExtensionUtils {

  /**
   * Transforms the given {@code result} into a {@link Message}
   *
   * @param result a {@link Result} object
   * @return a {@link Message}
   */
  public static Message toMessage(Result result) {
    return toMessage(result, (MediaType) result.getMediaType().orElse(ANY));
  }

  /**
   * Transforms the given {@code result} into a {@link Message}
   *
   * @param result    a {@link Result} object
   * @param mediaType the {@link MediaType} for the message payload
   * @return a {@link Message}
   */
  public static Message toMessage(Result result, MediaType mediaType) {
    return Message.builder()
        .payload(result.getOutput())
        .mediaType(mediaType)
        .attributes((Attributes) result.getAttributes().orElse(NULL_ATTRIBUTES))
        .build();
  }

  /**
   * Returns {@code true} if any of the items in {@code resolvers} return true for the {@link ValueResolver#isDynamic()} method
   *
   * @param resolvers a {@link Iterable} with instances of {@link ValueResolver}
   * @param <T>       the generic type of the {@link ValueResolver} items
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
    return parameterModel.getModelProperties().stream().anyMatch(p -> p instanceof NullSafeModelProperty);
  }

  /**
   * Returns all the {@link ConnectionProviderModel} instances available for the given {@code configurationModel} plus the ones
   * globally defined at the {@code extensionModel}.
   * The {@link List} will first contain those defined at a {@link ConfigurationModel#getConnectionProviders()} level and finally the
   * ones at {@link ExtensionModel#getConnectionProviders()}
   *
   * @param extensionModel     the {@link ExtensionModel} which owns the {@code configurationModel}
   * @param configurationModel a {@link ConfigurationModel}
   * @return a {@link List}. Might be empty but will never be {@code null}
   */
  public static List<ConnectionProviderModel> getAllConnectionProviders(ExtensionModel extensionModel,
                                                                        ConfigurationModel configurationModel) {
    return ImmutableList.<ConnectionProviderModel>builder().addAll(configurationModel.getConnectionProviders())
        .addAll(extensionModel.getConnectionProviders()).build();
  }

  /**
   * Sorts the given {@code list} in ascending alphabetic order, using {@link NamedObject#getName()} as the sorting criteria
   *
   * @param list a {@link List} with instances of {@link NamedObject}
   * @param <T>  the generic type of the items in the {@code list}
   * @return the sorted {@code list}
   */
  public static <T extends NamedObject> List<T> alphaSortDescribedList(List<T> list) {
    if (isEmpty(list)) {
      return list;
    }

    Collections.sort(list, new NamedObjectComparator());
    return list;
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
   * @param declaration        a {@link BaseDeclaration}
   * @param interceptorFactory a {@link InterceptorFactory}
   */
  public static void addInterceptorFactory(BaseDeclaration declaration, InterceptorFactory interceptorFactory) {
    getOrCreateInterceptorModelProperty(declaration).addInterceptorFactory(interceptorFactory);
  }

  /**
   * Adds the given {@code interceptorFactory} to the {@code declaration} at the given {@code position}
   *
   * @param declaration        a {@link BaseDeclaration}
   * @param interceptorFactory a {@link InterceptorFactory}
   * @param position           a valid list index
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

    return interceptorFactories.stream().map(InterceptorFactory::createInterceptor).collect(new ImmutableListCollector<>());
  }

  /**
   * Returns the default value associated with the given annotation.
   * <p>
   * The reason for this method to be instead of simply using {@link Optional#defaultValue()} is a limitation on the Java language
   * to have an annotation which defaults to a {@code null} value. For that reason, this method tests the default value for
   * equality against the {@link Optional#NULL}. If such test is positive, then {@code null} is returned.
   * <p>
   * If a {@code null} {@code optional} is supplied, then this method returns {@code null}
   *
   * @param optional a nullable annotation
   * @return the default value associated to the annotation or {@code null}
   */
  public static String getDefaultValue(Optional optional) {
    if (optional == null) {
      return null;
    }

    String defaultValue = optional.defaultValue();
    return Optional.NULL.equals(defaultValue) ? null : defaultValue;
  }

  /**
   * Tests the given {@code object} to be annotated with {@link Optional}.
   * <p>
   * If the annotation is present, then a default value is extracted by the rules of {@link #getDefaultValue(Optional)}.
   * Otherwise, {@code null} is returned.
   * <p>
   * Notice that a {@code null} return value doesn't necessarily mean that the annotation is not present. It could well be that
   * {@code null} happens to be the default value.
   *
   * @param object an object potentially annotated with {@link Optional}
   * @return A default value or {@code null}
   */
  public static Object getDefaultValue(AccessibleObject object) {
    return getDefaultValue(object.getAnnotation(Optional.class));
  }

  public static Event getInitialiserEvent(MuleContext muleContext) {
    FlowConstruct flowConstruct = new FlowConstruct() {
      // TODO MULE-9076: This is only needed because the muleContext is get from the given flow.

      @Override
      public MuleContext getMuleContext() {
        return muleContext;
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
      public FlowConstructStatistics getStatistics() {
        return null;
      }
    };
    return Event.builder(create(flowConstruct, "InitializerEvent")).message(InternalMessage.builder().nullPayload().build())
        .exchangePattern(REQUEST_RESPONSE).flow(flowConstruct).build();
  }

  /**
   * Returns the {@link Method} that was used to declare the given {@code operationDeclaration}.
   *
   * @param operationDeclaration a {@link OperationDeclaration}
   * @return A {@link Method} or {@code null} if the {@code operationDeclaration} was defined by other means
   */
  public static Method getImplementingMethod(OperationDeclaration operationDeclaration) {
    return operationDeclaration.getModelProperty(ImplementingMethodModelProperty.class)
        .map(ImplementingMethodModelProperty::getMethod).orElse(null);
  }

  /**
   * If the {@code extensionModel} contains a {@link ClassLoaderModelProperty}, then it returns the {@link ClassLoader} associated
   * to such property.
   *
   * @param extensionModel a {@link ExtensionModel}
   * @return a {@link ClassLoader}
   * @throws IllegalModelDefinitionException if no {@link ClassLoaderModelProperty} is set on the extension
   */
  public static ClassLoader getClassLoader(ExtensionModel extensionModel) {
    return extensionModel.getModelProperty(ClassLoaderModelProperty.class).map(ClassLoaderModelProperty::getClassLoader)
        .orElseThrow(() -> noClassLoaderException(extensionModel.getName()));
  }

  /**
   * Executes the given {@code callable} using the {@link ClassLoader} associated to the {@code extensionModel}
   *
   * @param extensionModel a {@link ExtensionModel}
   * @param callable       a {@link Callable}
   * @param <T>            the generic type of the {@code callable}'s return type
   * @return the value returned by the {@code callable}
   * @throws Exception if the {@code callable} fails to execute
   */
  public static <T> T withExtensionClassLoader(ExtensionModel extensionModel, Callable<T> callable) throws Exception {
    return withContextClassLoader(getClassLoader(extensionModel), callable);
  }

  public static void injectConfigName(EnrichableModel model, Object target, String configName) {
    model.getModelProperty(RequireNameField.class).ifPresent(property -> {
      final Field configNameField = property.getConfigNameField();

      if (!configNameField.getDeclaringClass().isInstance(target)) {
        throw new IllegalConfigurationModelDefinitionException(
                                                               format("field '%s' is annotated with @%s but not defined on an instance of type '%s'",
                                                                      configNameField.toString(),
                                                                      ConfigName.class.getSimpleName(),
                                                                      target.getClass().getName()));
      }

      configNameField.setAccessible(true);
      setField(configNameField, target, configName);
    });
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

  public static boolean isTransactional(ComponentModel operationModel) {
    return operationModel
        .getModelProperty(ConnectivityModelProperty.class)
        .map(ConnectivityModelProperty::supportsTransactions)
        .orElse(false);
  }

  /**
   * Tests the {@code configurationModel} for a {@link ConfigurationFactoryModelProperty} and
   * returns the contained {@link ConfigurationFactory}.
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
   * Tests the given {@code model} for a {@link MetadataResolverFactoryModelProperty} and if present
   * it returns the contained {@link MetadataResolverFactory}. If no such property is found, then
   * a {@link NullMetadataResolverFactory} is returned
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
   * Tests the given {@code operationModel} for a {@link OperationExecutorModelProperty} and if present
   * it returns the enclosed {@link OperationExecutorFactory}. If no such property is found, then a
   * {@link IllegalOperationModelDefinitionException} is thrown.
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
   * Tests the given {@code sourceModel} for a {@link SourceFactoryModelProperty} and if present
   * it returns the enclosed {@link SourceFactory}. If no such property is found, then a
   * {@link IllegalSourceModelDefinitionException} is thrown
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
   * Tests the given {@code connectionProviderModel} for a {@link ConnectionProviderFactoryModelProperty} and if present
   * it returns the enclosed {@link ConnectionProviderFactory}. If no such property is found, then a
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
   * Tests the given {@code connectionProviderModel} for a {@link ConnectionTypeModelProperty} and if present
   * it returns the enclosed connection type. If no such property is found, then a
   * {@link IllegalConnectionProviderModelDefinitionException} is thrown
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
   * Creates an exception that says that no {@link ClassLoader} was specified for the extension of the given {@code extensionName}
   *
   * @param extensionName the name of the offending extension
   * @return an {@link IllegalModelDefinitionException}
   */
  public static IllegalModelDefinitionException noClassLoaderException(String extensionName) {
    return new IllegalModelDefinitionException("No ClassLoader was specified for extension " + extensionName);
  }

  private static class NamedObjectComparator implements Comparator<NamedObject> {

    @Override
    public int compare(NamedObject o1, NamedObject o2) {
      return o1.getName().compareTo(o2.getName());
    }
  }
}
