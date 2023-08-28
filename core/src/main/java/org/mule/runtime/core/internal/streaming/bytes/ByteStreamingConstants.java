/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.lang.Integer.getInteger;
import static java.lang.System.getProperty;
import static org.mule.runtime.api.util.DataUnit.KB;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_STREAMING_BUCKET_SIZE;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_STREAMING_MAX_BUFFER_POOL_SIZE;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_STREAMING_MAX_HEAP_PERCENTAGE;

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
  public static final int DEFAULT_BUFFER_POOL_SIZE = getInteger(MULE_STREAMING_MAX_BUFFER_POOL_SIZE, 2048);

  private static double getMaxStreamingMemoryPercentage() {
    String v = getProperty(MULE_STREAMING_MAX_HEAP_PERCENTAGE);
    return v != null ? Double.valueOf(v) : 0.7;
  }

  private ByteStreamingConstants() {}
}
