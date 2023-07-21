/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util;

import static java.lang.System.getProperty;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_SECURITY_SYSTEM_PROPERTY;
import static org.mule.runtime.core.privileged.security.tls.TlsConfiguration.DEFAULT_SECURITY_MODEL;

public final class SecurityUtils {

  public static String getSecurityModel() {
    return getProperty(MULE_SECURITY_SYSTEM_PROPERTY, DEFAULT_SECURITY_MODEL);
  }

  public static boolean isDefaultSecurityModel() {
    return getSecurityModel().equals(DEFAULT_SECURITY_MODEL);
  }
}
