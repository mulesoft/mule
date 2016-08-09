/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.retriever;

/**
 * Generic contract for configurations that contains operations for retrieving emails.
 *
 * @since 4.0
 */
public interface RetrieverConfiguration {

  /**
   * @return a boolean value that indicates whether the retrieved emails should be opened and read or not.
   */
  boolean isEagerlyFetchContent();
}
