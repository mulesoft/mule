/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.factories.streaming;

import org.mule.runtime.config.api.factories.streaming.AbstractCursorProviderObjectFactory;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;

public class NullCursorStreamProviderObjectFactory
    extends AbstractCursorProviderObjectFactory<CursorStreamProviderFactory> {

  @Override
  public CursorStreamProviderFactory doGetObject() throws Exception {
    return streamingManager.forBytes().getNullCursorProviderFactory();
  }
}
