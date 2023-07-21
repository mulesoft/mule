/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
