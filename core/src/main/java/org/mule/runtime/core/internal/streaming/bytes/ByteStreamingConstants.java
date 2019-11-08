/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;
import static org.mule.runtime.api.util.DataUnit.KB;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_STREAMING_BUCKET_SIZE;

/**
 * Constants around byte streaming
 *
 * @since 4.1.4
 */
public final class ByteStreamingConstants {

  /**
   * The default size of a chunk/bucket for buffers which grow elastically
   */
  public static final int DEFAULT_BUFFER_BUCKET_SIZE = getDefaultBucketSize();

  private static int getDefaultBucketSize() {
    String bucketSize = getProperty(MULE_STREAMING_BUCKET_SIZE);
    return bucketSize != null ? valueOf(bucketSize) : KB.toBytes(8);
  }

  private ByteStreamingConstants() {}
}
