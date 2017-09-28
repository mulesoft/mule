/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories.streaming;

import org.mule.runtime.config.api.factories.streaming.AbstractCursorProviderObjectFactory;
import org.mule.runtime.core.api.streaming.object.CursorIteratorProviderFactory;

public class NullCursorIteratorProviderObjectFactory extends AbstractCursorProviderObjectFactory<CursorIteratorProviderFactory> {

  @Override
  public CursorIteratorProviderFactory doGetObject() throws Exception {
    return streamingManager.forObjects().getNullCursorProviderFactory();
  }
}
