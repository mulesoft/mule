/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories.streaming;

import org.mule.runtime.core.streaming.bytes.CursorStreamProviderFactory;

public class NullCursorStreamProviderObjectFactory
    extends AbstractCursorProviderObjectFactory<CursorStreamProviderFactory> {

  @Override
  public CursorStreamProviderFactory doGetObject() throws Exception {
    return streamingManager.forBytes().getNullCursorProviderFactory();
  }
}
