/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.api;

import org.mule.api.annotation.Experimental;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * Internal interface to the troubleshooting operation callbacks.
 *
 * @since 4.5, refactored in 4.10
 */
@Experimental
public interface TroubleshootingOperationCallback {

  /**
   * The operation logic.
   * <p>
   * The troubleshooting operations should be able to be triggered from an external client.
   * <p>
   * The implementation of each operation callback should write a human-readable result.
   *
   * @param arguments    A dictionary with the argument names and values.
   * @param resultWriter A writer to write the result to.
   * @throws IOException when an error occurs writing to {@code resultWriter}.
   */
  void execute(Map<String, String> arguments, Writer resultWriter) throws IOException;
}
