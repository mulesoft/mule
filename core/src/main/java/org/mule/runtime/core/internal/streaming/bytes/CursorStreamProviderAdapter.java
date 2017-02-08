/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.api.streaming.CursorStreamProvider;
import org.mule.runtime.core.api.Event;

/**
 * An adapter interface which extends the {@link CursorStreamProvider} contract with
 * behaviour which should not be exposed in the public API.
 *
 * @since 4.0
 */
public interface CursorStreamProviderAdapter extends CursorStreamProvider {

  /**
   * Releases all the resources currently held by {@code this instance}. Invoking this
   * method will render all cursors opened by this provider useless.
   * <p>
   * Only the runtime should invoke this method.
   */
  void releaseResources();

  /**
   * @return The {@link Event} under which {@code this} instance was created
   */
  Event getCreatorEvent();
}
