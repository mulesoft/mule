/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.function;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.FunctionParameter;

/**
 * Factory definition for creating {@link FunctionParameter.DefaultValueResolver}s
 *
 * @since 4.0
 */
@FunctionalInterface
public interface FunctionParameterDefaultValueResolverFactory {

  FunctionParameter.DefaultValueResolver create(Object defaultValue, DataType type);
}
