/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.metadata;

import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
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
public class DefaultFunctionDataType extends SimpleDataType implements FunctionDataType {

  private static final long serialVersionUID = 5452095230956829108L;

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
