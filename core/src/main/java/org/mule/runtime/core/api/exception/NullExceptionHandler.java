/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.exception;

import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

/**
 * Null {@link FlowExceptionHandler} which can be used to configure a {@link MessageProcessorChain} to not handle errors. This
 * should be the case when error handling will be propagated and executed within another chain so that exceptions are not altered
 * in any way prior to that.
 *
 * @since 4.0
 */
public final class NullExceptionHandler extends BaseExceptionHandler {

  private static final NullExceptionHandler INSTANCE = new NullExceptionHandler();

  private NullExceptionHandler() {}

  public static FlowExceptionHandler getInstance() {
    return INSTANCE;
  }

  @Override
  protected void onError(Exception exception) {
    // Do nothing
  }

  @Override
  public String toString() {
    return NullExceptionHandler.class.getSimpleName();
  }
}
