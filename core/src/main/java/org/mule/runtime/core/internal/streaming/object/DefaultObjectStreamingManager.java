/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import org.mule.runtime.core.internal.streaming.CursorManager;
import org.mule.runtime.core.internal.streaming.object.factory.InMemoryCursorIteratorProviderFactory;
import org.mule.runtime.core.internal.streaming.object.factory.NullCursorIteratorProviderFactory;
import org.mule.runtime.core.streaming.objects.CursorIteratorProviderFactory;
import org.mule.runtime.core.streaming.objects.InMemoryCursorIteratorConfig;
import org.mule.runtime.core.streaming.objects.ObjectStreamingManager;

/**
 * Default implementation of {@link ObjectStreamingManager}
 *
 * @since 4.0
 */
public class DefaultObjectStreamingManager implements ObjectStreamingManager {

  private final CursorManager cursorManager;

  public DefaultObjectStreamingManager(CursorManager cursorManager) {
    this.cursorManager = cursorManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorIteratorProviderFactory getInMemoryCursorProviderFactory(InMemoryCursorIteratorConfig config) {
    return new InMemoryCursorIteratorProviderFactory(cursorManager, config);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorIteratorProviderFactory getNullCursorProviderFactory() {
    return new NullCursorIteratorProviderFactory(cursorManager);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CursorIteratorProviderFactory getDefaultCursorProviderFactory() {
    return getInMemoryCursorProviderFactory(InMemoryCursorIteratorConfig.getDefault());
  }
}
