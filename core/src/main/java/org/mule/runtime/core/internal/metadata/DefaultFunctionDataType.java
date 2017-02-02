/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.List;
import java.util.Optional;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.FunctionDataType;
import org.mule.runtime.api.metadata.FunctionParameter;
import org.mule.runtime.api.metadata.MediaType;

/**
 * Basic implementation of {@link FunctionDataType}.
 *
 * @since 4.0
 */
public class DefaultFunctionDataType extends SimpleDataType implements FunctionDataType {

  private List<FunctionParameter> parameters;
  private Optional<DataType> returnType;

  public DefaultFunctionDataType(Class<?> type, DataType returnType, List<FunctionParameter> parameters, MediaType mediaType,
                                 boolean consumable) {
    super(type, mediaType, consumable);
    this.parameters = unmodifiableList(parameters);
    this.returnType = returnType != null ? of(returnType) : empty();
  }

  @Override
  public List<FunctionParameter> getParameters() {
    return parameters;
  }

  @Override
  public Optional<DataType> getReturnType() {
    return returnType;
  }
}
