/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.metadata;

import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.FunctionDataType;
import org.mule.runtime.api.metadata.FunctionParameter;
import org.mule.runtime.api.metadata.MediaType;

import java.util.List;
import java.util.Optional;

/**
 * Basic implementation of {@link FunctionDataType}.
 *
 * @since 4.0
 */
@NoExtend
public class DefaultFunctionDataType extends SimpleDataType implements FunctionDataType {

  private static final long serialVersionUID = 5452095230956829108L;

  private final List<FunctionParameter> parameters;
  private final Optional<DataType> returnType;

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
