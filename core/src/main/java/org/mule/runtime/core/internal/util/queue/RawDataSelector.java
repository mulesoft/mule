/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
