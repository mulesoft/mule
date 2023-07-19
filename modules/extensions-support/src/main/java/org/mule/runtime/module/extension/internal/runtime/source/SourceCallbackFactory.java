/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.source;


import org.mule.sdk.api.runtime.source.SourceCallback;

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
