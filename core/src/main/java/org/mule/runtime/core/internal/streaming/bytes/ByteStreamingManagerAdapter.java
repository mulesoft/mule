/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.core.streaming.bytes.ByteStreamingManager;

/**
 * Adapter interface to expand the {@link ByteStreamingManager} API with behavior which
 * should not be exposed
 *
 * @since 4.0
 */
public interface ByteStreamingManagerAdapter extends ByteStreamingManager {

  /**
   * Invoke this method each time a new {@link CursorStreamProviderAdapter} is created
   *
   * @param provider a new provider to be tracked
   */
  void onOpen(CursorStreamProviderAdapter provider);

  /**
   * Invoke this method each time a new {@link CursorStreamAdapter} is opened
   *
   * @param cursor the cursor to be tracked
   */
  void onOpen(CursorStreamAdapter cursor);

  /**
   * Invoke this method each time {@link CursorStreamAdapter#close()} is invoked
   *
   * @param cursor the cursor which was closed
   */
  void onClose(CursorStreamAdapter cursor);

}
