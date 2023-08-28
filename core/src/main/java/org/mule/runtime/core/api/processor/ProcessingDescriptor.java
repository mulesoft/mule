/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
