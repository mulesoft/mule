/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * An {@link ArgumentResolver} which returns the {@link MediaType} of the current message
 *
 * @since 4.0
 */
public final class MediaTypeArgumentResolver implements ArgumentResolver<MediaType> {

  @Override
  public MediaType resolve(ExecutionContext executionContext) {
    return ((ExecutionContextAdapter) executionContext).getEvent().getMessage().getPayload().getDataType().getMediaType();
  }
}
