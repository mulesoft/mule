/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.streaming.DefaultStreamingHelper;

/**
 * An argument resolver which provides instances of {@link StreamingHelper}
 *
 * @since 4.0
 */
public class StreamingHelperArgumentResolver implements ArgumentResolver<StreamingHelper> {

  /**
   * {@inheritDoc}
   */
  @Override
  public LazyValue<StreamingHelper> resolve(ExecutionContext executionContext) {
    return new LazyValue<>(() -> {
      ExecutionContextAdapter<OperationModel> context = (ExecutionContextAdapter<OperationModel>) executionContext;
      return new DefaultStreamingHelper(context.getCursorProviderFactory(), context.getStreamingManager(), context.getEvent());
    });
  }
}
