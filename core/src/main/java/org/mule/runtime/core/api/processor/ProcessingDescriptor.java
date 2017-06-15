/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

/**
 * Provides information about a processing unit represented by this instance
 */
public interface ProcessingDescriptor {

  /**
   * Whether the processing represented by this instance is synchronous or not
   */
  boolean isSynchronous();

}
