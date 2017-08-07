/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.metadata;

import org.mule.runtime.api.metadata.DefaultMetadataKey;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;

/**
 * Extension of {@link MetadataKeyBuilder} which adds de capability of create {@link DefaultMetadataKey} with a configured
 * {@code partName}
 *
 * @since 4.0
 */
public class MultilevelMetadataKeyBuilder extends MetadataKeyBuilder {

  private MultilevelMetadataKeyBuilder(String id, String partName) {
    super(id);
    setPartName(partName);
  }

  /**
   * Creates and returns new instance of a {@link MultilevelMetadataKeyBuilder}, to help building a new {@link MetadataKey}
   * represented by the given {@param id}
   *
   * @param id of the {@link MetadataKey} to be created
   * @return an initialized instance of {@link MultilevelMetadataKeyBuilder}
   */
  public static MultilevelMetadataKeyBuilder newKey(String id, String partName) {
    return new MultilevelMetadataKeyBuilder(id, partName);
  }
}
