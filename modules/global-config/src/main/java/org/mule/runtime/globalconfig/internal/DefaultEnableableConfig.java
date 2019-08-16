/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.globalconfig.internal;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.globalconfig.api.EnableableConfig;

@NoExtend
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
