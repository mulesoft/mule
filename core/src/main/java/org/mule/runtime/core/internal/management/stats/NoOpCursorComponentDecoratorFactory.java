/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;

import java.io.InputStream;
import java.util.Iterator;

class NoOpCursorComponentDecoratorFactory implements CursorComponentDecoratorFactory {

  public static final NoOpCursorComponentDecoratorFactory NO_OP_INSTANCE = new NoOpCursorComponentDecoratorFactory();

  private NoOpCursorComponentDecoratorFactory() {
    // Nothing to do
  }

  @Override
  public <T> Iterator<T> decorateInput(Iterator<T> decorated) {
    return decorated;
  }

  @Override
  public InputStream decorateInput(InputStream decorated) {
    return decorated;
  }

  @Override
  public <C, T> PagingProvider<C, T> decorateOutput(PagingProvider<C, T> decorated) {
    return decorated;
  }

  @Override
  public <T> Iterator<T> decorateOutput(Iterator<T> decorated) {
    return decorated;
  }

  @Override
  public InputStream decorateOutput(InputStream decorated) {
    return decorated;
  }

}
