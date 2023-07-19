/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util;

/**
 * Closes a stream. A stream closer can close multiple stream types.
 */
public interface StreamCloser {

  boolean canClose(Class<?> streamType);

  void close(Object stream) throws Exception;

}
