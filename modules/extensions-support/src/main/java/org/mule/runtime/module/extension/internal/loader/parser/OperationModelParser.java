/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.operation.ExecutionType;
import org.mule.runtime.module.extension.api.loader.java.property.CompletableComponentExecutorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;

import java.util.List;
import java.util.Optional;

public interface OperationModelParser {

  String getName();

  String getDescription();

  OutputModelParser getOutputType();

  OutputModelParser getAttributesOutputType();

  List<ParameterGroupModelParser> getParameterGroupModelParsers();

  List<NestedRouteModelParser> getNestedRouteParsers();

  CompletableComponentExecutorModelProperty getExecutorModelProperty();

  Optional<NestedChainModelParser> getNestedChainParser();

  boolean isBlocking();

  boolean isIgnored();

  boolean isScope();

  boolean isRouter();

  boolean isConnected();

  boolean hasConfig();

  boolean isNonBlocking();

  boolean supportsStreaming();

  boolean isTransactional();

  boolean isAutoPaging();

  Optional<ExecutionType> getExecutionType();

  Optional<MediaTypeModelProperty> getMediaTypeModelProperty();

  Optional<ExceptionHandlerModelProperty> getExceptionHandlerModelProperty();

  List<ModelProperty> getAdditionalModelProperties();
}
