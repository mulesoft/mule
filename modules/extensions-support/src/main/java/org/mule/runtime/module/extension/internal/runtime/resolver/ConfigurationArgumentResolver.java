/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;

/**
 * An implementation of {@link ArgumentResolver} which returns the value obtained through
 * {@link ExecutionContext#getConfiguration()}
 *
 * @since 3.7.1
 */
public final class ConfigurationArgumentResolver implements ArgumentResolver<Object> {

  @Override
  public Object resolve(ExecutionContext executionContext) {
    return ((ConfigurationInstance) executionContext.getConfiguration().get()).getValue();
  }
}
