/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object.factory;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.StreamingManager;

import java.util.Iterator;

public class NullCursorIteratorProviderFactory extends AbstractCursorIteratorProviderFactory {

  public NullCursorIteratorProviderFactory(StreamingManager streamingManager) {
    super(streamingManager);
  }

  @Override
  protected Object resolve(Iterator iterator, CoreEvent event) {
    return iterator;
  }
}
