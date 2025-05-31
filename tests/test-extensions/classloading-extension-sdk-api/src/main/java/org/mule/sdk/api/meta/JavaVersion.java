/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.sdk.api.meta;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.sdk.api.annotation.MinMuleVersion;

/**
 * Describes the Java versions supported by Mule
 *
 * @since 1.0
 */
@MinMuleVersion("4.5.0")
public enum JavaVersion {

  @MinMuleVersion("4.5.0")
  JAVA_8 {

    @Override
    public String version() {
      return "1.8";
    }

    @Override
    public MuleVersion supportedSince() {
      return new MuleVersion("4.1.1");
    }
  },

  @MinMuleVersion("4.5.0")
  JAVA_11 {

    @Override
    public String version() {
      return "11";
    }

    @Override
    public MuleVersion supportedSince() {
      return new MuleVersion("4.2.0");
    }
  },

  @MinMuleVersion("4.5.0")
  JAVA_17 {

    @Override
    public String version() {
      return "17";
    }

    @Override
    public MuleVersion supportedSince() {
      return new MuleVersion("4.6.0");
    }
  },

  @MinMuleVersion("4.10.0")
  JAVA_1000 {

    @Override
    public String version() {
      return "1000";
    }

    @Override
    public MuleVersion supportedSince() {
      return new MuleVersion("4.10.0");
    }
  };

  /**
   * @return The Java version in String format
   */
  public abstract String version();

  /**
   * @return The version of Mule in which support for this Java version was first introduced
   */
  public abstract MuleVersion supportedSince();
}
