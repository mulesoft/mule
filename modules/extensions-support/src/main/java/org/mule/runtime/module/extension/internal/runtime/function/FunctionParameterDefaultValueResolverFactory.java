/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
