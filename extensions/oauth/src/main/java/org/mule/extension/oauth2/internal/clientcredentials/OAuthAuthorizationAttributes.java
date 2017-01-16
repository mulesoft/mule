/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal.clientcredentials;

import org.mule.runtime.api.message.Attributes;

/**
 * Representation of an OAuth authorization request attributes.
 *
 * @since 4.0
 */
public class OAuthAuthorizationAttributes implements Attributes {

  private static final long serialVersionUID = -3075820956130637729L;

  private final String authorization;

  /**
   * @param authorization the value to put on the {@code Authorization} header in the token request.
   */
  public OAuthAuthorizationAttributes(String authorization) {
    this.authorization = authorization;
  }

  /**
   * @return the value to put on the {@code Authorization} header in the token request.
   */
  public String getAuthorization() {
    return authorization;
  }
}
