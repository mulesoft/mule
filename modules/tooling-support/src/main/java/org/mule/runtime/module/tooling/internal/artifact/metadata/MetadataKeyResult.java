/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.metadata;

import org.mule.runtime.api.metadata.MetadataKey;

/**
 * Adds information regarding a {@link MetadataKey} whether it is complete (has all required parts) or not and in the case it is
 * not complete, the reason why.
 */
public class MetadataKeyResult {

  private final MetadataKey metadataKey;

  private final String partialReason;

  public MetadataKeyResult(MetadataKey key) {
    this(key, null);
  }

  public MetadataKeyResult(MetadataKey key, String partialReason) {
    this.metadataKey = key;
    this.partialReason = partialReason;
  }

  public MetadataKey getMetadataKey() {
    return metadataKey;
  }

  public String getPartialReason() {
    return partialReason;
  }

  public boolean isComplete() {
    return partialReason == null;
  }
}
