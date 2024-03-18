/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import org.mule.api.annotation.NoImplement;

/**
 * Provides information about a processing unit represented by this instance
 */
@NoImplement
public interface ProcessingDescriptor {

  /**
   * Whether the processing represented by this instance is synchronous or not
   */
  boolean isSynchronous();

}
