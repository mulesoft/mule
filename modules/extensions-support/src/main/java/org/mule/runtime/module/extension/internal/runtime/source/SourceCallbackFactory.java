/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.extension.api.runtime.source.SourceCallback;

/**
 * A factory to create {@link SourceCallback} instances
 */
@FunctionalInterface
public interface SourceCallbackFactory {

  /**
   * Creates a new {@link SourceCallback}
   *
   * @param completionHandlerFactory a {@link SourceCompletionHandlerFactory}
   * @return a new {@link SourceCallback}
   */
  SourceCallback createSourceCallback(SourceCompletionHandlerFactory completionHandlerFactory);
}
