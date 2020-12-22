/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_ALWAYS_BEGIN;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_ALWAYS_JOIN;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_JOIN_IF_POSSIBLE;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_NONE;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_NOT_SUPPORTED;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.VERSION;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.internal.metadata.NullMetadataResolverFactory;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthModelProperty;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalSourceModelDefinitionException;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.property.ClassLoaderModelProperty;
import org.mule.runtime.extension.api.property.SyntheticModelModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationFactory;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutorFactory;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutorFactory;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.source.BackPressureAction;
import org.mule.runtime.extension.api.runtime.source.BackPressureMode;
import org.mule.runtime.extension.api.runtime.source.SdkSourceFactory;
import org.mule.runtime.extension.api.runtime.source.SourceFactory;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.runtime.extension.api.tx.SourceTransactionalAction;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.api.loader.java.property.ComponentExecutorModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.NullSafeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SdkSourceFactoryModelProperty;
import org.mule.runtime.module.extension.internal.runtime.config.MutableConfigurationStats;
import org.mule.runtime.module.extension.internal.runtime.execution.deprecated.ComponentExecutorCompletableAdapterFactory;
import org.mule.runtime.module.extension.internal.runtime.execution.deprecated.ReactiveOperationExecutorFactoryWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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
  public static boolean returnsListOfMessages(HasOutputModel componentModel) {
    MetadataType outputType = componentModel.getOutput().getType();
    return outputType instanceof ArrayType &&
        Message.class.getName().equals(getId(((ArrayType) outputType).getType()).orElse(null));
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


  public static Map<String, Object> toMap(ResolverSet resolverSet, ValueResolvingContext ctx) throws MuleException {
    ImmutableMap.Builder<String, Object> map = ImmutableMap.builder();
    for (Entry<String, ValueResolver<?>> entry : resolverSet.getResolvers().entrySet()) {
      Object value = entry.getValue().resolve(ctx);
      if (value != null) {
        map.put(entry.getKey(), value);
      }
    }

    return map.build();
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
   * @param callable       a {@link Callable}
   * @param <T>            the generic type of the {@code callable}'s return type
   * @return the value returned by the {@code callable}
   * @throws Exception if the {@code callable} fails to execute
   */
  public static <T> T withExtensionClassLoader(ExtensionModel extensionModel, Callable<T> callable) throws Exception {
    return withContextClassLoader(getClassLoader(extensionModel), callable);
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
   * @param mode a {@link BackPressureMode}
   * @return a {@link BackPressureStrategy}
   */
  public static BackPressureStrategy toBackPressureStrategy(BackPressureMode mode) {
    switch (mode) {
      case WAIT:
        return BackPressureStrategy.WAIT;
      case FAIL:
        return BackPressureStrategy.FAIL;
      case DROP:
        return BackPressureStrategy.DROP;
    }

    throw new IllegalArgumentException("Unmapped mode " + mode.name());
  }

  /**
   * @param backPressureModeName the name of a {@link BackPressureMode}
   * @return a {@link BackPressureStrategy}
   */
  public static BackPressureStrategy toBackPressureStrategy(String backPressureModeName) {
    return toBackPressureStrategy(BackPressureMode.valueOf(backPressureModeName));
  }

  public static Optional<BackPressureAction> toBackPressureAction(BackPressureStrategy backPressureStrategy) {
    if (backPressureStrategy == BackPressureStrategy.FAIL) {
      return Optional.of(BackPressureAction.FAIL);
    } else if (backPressureStrategy == BackPressureStrategy.DROP) {
      return Optional.of(BackPressureAction.DROP);
    } else if (backPressureStrategy == BackPressureStrategy.WAIT) {
      return empty();
    } else {
      throw new IllegalArgumentException("Unsupported BackPressureStrategy " + backPressureStrategy);
    }
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
   * Tests the given {@code operationModel} for a {@link ComponentExecutorModelProperty} and if present it returns the enclosed
   * {@link ComponentExecutorFactory}. If no such property is found, then a {@link IllegalOperationModelDefinitionException} is
   * thrown.
   *
   * @param operationModel an {@link OperationModel}
   * @return a {@link ComponentExecutorFactory}
   * @throws IllegalOperationModelDefinitionException if the operation is not properly enriched
   * @deprecated since 4.3. Use {@link #getOperationExecutorFactory(ComponentModel)} instead
   */
  @Deprecated
  public static <T extends ComponentModel> ComponentExecutorFactory<T> getLegacyOperationExecutorFactory(T operationModel) {
    ComponentExecutorFactory executorFactory =
        fromModelProperty(operationModel,
                          ComponentExecutorModelProperty.class,
                          ComponentExecutorModelProperty::getExecutorFactory,
                          () -> new IllegalOperationModelDefinitionException(format("Operation '%s' does not provide a %s",
                                                                                    operationModel.getName(),
                                                                                    ComponentExecutorFactory.class
                                                                                        .getSimpleName())));

    return new ReactiveOperationExecutorFactoryWrapper(executorFactory);
  }

  public static <T extends ComponentModel> CompletableComponentExecutorFactory<T> getOperationExecutorFactory(T operationModel) {
    if (operationModel.getModelProperty(ComponentExecutorModelProperty.class).isPresent()) {
      return new ComponentExecutorCompletableAdapterFactory<>(getLegacyOperationExecutorFactory(operationModel));
    }

    return fromModelProperty(operationModel,
                             CompletableComponentExecutorModelProperty.class,
                             CompletableComponentExecutorModelProperty::getExecutorFactory,
                             () -> new IllegalOperationModelDefinitionException(format("Operation '%s' does not provide a %s",
                                                                                       operationModel.getName(),
                                                                                       CompletableComponentExecutorModelProperty.class
                                                                                           .getSimpleName())));
  }

  public static boolean isNonBlocking(ComponentModel model) {
    if (model instanceof OperationModel) {
      return !((OperationModel) model).isBlocking();
    }

    return model instanceof ConstructModel;
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
   * Tests the given {@code sourceModel} for a {@link SdkSourceFactoryModelProperty} and if present it returns the enclosed
   * {@link SdkSourceFactory}. If no such property is found, then a {@link IllegalSourceModelDefinitionException} is thrown
   *
   * @param sourceModel a {@link SourceModel}
   * @return a {@link SourceFactory}
   * @throws IllegalSourceModelDefinitionException if the source is not properly enriched
   */
  public static SdkSourceFactory getSdkSourceFactory(SourceModel sourceModel) {
    return fromModelProperty(sourceModel,
                             SdkSourceFactoryModelProperty.class,
                             SdkSourceFactoryModelProperty::getMessageSourceFactory,
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
  public static Type getConnectionType(ConnectionProviderModel connectionProviderModel) {
    return fromModelProperty(connectionProviderModel,
                             ConnectionTypeModelProperty.class,
                             ConnectionTypeModelProperty::getConnectionTypeElement,
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
  public static String getExtensionsNamespace(ExtensionModel extensionModel) {
    return getExtensionsNamespace(extensionModel.getXmlDslModel());
  }

  /**
   * @return the extension's error namespace for a given {@link ExtensionDeclaration}
   */
  public static String getExtensionsNamespace(ExtensionDeclaration extensionDeclaration) {
    return getExtensionsNamespace(extensionDeclaration.getXmlDslModel());
  }

  private static String getExtensionsNamespace(XmlDslModel dslModel) {
    return dslModel.getPrefix().toUpperCase();
  }

  public static ExtensionModel loadExtension(Class<?> clazz) {
    return loadExtension(clazz, new SmallMap<>());
  }

  public static ExtensionModel loadExtension(Class<?> clazz, Map<String, Object> params) {
    params.put(TYPE_PROPERTY_NAME, clazz.getName());
    params.put(VERSION, "4.0.0-SNAPSHOT");
    final DslResolvingContext dslResolvingContext = getDefault(singleton(MuleExtensionModelProvider.getExtensionModel()));
    return new DefaultJavaExtensionModelLoader().loadExtensionModel(clazz.getClassLoader(), dslResolvingContext, params);
  }

  public static String getImplicitConfigurationProviderName(ExtensionModel extensionModel,
                                                            ConfigurationModel implicitConfigurationModel) {
    return format("%s-%s-implicit", extensionModel.getName(), implicitConfigurationModel.getName());
  }

  /**
   * TODO MULE-14603 - This should not exist after MULE-14603 is fixed
   * <p>
   * Indicates if the given value is considered as an expression
   *
   * @param value Value to check
   * @return a boolean indicating if the value is an expression or not.
   */
  public static boolean isExpression(Object value) {
    if (value instanceof String) {
      String trim = ((String) value).trim();
      return trim.startsWith(DEFAULT_EXPRESSION_PREFIX) && trim.endsWith(DEFAULT_EXPRESSION_POSTFIX);
    } else {
      return false;
    }
  }

  /**
   * Parse the given value and remove expression markers if it is considered as an expression.
   *
   * @param value Value to parse
   * @return a String containing the expression without markers or null if the value is not an expression.
   */
  public static Optional<String> extractExpression(Object value) {
    Optional<String> result = empty();
    if (isExpression(value)) {
      String expression = (String) value;
      if (isNotEmpty(expression)) {
        String trimmedText = expression.trim();
        result =
            of(trimmedText.substring(DEFAULT_EXPRESSION_PREFIX.length(),
                                     trimmedText.length() - DEFAULT_EXPRESSION_POSTFIX.length()));
      }
    }

    return result;
  }

  /**
   * Indicates if the extension model is being built on compile time.
   *
   * @param extensionModel the extension model to check
   * @return a boolean indicating if the the extension model is being build on runtime or not
   * @since 4.3.0
   */
  public static boolean isCompileTime(ExtensionModel extensionModel) {
    return extensionModel.getModelProperty(CompileTimeModelProperty.class).isPresent();
  }

  /**
   * Indicates whether a give {@link EnrichableModel} is synthetic or not.
   *
   * @param enrichableModel the enrichable model that may be synthetic
   * @return if the enrichable model is synthetic or not
   * @since 4.3.0
   */
  public static boolean isSynthetic(EnrichableModel enrichableModel) {
    return enrichableModel.getModelProperty(SyntheticModelModelProperty.class).isPresent();
  }

  /**
   * Tests the given {@code enrichableModel} for a {@link ImplementingTypeModelProperty} and if present it
   * returns the enclosed implementing type.
   *
   * @param enrichableModel the provider to get the implemented type from
   * @return an {@link Optional} that either has the provider implementing type, or is empty
   * @since 4.3.0
   */
  public static Optional<Class> getImplementingType(EnrichableModel enrichableModel) {
    return enrichableModel.getModelProperty(ImplementingTypeModelProperty.class).map(mp -> mp.getType());
  }

  /**
   * @return the {@link MutableConfigurationStats} for a given {@link ExecutionContext}
   *
   * @since 4.2.3 - 4.3.0
   */
  public static MutableConfigurationStats getMutableConfigurationStats(ExecutionContext<?> context) {
    if (context.getConfiguration().isPresent()) {
      return (MutableConfigurationStats) (context.getConfiguration().get()).getStatistics();
    } else {
      return null;
    }
  }

  /**
   * @return whether the {@link ComponentModel} defines a streaming operation that uses a connection.
   *
   * @since 4.2.3 - 4.3.0
   */
  public static boolean isConnectedStreamingOperation(ComponentModel componentModel) {
    if (componentModel instanceof ConnectableComponentModel) {
      ConnectableComponentModel connectableComponentModel = (ConnectableComponentModel) componentModel;
      return (connectableComponentModel.requiresConnection()
          && (connectableComponentModel.supportsStreaming()
              || connectableComponentModel.getModelProperty(PagedOperationModelProperty.class).isPresent()));
    }
    return false;
  }
}
