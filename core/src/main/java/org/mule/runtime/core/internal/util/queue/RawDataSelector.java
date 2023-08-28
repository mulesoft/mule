/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.queue;

/**
 * Callback for defining if a certain operation should be executed over certain serialized data.
 */
interface RawDataSelector {

  /**
   * @param data serialized form data
   * @return true if this data should be selected for operation
   */
  boolean isSelectedData(byte[] data);

}
