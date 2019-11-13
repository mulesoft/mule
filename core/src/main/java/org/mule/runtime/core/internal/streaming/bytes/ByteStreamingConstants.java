/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.lang.Integer.getInteger;
import static java.lang.System.getProperty;
import static org.mule.runtime.api.util.DataUnit.KB;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_STREAMING_BUCKET_SIZE;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_STREAMING_BUFFER_POOL_SIZE;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_STREAMING_MAX_MEMORY_PERCENTAGE;

/**
 * Constants around byte streaming
 *
 * @since 4.1.4
 */
public final class ByteStreamingConstants {

  /**
   * The default size of a chunk/bucket for buffers which grow elastically
   */
  public static final int DEFAULT_BUFFER_BUCKET_SIZE = getInteger(MULE_STREAMING_BUCKET_SIZE, KB.toBytes(8));

  /**
   * A [0;1] percentage of how much of the total heap memory can be devoted to repeatable streaming buffers
   *
   * @since 4.3.0
   */
  public static final double MAX_STREAMING_MEMORY_PERCENTAGE = getMaxStreamingMemoryPercentage();

  /**
   * The max size for pools of buffers devoted to repeatable streaming
   *
   * @since 4.3.0
   */
  public static final int DEFAULT_BUFFER_POOL_SIZE = getInteger(MULE_STREAMING_BUFFER_POOL_SIZE, 2048);

  private static double getMaxStreamingMemoryPercentage() {
    String v = getProperty(MULE_STREAMING_MAX_MEMORY_PERCENTAGE);
    return v != null ? Double.valueOf(v) : 0.7;
  }

  private ByteStreamingConstants() {}
}
