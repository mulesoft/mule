/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static reactor.core.publisher.Mono.empty;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.util.Map;

import org.reactivestreams.Publisher;

/**
 * Null object implementation of {@link SourceCallbackExecutor}
 *
 * @since 4.0
 */
public class NullSourceCallbackExecutor implements SourceCallbackExecutor {

  /**
   * @return {@code null}
   */
  @Override
  public Publisher<Void> execute(CoreEvent event, Map<String, Object> parameters, SourceCallbackContext context) {
    return empty();
  }
}
