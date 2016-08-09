/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_ALWAYS_JOIN;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_JOIN_IF_POSSIBLE;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_NOT_SUPPORTED;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.REQUIRED;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.SUPPORTED;
import static org.springframework.util.ReflectionUtils.setField;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.annotation.param.ConfigName;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.connectivity.OperationTransactionalAction;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.EnrichableModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.InterceptableModel;
import org.mule.runtime.extension.api.introspection.Named;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.introspection.property.ClassLoaderModelProperty;
import org.mule.runtime.extension.api.introspection.source.SourceModel;
import org.mule.runtime.extension.api.runtime.InterceptorFactory;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.module.extension.internal.model.property.ConnectivityModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.RequireNameField;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Utilities for handling {@link ExtensionModel extensions}
 *
 * @since 3.7.0
 */
public class MuleExtensionUtils {

  private MuleExtensionUtils() {}

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
   * Collects the {@link ParameterModel parameters} from {@code model} which supports or requires expressions
   *
   * @param model a {@link ParameterizedModel}
   * @return a {@link List} of {@link ParameterModel}. Can be empty but will never be {@code null}
   */
  public static List<ParameterModel> getDynamicParameters(ParameterizedModel model) {
    return model.getParameterModels().stream().filter(parameter -> acceptsExpressions(parameter.getExpressionSupport()))
        .collect(toList());
  }

  /**
   * @param support a {@link ExpressionSupport}
   * @return Whether or not the given {@code support} is one which accepts or requires expressions
   */
  public static boolean acceptsExpressions(ExpressionSupport support) {
    return support == SUPPORTED || support == REQUIRED;
  }

  /**
   * Returns a {@link List} with all the {@link ComponentModel} available to the {@code configurationModel} which requires a
   * connection. This includes both {@link SourceModel} and {@link OperationModel}.
   *
   * @param configurationModel a {@link RuntimeConfigurationModel}
   * @return a {@link List} of {@link ComponentModel}. It might be empty but will never be {@code null}
   */
  public static List<ComponentModel> getConnectedComponents(RuntimeConfigurationModel configurationModel) {
    List<ComponentModel> connectedModels = new LinkedList<>();
    new IdempotentExtensionWalker() {

      @Override
      public void onOperation(OperationModel model) {
        collect(model);
      }

      @Override
      public void onSource(SourceModel model) {
        collect(model);
      }

      private void collect(EnrichableModel model) {
        if (MuleExtensionUtils.isConnected(model)) {
          connectedModels.add((ComponentModel) model);
        }
      }
    }.walk(configurationModel.getExtensionModel());

    return connectedModels;
  }

  /**
   * Returns all the {@link ConnectionProviderModel} instances available for the given {@code configurationModel}. The
   * {@link List} will first contain those defined at a {@link ConfigurationModel#getConnectionProviders()} level and finally the
   * ones at {@link ExtensionModel#getConnectionProviders()}
   *
   * @param configurationModel a {@link RuntimeConfigurationModel}
   * @return a {@link List}. Might be empty but will never be {@code null}
   */
  public static List<ConnectionProviderModel> getAllConnectionProviders(RuntimeConfigurationModel configurationModel) {
    return ImmutableList.<ConnectionProviderModel>builder().addAll(configurationModel.getConnectionProviders())
        .addAll(configurationModel.getExtensionModel().getConnectionProviders()).build();
  }

  /**
   * Sorts the given {@code list} in ascending alphabetic order, using {@link Named#getName()} as the sorting criteria
   *
   * @param list a {@link List} with instances of {@link Named}
   * @param <T> the generic type of the items in the {@code list}
   * @return the sorted {@code list}
   */
  public static <T extends Named> List<T> alphaSortDescribedList(List<T> list) {
    if (isEmpty(list)) {
      return list;
    }

    Collections.sort(list, new NamedComparator());
    return list;
  }

  /**
   * Creates a new {@link List} of {@link Interceptor interceptors} using the factories returned by
   * {@link InterceptableModel#getInterceptorFactories()}
   *
   * @param model the model on which {@link InterceptableModel#getInterceptorFactories()} is to be invoked
   * @return an immutable {@link List} with instances of {@link Interceptor}
   */
  public static List<Interceptor> createInterceptors(InterceptableModel model) {
    return createInterceptors(model.getInterceptorFactories());
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

  public static MuleEvent getInitialiserEvent(MuleContext muleContext) {
    return new DefaultMuleEvent(MuleMessage.builder().nullPayload().build(), REQUEST_RESPONSE, new FlowConstruct() {
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
    });
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
   * @param callable a {@link Callable}
   * @param <T> the generic type of the {@code callable}'s return type
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
        throw new IllegalConfigurationModelDefinitionException(String
            .format("field '%s' is annotated with @%s but not defined on an instance of type '%s'", configNameField.toString(),
                    ConfigName.class.getSimpleName(), target.getClass().getName()));
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

  public static boolean isTransactional(OperationModel operationModel) {
    return operationModel.getModelProperty(ConnectivityModelProperty.class).map(ConnectivityModelProperty::supportsTransactions)
        .orElse(false);
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

  private static boolean isConnected(EnrichableModel o) {
    return o.getModelProperty(ConnectivityModelProperty.class).isPresent();
  }

  private static class NamedComparator implements Comparator<Named> {

    @Override
    public int compare(Named o1, Named o2) {
      return o1.getName().compareTo(o2.getName());
    }
  }
}
