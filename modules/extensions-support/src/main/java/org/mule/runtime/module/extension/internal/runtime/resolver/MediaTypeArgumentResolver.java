/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
