/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

/**
 * A factory to create a {@link SourceCompletionHandler} associated to a {@link SourceCallbackContext}
 *
 * @since 4.0
 */
@FunctionalInterface
public interface SourceCompletionHandlerFactory {

  /**
   * Creates a new {@link SourceCompletionHandler}
   *
   * @param context a {@link SourceCallbackContext}
   * @return a new {@link SourceCompletionHandler}
   */
  SourceCompletionHandler createCompletionHandler(SourceCallbackContextAdapter context);
}
