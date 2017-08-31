/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.streaming.DefaultStreamingHelper;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;

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
  public StreamingHelper resolve(ExecutionContext executionContext) {
    ExecutionContextAdapter<OperationModel> context = (ExecutionContextAdapter<OperationModel>) executionContext;
    return new DefaultStreamingHelper(context.getCursorProviderFactory(), context.getStreamingManager(), context.getEvent());
  }
}
