/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SdkSourceFactoryModelProperty;

import java.util.List;
import java.util.Optional;

public interface SourceModelParser {

  String getName();

  String getDescription();

  OutputModelParser getOutputType();

  OutputModelParser getAttributesOutputType();

  Optional<SourceCallbackModelParser> getOnSuccessCallbackParser();

  Optional<SourceCallbackModelParser> getOnErrorCallbackParser();

  Optional<SourceCallbackModelParser> getOnTerminateCallbackParser();

  Optional<SourceCallbackModelParser> getOnBackPressureCallbackParser();

  List<ParameterGroupModelParser> getParameterGroupModelParsers();

  SdkSourceFactoryModelProperty getSourceFactoryModelProperty();

  boolean emitsResponse();

  boolean runsOnPrimaryNodeOnly();

  boolean isIgnored();

  boolean isConnected();

  boolean hasConfig();

  boolean supportsStreaming();

  boolean isTransactional();

  Optional<MediaTypeModelProperty> getMediaTypeModelProperty();

  Optional<ExceptionHandlerModelProperty> getExceptionHandlerModelProperty();

  List<ModelProperty> getAdditionalModelProperties();

  interface SourceCallbackModelParser {

    List<ParameterGroupModelParser> getParameterGroupModelParsers();

  }
}
