/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
