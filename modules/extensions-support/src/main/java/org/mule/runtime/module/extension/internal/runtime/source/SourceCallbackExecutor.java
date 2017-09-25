/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.util.Map;

import org.reactivestreams.Publisher;

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
   * @param parameters
   * @param context    a {@link SourceCallbackContext} @return the callback's result
   * @return a Publisher which completes either with no value or with an error
   */
  Publisher<Void> execute(CoreEvent event, Map<String, Object> parameters, SourceCallbackContext context);

}
