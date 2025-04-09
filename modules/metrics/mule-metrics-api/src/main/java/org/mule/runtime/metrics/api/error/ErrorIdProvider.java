/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.error;

import org.mule.runtime.api.message.Error;

/**
 * Defines an interface for assigning IDs to {@link Error} implementations.
 *
 * @since 4.9.0
 */
public interface ErrorIdProvider {

  /**
   * Generates an ID for an {@link Error} implementation.
   *
   * @param error An {@link Error}.
   * @return An error ID that identifies the error (subsequent calls return the same error ID).
   */
  String getErrorId(Error error);

  /**
   * Generates an ID for a {@link Throwable} implementation.
   *
   * @param error A {@link Throwable} implementation.
   * @return An error ID that identifies the throwable (subsequent calls return the same error ID).
   */
  String getErrorId(Throwable error);
}
