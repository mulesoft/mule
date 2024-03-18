/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.utils;

import org.mule.runtime.api.meta.MuleVersion;

/**
 * Holds information about the minimum {@link MuleVersion} that a component can run on and the reason for that value.
 *
 * @since 4.5
 */
public class ResolvedMinMuleVersion {

  public static final MuleVersion FIRST_MULE_VERSION = new MuleVersion("4.1.1");
  private String componentName;
  private MuleVersion minMuleVersion;
  private String reason;
  private ResolvedMinMuleVersion innerComponent = null;

  public ResolvedMinMuleVersion(String componentName, MuleVersion minMuleVersion, String reason) {
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

  public void updateIfHigherMMV(ResolvedMinMuleVersion candidate, String reason) {
    if (!(this.minMuleVersion.atLeast(candidate.getMinMuleVersion()))) {
      this.reason = reason;
      this.minMuleVersion = candidate.getMinMuleVersion();
      this.innerComponent = candidate;
    }
  }
}
