/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.java.utils.ResolvedMinMuleVersion;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.InputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.MetadataKeyModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.OutputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.RoutesChainInputTypesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.metadata.ScopeChainInputTypeResolverModelParser;

import java.util.List;
import java.util.Optional;

/**
 * Parses the syntactic definition of a {@link OperationModel} so that the semantics reflected in it can be extracted in a uniform
 * way, regardless of the actual syntax used by the extension developer.
 *
 * @see ExtensionModelParser
 * @since 4.5.0
 */
public interface OperationModelParser extends SemanticTermsParser, AdditionalPropertiesModelParser, StereotypeModelParser,
    NotificationEmitterParser, ComponentVisibilityParser {

  /**
   * @return the operation's name
   */
  String getName();

  /**
   * @return the operation's description
   */
  String getDescription();

  /**
   * @return an {@link OutputModelParser} describing the operation's output value
   */
  OutputModelParser getOutputType();

  /**
   * @return an {@link OutputModelParser} describing the operation's output attributes
   */
  OutputModelParser getAttributesOutputType();

  /**
   * Returns a list with a {@link ParameterGroupModelParser} per each parameter group defined in the operation. Each group is
   * listed in the same order as defined in the syntax.
   *
   * @return a list with the config's {@link ParameterGroupModelParser}
   */
  List<ParameterGroupModelParser> getParameterGroupModelParsers();

  /**
   * Returns a list with a {@link NestedRouteModelParser} per each route defined in the operation.
   * <p>
   * The Routes are listed in the same order as defined in the syntax.
   * <p>
   * This list will only be populated when {@link #isRouter()} returns {@code true}. The list will be empty otherwise.
   *
   * @return a list with the operation's {@link NestedRouteModelParser}
   */
  List<NestedRouteModelParser> getNestedRouteParsers();

  /**
   * Returns the {@link CompletableComponentExecutorModelProperty} which will be used to create the
   * {@link CompletableComponentExecutor} that brings the operation to life.
   *
   * @return a {@link CompletableComponentExecutorModelProperty}
   */
  Optional<CompletableComponentExecutorModelProperty> getExecutorModelProperty();

  /**
   * Returns an {@link NestedChainModelParser} if the operation defined one.
   * <p>
   * The value will be present only if {@link #isScope()} returns {@code true}
   *
   * @return an {@link Optional} {@link NestedChainModelParser}
   */
  Optional<NestedChainModelParser> getNestedChainParser();

  /**
   * @return whether the operation is blocking or non-blocking
   */
  boolean isBlocking();

  /**
   * @return whether this operation should be ignored and excluded from the resulting {@link ExtensionModel}. If the operation is
   *         ignored there is no guarantee for it to be valid, no other parser method should be called if that is the case.
   */
  boolean isIgnored();

  /**
   * @return whether this operation represents a scope operation
   */
  boolean isScope();

  /**
   * @return whether this operation represents a router
   */
  boolean isRouter();

  /**
   * @return whether this operation requires access to a connection to function
   */
  boolean isConnected();

  /**
   * @return whether this operation requires a configuration object to function
   */
  boolean hasConfig();

  /**
   * @return whether this operation supports returning streamable payloads.
   */
  boolean supportsStreaming();

  /**
   * @return whether this operation supports executing as part of a transaction
   */
  boolean isTransactional();

  /**
   * Returns whether this operation supports auto paging.
   * <p>
   * If this method returns {@code true}, so will {@link #supportsStreaming()}
   *
   * @return whether this operation supports auto paging.
   */
  boolean isAutoPaging();

  /**
   * Returns whether this operation supports configuring its streaming.
   *
   * @since 4.8
   */
  boolean hasStreamingConfiguration();

  /**
   * Returns whether this operation supports configuring its transactional action.
   *
   * @since 4.8
   */
  boolean hasTransactionalAction();

  /**
   * Returns whether this operation supports configuring its reconnection strategy.
   *
   * @since 4.8
   */
  boolean hasReconnectionStrategy();

  /**
   * Returns whether this operation propagates connectivity errors.
   *
   * @since 4.8
   */
  boolean propagatesConnectivityError();

  /**
   * @return the operation's {@link ExecutionType} if one was defined. If no value present, Mule will infer one.
   */
  Optional<ExecutionType> getExecutionType();

  /**
   * @return a {@link MediaTypeModelProperty} describing the operation's output mimeType, if one was defined
   */
  Optional<MediaTypeModelProperty> getMediaTypeModelProperty();

  /**
   * @return an {@link Optional} {@link ExceptionHandlerModelProperty} is an exception handler was defined for this operation.
   */
  Optional<ExceptionHandlerModelProperty> getExceptionHandlerModelProperty();

  /**
   * @return the operation's {@link DeprecationModel} if one was defined
   */
  Optional<DeprecationModel> getDeprecationModel();

  /**
   * @return the operation's {@link DisplayModel}
   */
  Optional<DisplayModel> getDisplayModel();


  /**
   * @return a list with an {@link ErrorModelParser} per each error that the operation can raise.
   */
  List<ErrorModelParser> getErrorModelParsers();

  /**
   * @return a {@link ResolvedMinMuleVersion} that contains the minimum mule version this component can run on and the reason why
   *         that version was assigned.
   */
  Optional<ResolvedMinMuleVersion> getResolvedMinMuleVersion();

  /**
   * @return an {@link Optional} {@link OutputResolverModelParser} that encapsulates the operation's output resolver if dynamic
   *         metadata were defined
   */
  Optional<OutputResolverModelParser> getOutputResolverModelParser();

  /**
   * @return an {@link Optional} {@link AttributesResolverModelParser} that encapsulates the operation's attribute resolver if
   *         dynamic metadata were defined
   */
  Optional<AttributesResolverModelParser> getAttributesResolverModelParser();

  /**
   * @return a {@link List} of {@link InputResolverModelParser} that encapsulates the operation's input resolvers if dynamic
   *         metadata were defined
   */
  List<InputResolverModelParser> getInputResolverModelParsers();

  /**
   * @return an {@link Optional} {@link MetadataKeyModelParser} that encapsulates the operation's key id resolver if dynamic
   *         metadata were defined
   */
  Optional<MetadataKeyModelParser> getMetadataKeyModelParser();

  /**
   * @return a {@link ScopeChainInputTypeResolverModelParser} if the parsed operation is a scope
   * @since 4.7.0
   */
  Optional<ScopeChainInputTypeResolverModelParser> getScopeChainInputTypeResolverModelParser();

  /**
   * @return a {@link RoutesChainInputTypesResolverModelParser} if the parsed operation is a router
   * @since 4.7.0
   */
  Optional<RoutesChainInputTypesResolverModelParser> getRoutesChainInputTypesResolverModelParser();

}
