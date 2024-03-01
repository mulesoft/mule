/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tls.internal.util;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_SECURITY_SYSTEM_PROPERTY;
import static org.mule.runtime.module.tls.internal.TlsConfiguration.DEFAULT_SECURITY_MODEL;

import static java.lang.System.getProperty;

public final class SecurityUtils {

  public static String getSecurityModel() {
    return getProperty(MULE_SECURITY_SYSTEM_PROPERTY, DEFAULT_SECURITY_MODEL);
  }

  public static boolean isDefaultSecurityModel() {
    return getSecurityModel().equals(DEFAULT_SECURITY_MODEL);
  }
}
