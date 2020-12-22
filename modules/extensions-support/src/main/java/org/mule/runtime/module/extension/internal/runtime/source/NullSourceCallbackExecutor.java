/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;

import java.util.Map;

/**
 * Null object implementation of {@link SourceCallbackExecutor}
 *
 * @since 4.0
 */
public class NullSourceCallbackExecutor implements SourceCallbackExecutor {

  public static final NullSourceCallbackExecutor INSTANCE = new NullSourceCallbackExecutor();

  private NullSourceCallbackExecutor() {}

  @Override
  public void execute(CoreEvent event,
                      Map<String, Object> parameters,
                      SourceCallbackContext context,
                      CompletableCallback<Void> callback) {
    callback.complete(null);
  }
}
