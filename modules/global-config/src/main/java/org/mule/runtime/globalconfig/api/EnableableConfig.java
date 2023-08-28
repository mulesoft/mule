/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
