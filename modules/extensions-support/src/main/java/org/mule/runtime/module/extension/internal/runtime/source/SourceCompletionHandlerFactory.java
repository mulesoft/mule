/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.CompletionHandler;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

/**
 * A factory to create a {@link CompletionHandler} associated to a {@link SourceCallbackContext}
 *
 * @since 4.0
 */
@FunctionalInterface
public interface SourceCompletionHandlerFactory {

  /**
   * Creates a new {@link CompletionHandler}
   *
   * @param context a {@link SourceCallbackContext}
   * @return a new {@link CompletionHandler}
   */
  CompletionHandler<Event, MessagingException> createCompletionHandler(SourceCallbackContext context);
}
