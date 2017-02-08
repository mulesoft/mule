/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.runtime.api.streaming.CursorStream;

/**
 * Adapter interface to expand the {@link CursorStream} API with functionality which should
 * not be in the public API.
 *
 * @since 4.0
 */
public abstract class CursorStreamAdapter extends CursorStream {

  /**
   * @return the provider which created this cursor
   */
  public abstract CursorStreamProviderAdapter getProvider();
}
