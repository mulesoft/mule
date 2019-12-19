/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * An implementation of {@link ArgumentResolver} which returns the value obtained through
 * {@link ExecutionContext#getConfiguration()}
 *
 * @since 4.0
 */
public final class DefaultEncodingArgumentResolver implements ArgumentResolver<String> {

  @Override
  public String resolve(ExecutionContext executionContext) {
    return ((ExecutionContextAdapter) executionContext).getMuleContext().getConfiguration().getDefaultEncoding();
  }
}
