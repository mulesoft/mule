/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.property.SourceClusterSupportModelProperty;
import org.mule.runtime.extension.api.runtime.source.SdkSourceFactory;
import org.mule.runtime.extension.api.property.BackPressureStrategyModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SdkSourceFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.java.utils.ResolvedMinMuleVersion;

import java.util.List;
import java.util.Optional;

/**
 * Parses the syntactic definition of a {@link SourceModel} so that the semantics reflected in it can be extracted in a uniform
 * way, regardless of the actual syntax used by the extension developer.
 *
 * @see ExtensionModelParser
 * @since 4.5.0
 */
public interface SourceModelParser extends SemanticTermsParser, StereotypeModelParser, AdditionalPropertiesModelParser,
    NotificationEmitterParser, ComponentVisibilityParser {

  /**
   * @return the source's name
   */
  String getName();

  /**
   * @return the source's description
   */
  String getDescription();

  /**
   * Returns a list with a {@link ParameterGroupModelParser} per each parameter group defined in the source. Each group is listed
   * in the same order as defined in the syntax.
   *
   * @return a list with the config's {@link ParameterGroupModelParser}
   */
  List<ParameterGroupModelParser> getParameterGroupModelParsers();

  /**
   * @return an {@link OutputModelParser} describing the operation's output value
   */
  OutputModelParser getOutputType();

  /**
   * @return an {@link OutputModelParser} describing the operation's output attributes
   */
  OutputModelParser getAttributesOutputType();

  /**
   * @return a {@link SourceCallbackModelParser} for the success callback, if one was defined
   */
  Optional<SourceCallbackModelParser> getOnSuccessCallbackParser();

  /**
   * @return a {@link SourceCallbackModelParser} for the error callback, if one was defined
   */
  Optional<SourceCallbackModelParser> getOnErrorCallbackParser();

  /**
   * @return a {@link SourceCallbackModelParser} for the terminate callback, if one was defined
   */
  Optional<SourceCallbackModelParser> getOnTerminateCallbackParser();

  /**
   * @return a {@link SourceCallbackModelParser} for the back pressure callback, if one was defined
   */
  Optional<SourceCallbackModelParser> getOnBackPressureCallbackParser();

  /**
   * Returns the {@link SdkSourceFactoryModelProperty} which will be used to create the {@link SdkSourceFactory} that brings the
   * operation to life.
   *
   * @return a {@link SdkSourceFactoryModelProperty}
   */
  Optional<SdkSourceFactoryModelProperty> getSourceFactoryModelProperty();

  /**
   * @return whether this source emits responses
   */
  boolean emitsResponse();

  /**
   * @return whether this source should be ignored and excluded from the resulting {@link ExtensionModel}. If the source is
   *         ignored there is no guarantee for it to be valid, no other parser method should be called if that is the case.
   */
  boolean isIgnored();

  /**
   * @return whether this source requires access to a connection to function
   */
  boolean isConnected();

  /**
   * @return whether this source requires access to a config to function
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
   * @return a {@link MediaTypeModelProperty} describing the source's output mimeType, if one was defined
   */
  Optional<MediaTypeModelProperty> getMediaTypeModelProperty();

  /**
   * @return an {@link Optional} {@link ExceptionHandlerModelProperty} is an exception handler was defined for this source.
   */
  Optional<ExceptionHandlerModelProperty> getExceptionHandlerModelProperty();

  /**
   * @return the source's {@link DeprecationModel} if one was defined
   */
  Optional<DeprecationModel> getDeprecationModel();

  /**
   * @return the source's {@link DisplayModel}
   */
  Optional<DisplayModel> getDisplayModel();

  /**
   * @return the back pressure support info for the source
   */
  Optional<BackPressureStrategyModelProperty> getBackPressureStrategyModelProperty();

  /**
   * @return the type of cluster support this source provides
   */
  SourceClusterSupportModelProperty getSourceClusterSupportModelProperty();

  /**
   * @return a {@link ResolvedMinMuleVersion} that contains the minimum mule version this component can run on and the reason why
   *         that version was assigned.
   */
  Optional<ResolvedMinMuleVersion> getResolvedMinMuleVersion();

  /**
   * Parses the syntactic definition of a {@link SourceCallbackModel} so that the semantics reflected in it can be extracted in a
   * uniform way, regardless of the actual syntax used by the extension developer.
   *
   * @see ExtensionModelParser
   * @see SourceCallbackModel
   * @since 4.5.0
   */
  interface SourceCallbackModelParser {

    /**
     * Returns a list with a {@link ParameterGroupModelParser} per each parameter group defined in the callback. Each group is
     * listed in the same order as defined in the syntax.
     *
     * @return a list with the config's {@link ParameterGroupModelParser}
     */
    List<ParameterGroupModelParser> getParameterGroupModelParsers();

  }
}
