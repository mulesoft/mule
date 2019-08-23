/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.globalconfig.api;

import org.mule.api.annotation.NoImplement;

/**
 * Interface for configuration elements that could be enable/disable as a whole.
 *
 * @since 4.3.0
 */
@NoImplement
public interface EnableableConfig {

  /**
   * @return true if the main configuration element is enabled, false otherwise.
   */
  boolean isEnabled();

}
