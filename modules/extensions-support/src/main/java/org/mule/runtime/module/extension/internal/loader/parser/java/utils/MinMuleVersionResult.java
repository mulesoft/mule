/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.utils;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.sdk.api.annotation.MinMuleVersion;

/**
 * Holds information about an SdkComponent, its {@link MinMuleVersion} and the reason for that value.
 *
 * @since 4.6
 */
public class MinMuleVersionResult {

  private String componentName;
  private MuleVersion minMuleVersion;
  private String reason;
  private MinMuleVersionResult innerComponent = null;

  public MinMuleVersionResult(String componentName, MuleVersion minMuleVersion, String reason) {
    this.componentName = componentName;
    this.minMuleVersion = minMuleVersion;
    this.reason = reason;
  }

  public MuleVersion getMinMuleVersion() {
    return minMuleVersion;
  }

  public String getReason() {
    if (innerComponent != null) {
      return reason.concat(" ").concat(innerComponent.getReason());
    }
    return reason;
  }

  public String getName() {
    return componentName;
  }

  public void updateIfHigherMMV(MinMuleVersionResult candidate, String reason) {
    if (!(this.minMuleVersion.atLeast(candidate.getMinMuleVersion()))) {
      this.reason = reason;
      this.minMuleVersion = candidate.getMinMuleVersion();
      this.innerComponent = candidate;
    }
  }
}
