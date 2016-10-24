/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.runtime.api.metadata.MetadataResolvingException;

/**
 * Contract for extension components that knows how to resolve the value of the Metadata Key ID
 * parameter
 *
 * @since 4.0
 */
public interface MetadataKeyObjectResolver {

  /**
   * @return The value of the parameter considered as MetadataKeyId
   * @throws MetadataResolvingException if the resolution fails
   */
  Object getMetadataKeyValue() throws MetadataResolvingException;
}
