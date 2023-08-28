/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.globalconfig.internal;

import org.mule.runtime.globalconfig.api.EnableableConfig;

class DefaultEnableableConfig implements EnableableConfig {

  public static final String ENABLED_PROPERTY = "enabled";
  private final boolean enabled;

  public DefaultEnableableConfig(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

}
