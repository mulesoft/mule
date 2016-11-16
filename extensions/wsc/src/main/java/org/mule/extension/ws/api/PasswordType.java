/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api;

import org.mule.extension.ws.api.security.SecurityStrategy;

/**
 * Password types that can be configured in a {@link SecurityStrategy}.
 *
 * @since 4.0
 */
public enum PasswordType {

  /**
   * The actual password for the username, the password hash, or derived password.
   */
  TEXT("PasswordText"),

  /**
   * The digest of the password (and optionally nonce and/or creation timestamp) for the username.
   */
  DIGEST("PasswordDigest");

  private final String type;

  PasswordType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
