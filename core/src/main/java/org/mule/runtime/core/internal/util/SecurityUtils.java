/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
