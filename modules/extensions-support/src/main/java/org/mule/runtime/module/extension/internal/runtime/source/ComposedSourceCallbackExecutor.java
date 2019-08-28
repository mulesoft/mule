/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.util.Map;

/**
 * A {@link SourceCallbackExecutor} that allows chain the execution of two
 * delegating {@link SourceCallbackExecutor}s.
 *
 * @since 4.1
 */
public class ComposedSourceCallbackExecutor implements SourceCallbackExecutor {

  private final SourceCallbackExecutor first;
  private final SourceCallbackExecutor then;

  ComposedSourceCallbackExecutor(SourceCallbackExecutor first, SourceCallbackExecutor then) {
    this.first = first;
    this.then = then;
  }

  @Override
  public void execute(CoreEvent event,
                      Map<String, Object> parameters,
                      SourceCallbackContext context,
                      CompletableCallback<Void> callback) {
    first.execute(event, parameters, context, new CompletableCallback<Void>() {

      @Override
      public void complete(Void value) {
        then.execute(event, parameters, context, callback);
      }

      @Override
      public void error(Throwable e) {
        callback.error(e);
      }
    });
  }
}
