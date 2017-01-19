/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.List;

/**
 * Contains the output of a message source
 *
 * @since 4.0
 */
public class SourceResultAdapter {

  private final Result result;
  private final CursorStreamProviderFactory cursorStreamProviderFactory;
  private final boolean isCollection;

  /**
   * Creates a new instance
   *
   * @param result                      the source result
   * @param cursorStreamProviderFactory the {@link CursorStreamProviderFactory} used by the source
   * @param isCollection                whether the {@code result} represents a {@link List} of messages.
   */
  public SourceResultAdapter(Result result,
                             CursorStreamProviderFactory cursorStreamProviderFactory,
                             boolean isCollection) {
    this.result = result;
    this.cursorStreamProviderFactory = cursorStreamProviderFactory;
    this.isCollection = isCollection;
  }

  /**
   * @return The source {@link Result}
   */
  public Result getResult() {
    return result;
  }

  /**
   * @return The {@link CursorStreamProviderFactory} used by the source
   */
  public CursorStreamProviderFactory getCursorStreamProviderFactory() {
    return cursorStreamProviderFactory;
  }

  /**
   * @return Whether the {@link #getResult()} represents a {@link List} of messages.
   */
  public boolean isCollection() {
    return isCollection;
  }
}
