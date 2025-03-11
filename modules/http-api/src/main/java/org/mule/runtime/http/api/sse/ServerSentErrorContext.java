/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse;

/**
 * Error context. It contains the exception and... TODO: What else do we want here?
 */
public interface ServerSentErrorContext {

  /**
   * @return the exception.
   */
  Exception getException();
}
