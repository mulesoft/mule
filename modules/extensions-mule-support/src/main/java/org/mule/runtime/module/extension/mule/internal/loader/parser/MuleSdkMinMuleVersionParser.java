/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.extension.api.loader.parser.MinMuleVersionParser;

class MuleSdkMinMuleVersionParser implements MinMuleVersionParser {

  public static final String MIN_MULE_VERSION_STRING = "4.5";
  private static final MuleVersion MIN_MULE_VERSION = new MuleVersion(MIN_MULE_VERSION_STRING);
  private final String reason;

  MuleSdkMinMuleVersionParser(String reason) {
    this.reason = reason;
  }

  @Override
  public MuleVersion getMinMuleVersion() {
    return MIN_MULE_VERSION;
  }

  @Override
  public String getReason() {
    return reason;
  }
}
