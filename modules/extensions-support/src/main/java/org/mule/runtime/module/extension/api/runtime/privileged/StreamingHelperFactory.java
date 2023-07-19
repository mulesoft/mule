/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.api.runtime.privileged;

import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.runtime.module.extension.internal.runtime.resolver.StreamingHelperArgumentResolver;

/**
 * A factory which provides instances of {@link StreamingHelper}
 *
 * @since 4.1
 */
public class StreamingHelperFactory {

  public StreamingHelper resolve(ExecutionContext executionContext) {
    return new StreamingHelperArgumentResolver().resolve(executionContext);
  }
}
