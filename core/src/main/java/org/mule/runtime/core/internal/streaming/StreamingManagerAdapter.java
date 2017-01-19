/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.streaming.StreamingManager;

/**
 * Adapter interface which extends the {@link StreamingManager} contract with behavior
 * which should not be exposed in the public API.
 *
 * @since 4.0
 */
public interface StreamingManagerAdapter extends StreamingManager {

  /**
   * Invoked when the given {@code event} has been successfully completed
   * @param event a completed {@link Event}
   */
  void success(Event event);

  /**
   * Invoked when the given {@code event} has been completed in error
   * @param event a completed {@link Event}
   */
  void error(Event event);

}
