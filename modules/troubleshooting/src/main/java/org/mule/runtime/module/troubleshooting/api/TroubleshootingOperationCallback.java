/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.troubleshooting.api;

import org.mule.api.annotation.Experimental;

import java.io.Serializable;
import java.util.Map;

/**
 * Internal interface to the troubleshooting operation callbacks.
 * <p>
 * <b>Important:</b> Return externalizable objects to avoid marshalling errors when using it in an external tool.
 *
 * @since 4.5
 */
@Experimental
public interface TroubleshootingOperationCallback {

  /**
   * The operation logic. The troubleshooting operations should be able to be triggered from an external client, so the
   * implementation of each operation callback should return an object that can be marshaled and un-marshaled.
   *
   * @param arguments A dictionary with the argument names and values.
   * @return a {@link Serializable} object, which is the operation result.
   */
  Serializable execute(Map<String, String> arguments);
}
