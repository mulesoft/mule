/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.streaming;

import org.mule.runtime.core.streaming.bytes.ByteStreamingManager;

/**
 * Manages resources dedicated to perform streaming of bytes or objects, so that the runtime can keep track of them,
 * enforce policies and make sure that all resources are reclaimed once no longer needed.
 *
 * @since 4.0
 */
public interface StreamingManager {

  /**
   * @return a delegate manager to be used when streaming bytes
   */
  ByteStreamingManager forBytes();

}
