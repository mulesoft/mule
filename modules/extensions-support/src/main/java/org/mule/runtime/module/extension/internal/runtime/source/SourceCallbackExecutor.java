/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;

import java.util.Map;

/**
 * Invokes a {@link Source} callback
 *
 * @since 4.0
 */
@FunctionalInterface
interface SourceCallbackExecutor {

  /**
   * Executes the callback
   *
   * @param event      the result {@link CoreEvent}
   * @param parameters the callback's parameters
   * @param context    a {@link SourceCallbackContext} @return the callback's result
   * @param callback   the callback on which the result is to be signaled
   */
  void execute(CoreEvent event,
               Map<String, Object> parameters,
               SourceCallbackContext context,
               CompletableCallback<Void> callback);

}
